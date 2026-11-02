package org.rd806.serverquiz.storage.yaml;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.rd806.serverquiz.quiz.QuizType;
import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.QuizEntry;
import org.rd806.serverquiz.storage.QuizStorage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class YamlControl implements QuizStorage {

    /// 题库文件名仅允许字母、数字、下划线和连字符，避免路径穿越
    private static final String BANK_NAME_PATTERN = "[A-Za-z0-9_-]+";

    private File quizFolder;
    private File scoreFile;
    private FileConfiguration score;

    private List<Map<?, ?>> scoreRawMap;

    /// config.yml 中配置并成功加载的题库文件
    private final List<QuizBank> banks;
    /// 所有题库中的题目，按加载顺序统一编号
    private final List<QuizEntry> quizList;

    private final Map<String, ScoreData> scoreMap;

    public YamlControl() {
        this.scoreRawMap = new ArrayList<>();
        this.banks = new ArrayList<>();
        this.quizList = new ArrayList<>();
        this.scoreMap = new HashMap<>();
    }

    /// 一个题库文件，可同时包含 Choice 与 Blank 两段
    private static class QuizBank {
        private final File file;
        private final List<QuizEntry> choiceEntries = new ArrayList<>();
        private final List<QuizEntry> blankEntries = new ArrayList<>();

        private QuizBank(File file) {
            this.file = file;
        }
    }

    /// 创建题库目录、迁移旧版 Quiz.yml，并按 config.table 加载所有题库文件
    @Override
    public void setQuiz() {
        quizFolder = new File(ServerQuiz.main.getDataFolder(), "quiz");
        scoreFile = new File(ServerQuiz.main.getDataFolder(), "Score.yml");
        if (!quizFolder.exists() && !quizFolder.mkdirs()) {
            ServerQuiz.logger.warning("Could not create quiz folder: " + quizFolder.getPath());
        }
        if (!scoreFile.exists()) {
            ServerQuiz.main.saveResource("Score.yml", false);
        }

        migrateLegacyQuizFile();

        banks.clear();
        quizList.clear();
        scoreMap.clear();

        List<String> bankNames = ServerQuiz.config.getStringList("config.table");
        // 兼容旧配置：未配置 table 时使用默认的两个题库
        if (bankNames.isEmpty()) {
            bankNames = List.of("choice", "blank");
        }

        Set<String> loaded = new HashSet<>();
        for (String rawName : bankNames) {
            String name = rawName == null ? "" : rawName.trim();
            if (name.isEmpty() || !name.matches(BANK_NAME_PATTERN)) {
                ServerQuiz.logger.warning("Invalid quiz bank name: " + rawName);
                continue;
            }
            // 文件系统在部分系统上不区分大小写，按小写去重
            if (!loaded.add(name.toLowerCase(Locale.ROOT))) continue;

            File file = new File(quizFolder, name + ".yml");
            if (!file.exists()) {
                String resource = "quiz/" + name + ".yml";
                if (!hasResource(resource)) {
                    ServerQuiz.logger.warning("Quiz bank file [" + file.getName() + "] does not exist, skipped!");
                    continue;
                }
                ServerQuiz.main.saveResource(resource, false);
            }

            QuizBank bank = new QuizBank(file);
            loadBank(bank);
            banks.add(bank);
            quizList.addAll(bank.choiceEntries);
            quizList.addAll(bank.blankEntries);
            ServerQuiz.logger.info("Loaded quiz bank [" + file.getName() + "]: "
                    + bank.choiceEntries.size() + " choice, " + bank.blankEntries.size() + " blank");
        }

        // 统一分配全局编号
        for (int i = 0; i < quizList.size(); i++) {
            quizList.get(i).setId(i + 1);
        }

        // 获取玩家分数
        score = YamlConfiguration.loadConfiguration(scoreFile);
        scoreRawMap = score.getMapList("Score");
        for (Map<?, ?> tempMap : scoreRawMap) {
            String name = (String) tempMap.get("Name");
            int correctAnswers = getInt(tempMap, "CorrectAnswers");
            int totalAnswers = getInt(tempMap, "TotalAnswers");
            int scores = getInt(tempMap, "Scores");
            scoreMap.put(name, new ScoreData(correctAnswers, totalAnswers, scores));
        }

        ServerQuiz.main.quizConfig.setMaxNum(quizList.size());
        ServerQuiz.logger.info("Quiz list initialized!");
        ServerQuiz.logger.info("Quiz number: " + quizList.size());
    }

    /// 检查 jar 内是否存在指定资源
    private boolean hasResource(String path) {
        try (InputStream input = ServerQuiz.main.getResource(path)) {
            return input != null;
        } catch (IOException e) {
            return false;
        }
    }

    /// 将旧版单个 Quiz.yml 拆分迁移到 quiz/choice.yml 与 quiz/blank.yml
    private void migrateLegacyQuizFile() {
        File legacy = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml");
        if (!legacy.exists()) return;

        File choiceFile = new File(quizFolder, "choice.yml");
        File blankFile = new File(quizFolder, "blank.yml");
        // 新结构已存在时不迁移，避免覆盖
        if (choiceFile.exists() || blankFile.exists()) return;

        try {
            YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(legacy);
            YamlConfiguration choiceConfig = new YamlConfiguration();
            choiceConfig.set("Choice", legacyConfig.getMapList("Choice"));
            choiceConfig.save(choiceFile);
            YamlConfiguration blankConfig = new YamlConfiguration();
            blankConfig.set("Blank", legacyConfig.getMapList("Blank"));
            blankConfig.save(blankFile);
            // 迁移完成后重命名旧文件，避免下次启动重复迁移
            File backup = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml.migrated");
            if (!legacy.renameTo(backup)) {
                ServerQuiz.logger.warning("Could not rename Quiz.yml to Quiz.yml.migrated");
            }
            ServerQuiz.logger.info("Migrated Quiz.yml to quiz/choice.yml and quiz/blank.yml");
        } catch (IOException e) {
            ServerQuiz.logger.warning("Failed to migrate Quiz.yml: " + e.getMessage());
        }
    }

    /// 读取单个题库文件
    private void loadBank(QuizBank bank) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(bank.file);

        for (Map<?, ?> tempMap : config.getMapList("Choice")) {
            QuizEntry entry = parseChoice(tempMap);
            if (entry == null) {
                ServerQuiz.logger.warning("Skipped an invalid choice quiz in [" + bank.file.getName() + "]");
                continue;
            }
            bank.choiceEntries.add(entry);
        }

        for (Map<?, ?> tempMap : config.getMapList("Blank")) {
            bank.blankEntries.add(parseBlank(tempMap));
        }
    }

    /// 解析选择题
    private QuizEntry parseChoice(Map<?, ?> tempMap) {
        Object optionsObj = tempMap.get("Options");
        if (!(optionsObj instanceof Map<?, ?> options)) return null;

        List<QuizEntry.QuizOption> optionList = new ArrayList<>();
        optionList.add(new QuizEntry.QuizOption("A", value(options, "A")));
        optionList.add(new QuizEntry.QuizOption("B", value(options, "B")));
        optionList.add(new QuizEntry.QuizOption("C", value(options, "C")));
        optionList.add(new QuizEntry.QuizOption("D", value(options, "D")));

        QuizEntry entry = new QuizEntry(-1, QuizType.Choice, value(tempMap, "Question"),
                value(tempMap, "Answer"), optionList, getInt(tempMap, "Score"), null);
        String reward = getReward(tempMap);
        if (reward != null && !reward.isEmpty()) entry.setReward(reward);
        return entry;
    }

    /// 解析填空题
    private QuizEntry parseBlank(Map<?, ?> tempMap) {
        QuizEntry entry = new QuizEntry();
        entry.setType(QuizType.Blank);
        entry.setQuestion(value(tempMap, "Question"));
        entry.setAnswer(value(tempMap, "Answer"));
        entry.setScore(getInt(tempMap, "Score"));
        String reward = getReward(tempMap);
        if (reward != null && !reward.isEmpty()) entry.setReward(reward);
        return entry;
    }

    /// 保存单个题库文件
    private void saveBank(QuizBank bank) {
        try {
            // 先读取原文件，保留 Choice/Blank 以外的自定义内容
            YamlConfiguration config = YamlConfiguration.loadConfiguration(bank.file);
            config.set("Choice", choiceToMapList(bank.choiceEntries));
            config.set("Blank", blankToMapList(bank.blankEntries));
            config.save(bank.file);
        } catch (IOException e) {
            ServerQuiz.logger.warning("Could not save quiz bank [" + bank.file.getName() + "]");
        }
    }

    private void saveScoreFile() {
        try {
            updateScoreRawMap();
            score.set("Score", scoreRawMap);
            score.save(scoreFile);
        } catch (IOException e) {
            ServerQuiz.logger.warning("Could not save score file");
        }
    }

    /// 选择题转为可保存的 Map 列表
    private List<Map<String, Object>> choiceToMapList(List<QuizEntry> entries) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuizEntry quizEntry : entries) {
            Map<String, Object> quizData = new LinkedHashMap<>();
            quizData.put("Question", quizEntry.getQuestion());

            // 设置选项
            List<QuizEntry.QuizOption> optionList = quizEntry.getOptions();
            if (optionList != null && optionList.size() >= 4) {
                Map<String, String> options = new LinkedHashMap<>();
                options.put("A", optionList.get(0).text);
                options.put("B", optionList.get(1).text);
                options.put("C", optionList.get(2).text);
                options.put("D", optionList.get(3).text);
                quizData.put("Options", options);
            }

            // 设置答案、分数与奖励
            quizData.put("Answer", quizEntry.getAnswer());
            quizData.put("Score", quizEntry.getScore());
            quizData.put("Reward", quizEntry.getReward() == null ? "" : quizEntry.getReward().getType().toString());

            result.add(quizData);
        }
        return result;
    }

    /// 填空题转为可保存的 Map 列表
    private List<Map<String, Object>> blankToMapList(List<QuizEntry> entries) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuizEntry quizEntry : entries) {
            Map<String, Object> quizData = new LinkedHashMap<>();
            quizData.put("Question", quizEntry.getQuestion());
            quizData.put("Answer", quizEntry.getAnswer());
            quizData.put("Score", quizEntry.getScore());
            quizData.put("Reward", quizEntry.getReward() == null ? "" : quizEntry.getReward().getType().toString());
            result.add(quizData);
        }
        return result;
    }

    // 更新分数Map数据
    private void updateScoreRawMap() {
        scoreRawMap.clear();
        for (Map.Entry<String, ScoreData> entry : scoreMap.entrySet()) {
            Map<String, Object> scoreData = new LinkedHashMap<>();
            scoreData.put("Name", entry.getKey());
            scoreData.put("CorrectAnswers", entry.getValue().correctAnswers());
            scoreData.put("TotalAnswers", entry.getValue().totalAnswers());
            scoreData.put("Scores", entry.getValue().scores());
            scoreRawMap.add(scoreData);
        }
    }

    // 读取Map中的整数，缺失时返回0
    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    // 读取Map中的值，缺失时返回null
    private String value(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : value.toString();
    }

    // 读取奖励名称
    private String getReward(Map<?, ?> map) {
        return value(map, "Reward");
    }

    // 获取特定的Quiz
    @Override
    public QuizEntry getQuizById(int id) {
        if (id < 1 || id > quizList.size()) {
            ServerQuiz.logger.warning("Quiz id out of range!");
            return null;
        }
        // List 默认从 0 开始编号
        QuizEntry tempQuiz = quizList.get(id - 1);
        tempQuiz.setAnsweredPlayers(new HashSet<>());
        tempQuiz.setWinner(null);
        return tempQuiz;
    }

    /// 查找写入目标：优先选择已包含对应题型的题库，否则使用第一个题库
    private QuizBank findTargetBank(boolean forChoice) {
        for (QuizBank bank : banks) {
            if (forChoice ? !bank.choiceEntries.isEmpty() : !bank.blankEntries.isEmpty()) return bank;
        }
        return banks.isEmpty() ? null : banks.get(0);
    }

    @Override
    public boolean createScoreBoard(String name, UUID uuid) {
        // 存在则不创建
        if (getPlayerScore(name) != null) return true;
        scoreMap.put(name, new ScoreData(0, 0, 0));
        saveScoreFile();
        return true;
    }

    // 获取玩家分数
    @Override
    public ScoreData getPlayerScore(String name) {
        return scoreMap.get(name);
    }

    @Override
    public boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(true);
        if (bank == null) {
            ServerQuiz.logger.warning("No quiz bank file is configured!");
            return false;
        }

        QuizEntry tempQuiz = new QuizEntry();
        tempQuiz.setType(QuizType.Choice);
        tempQuiz.setQuestion(question);
        tempQuiz.setAnswer(answer);
        tempQuiz.setScore(score);

        List<QuizEntry.QuizOption> optionList = new ArrayList<>();
        optionList.add(new QuizEntry.QuizOption("A", optionA));
        optionList.add(new QuizEntry.QuizOption("B", optionB));
        optionList.add(new QuizEntry.QuizOption("C", optionC));
        optionList.add(new QuizEntry.QuizOption("D", optionD));
        tempQuiz.setOptions(optionList);

        tempQuiz.setReward(reward);
        tempQuiz.setId(quizList.size() + 1);

        bank.choiceEntries.add(tempQuiz);
        quizList.add(tempQuiz);
        ServerQuiz.main.quizConfig.setMaxNum(quizList.size());
        saveBank(bank);
        return true;
    }

    @Override
    public boolean addBlankQuiz(String question, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(false);
        if (bank == null) {
            ServerQuiz.logger.warning("No quiz bank file is configured!");
            return false;
        }

        QuizEntry tempQuiz = new QuizEntry();
        tempQuiz.setType(QuizType.Blank);
        tempQuiz.setQuestion(question);
        tempQuiz.setAnswer(answer);
        tempQuiz.setScore(score);

        // 设置奖品
        tempQuiz.setReward(reward);
        tempQuiz.setId(quizList.size() + 1);

        bank.blankEntries.add(tempQuiz);
        quizList.add(tempQuiz);
        ServerQuiz.main.quizConfig.setMaxNum(quizList.size());
        saveBank(bank);
        return true;
    }

    @Override
    public boolean updateScoreBoard(String name, boolean check, int score) {
        ScoreData oldData = scoreMap.get(name);
        ScoreData newData;

        if (check) {
            newData = new ScoreData(oldData.correctAnswers() + 1, oldData.totalAnswers() + 1, oldData.scores() + score);
        } else {
            newData = new ScoreData(oldData.correctAnswers(), oldData.totalAnswers() + 1, oldData.scores());
        }
        scoreMap.put(name, newData);
        saveScoreFile();
        return true;
    }

    // 清空Quiz
    @Override
    public void closeQuiz() {
        // 题库文件的修改在 addChoiceQuiz / addBlankQuiz 时已即时保存，
        // 这里不再重复写盘，避免无修改时也重写文件、丢失注释
        saveScoreFile();
        quizList.clear();
        banks.clear();
        scoreMap.clear();
        scoreRawMap.clear();
    }
}
