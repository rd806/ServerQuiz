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

    // 题库文件名仅允许字母、数字、下划线和连字符，避免路径穿越
    private static final String BANK_NAME_PATTERN = "[A-Za-z0-9_-]+";

    private File quizFolder;
    private File scoreFile;
    private FileConfiguration score;

    private List<Map<?, ?>> scoreRawMap;

    // config.yml 中配置并成功加载的题库文件
    private final List<QuizBank> banks;
    // 所有题库中的题目，按加载顺序统一编号
    private final List<QuizEntry> quizList;

    private final Map<String, ScoreData> scoreMap;

    public YamlControl() {
        this.scoreRawMap = new ArrayList<>();
        this.banks = new ArrayList<>();
        this.quizList = new ArrayList<>();
        this.scoreMap = new HashMap<>();
    }

    /**
     * 一个题库文件：文件头声明类型，Entry 下列出所有条目
     */
    private static class QuizBank {
        // config.table 中配置的逻辑名称（不含 .yml）
        private final String name;
        private final File file;
        private QuizType type;
        private final List<QuizEntry> entries = new ArrayList<>();

        private QuizBank(String name, File file) {
            this.name = name;
            this.file = file;
        }
    }

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

            QuizBank bank = new QuizBank(name, file);
            if (!loadBank(bank)) continue;
            banks.add(bank);
            quizList.addAll(bank.entries);
            ServerQuiz.logger.info("Loaded quiz bank [" + file.getName() + "] (" + bank.type + "): "
                    + bank.entries.size() + " quiz(zes)");
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

    // 检查 jar 内是否存在指定资源
    private boolean hasResource(String path) {
        try (InputStream input = ServerQuiz.main.getResource(path)) {
            return input != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 将旧版单个 Quiz.yml 拆分迁移为 quiz/choice.yml 与 quiz/blank.yml（新格式）
     */
    private void migrateLegacyQuizFile() {
        File legacy = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml");
        if (!legacy.exists()) return;

        File choiceFile = new File(quizFolder, "choice.yml");
        File blankFile = new File(quizFolder, "blank.yml");
        // 新结构已存在时不迁移，避免覆盖
        if (choiceFile.exists() || blankFile.exists()) return;

        YamlConfiguration legacyConfig = YamlConfiguration.loadConfiguration(legacy);

        QuizBank choiceBank = new QuizBank("choice", choiceFile);
        choiceBank.type = QuizType.Choice;
        for (Map<?, ?> tempMap : getMapList(legacyConfig, "Choice")) {
            QuizEntry entry = parseLegacyChoice(tempMap);
            if (entry != null) choiceBank.entries.add(entry);
        }

        QuizBank blankBank = new QuizBank("blank", blankFile);
        blankBank.type = QuizType.Blank;
        for (Map<?, ?> tempMap : getMapList(legacyConfig, "Blank")) {
            QuizEntry entry = parseLegacyBlank(tempMap);
            blankBank.entries.add(entry);
        }

        if (!saveBank(choiceBank) || !saveBank(blankBank)) {
            ServerQuiz.logger.warning("Failed to migrate Quiz.yml");
            return;
        }

        // 迁移完成后重命名旧文件，避免下次启动重复迁移
        File backup = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml.migrated");
        if (!legacy.renameTo(backup)) {
            ServerQuiz.logger.warning("Could not rename Quiz.yml to Quiz.yml.migrated");
        }
        ServerQuiz.logger.info("Migrated Quiz.yml to quiz/choice.yml and quiz/blank.yml");
    }

    /**
     * 读取单个题库文件，成功返回 true
     */
    private boolean loadBank(QuizBank bank) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(bank.file);

        Object typeValue = getConfigValue(config, "Type");
        if (typeValue != null && !typeValue.toString().isBlank()) {
            QuizType type = parseType(typeValue.toString());
            if (type == null) {
                ServerQuiz.logger.warning("Invalid Type in quiz bank [" + bank.file.getName() + "], skipped!");
                return false;
            }
            bank.type = type;
            for (Map<?, ?> tempMap : getMapList(config, "Entry")) {
                QuizEntry entry = parseEntry(tempMap, type);
                bank.entries.add(entry);
            }
            return true;
        }

        // 没有 Type 头时兼容旧版 Choice/Blank 段格式
        return loadLegacyBank(bank, config);
    }

    /**
     * 兼容旧版多文件格式：文件中直接使用 Choice/Blank 段
     */
    private boolean loadLegacyBank(QuizBank bank, YamlConfiguration config) {
        List<Map<?, ?>> choiceMaps = getMapList(config, "Choice");
        List<Map<?, ?>> blankMaps = getMapList(config, "Blank");
        if (choiceMaps.isEmpty() && blankMaps.isEmpty()) {
            ServerQuiz.logger.warning("Quiz bank [" + bank.file.getName() + "] has no Type header, skipped!");
            return false;
        }

        // 新格式每个文件只有一种类型；旧文件同时包含两种时按文件名选择
        boolean choice = !choiceMaps.isEmpty();
        if (choice && !blankMaps.isEmpty()) {
            choice = !bank.file.getName().toLowerCase(Locale.ROOT).startsWith("blank");
            ServerQuiz.logger.warning("Quiz bank [" + bank.file.getName()
                    + "] contains both Choice and Blank, only " + (choice ? "Choice" : "Blank") + " is loaded!");
        }
        bank.type = choice ? QuizType.Choice : QuizType.Blank;

        if (choice) {
            for (Map<?, ?> tempMap : choiceMaps) {
                QuizEntry entry = parseLegacyChoice(tempMap);
                if (entry != null) bank.entries.add(entry);
            }
        } else {
            for (Map<?, ?> tempMap : blankMaps) {
                QuizEntry entry = parseLegacyBlank(tempMap);
                bank.entries.add(entry);
            }
        }
        return true;
    }

    // 解析新格式的条目，字段与 MySQL 列名一致
    private QuizEntry parseEntry(Map<?, ?> tempMap, QuizType type) {
        QuizEntry entry = new QuizEntry();
        entry.setType(type);
        entry.setQuestion(value(tempMap, "question"));
        entry.setAnswer(value(tempMap, "answer"));
        entry.setScore(getInt(tempMap, "score"));

        String reward = value(tempMap, "reward");
        if (reward != null && !reward.isEmpty()) entry.setReward(reward);

        if (type == QuizType.Choice) {
            List<QuizEntry.QuizOption> optionList = new ArrayList<>();
            optionList.add(new QuizEntry.QuizOption("A", value(tempMap, "option_a")));
            optionList.add(new QuizEntry.QuizOption("B", value(tempMap, "option_b")));
            optionList.add(new QuizEntry.QuizOption("C", value(tempMap, "option_c")));
            optionList.add(new QuizEntry.QuizOption("D", value(tempMap, "option_d")));
            // 设置选项后会根据答案标签重新计算正确答案的内容集合
            entry.setOptions(optionList);
        }
        return entry;
    }

    // 解析旧版选择题
    private QuizEntry parseLegacyChoice(Map<?, ?> tempMap) {
        Object optionsObj = getObject(tempMap, "Options");
        if (!(optionsObj instanceof Map<?, ?> options)) return null;

        List<QuizEntry.QuizOption> optionList = new ArrayList<>();
        optionList.add(new QuizEntry.QuizOption("A", value(options, "A")));
        optionList.add(new QuizEntry.QuizOption("B", value(options, "B")));
        optionList.add(new QuizEntry.QuizOption("C", value(options, "C")));
        optionList.add(new QuizEntry.QuizOption("D", value(options, "D")));

        QuizEntry entry = new QuizEntry(-1, QuizType.Choice, value(tempMap, "Question"),
                value(tempMap, "Answer"), optionList, getInt(tempMap, "Score"), null);
        String reward = value(tempMap, "Reward");
        if (reward != null && !reward.isEmpty()) entry.setReward(reward);
        return entry;
    }

    // 解析旧版填空题
    private QuizEntry parseLegacyBlank(Map<?, ?> tempMap) {
        QuizEntry entry = new QuizEntry();
        entry.setType(QuizType.Blank);
        entry.setQuestion(value(tempMap, "Question"));
        entry.setAnswer(value(tempMap, "Answer"));
        entry.setScore(getInt(tempMap, "Score"));
        String reward = value(tempMap, "Reward");
        if (reward != null && !reward.isEmpty()) entry.setReward(reward);
        return entry;
    }

    // 保存单个题库文件，成功返回 true
    private boolean saveBank(QuizBank bank) {
        if (bank.type == null) {
            ServerQuiz.logger.warning("Cannot save quiz bank [" + bank.file.getName() + "]: unknown type");
            return false;
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.set("type", bank.type == QuizType.Choice ? "choice" : "blank");
            config.set("entry", entriesToMapList(bank.entries, bank.type));
            config.save(bank.file);
            return true;
        } catch (IOException e) {
            ServerQuiz.logger.warning("Could not save quiz bank [" + bank.file.getName() + "]");
            return false;
        }
    }

    /**
     * 题目转为与 MySQL 列名一致，且可保存 Map 列表
     */
    private List<Map<String, Object>> entriesToMapList(List<QuizEntry> entries, QuizType type) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (QuizEntry quizEntry : entries) {
            Map<String, Object> quizData = new LinkedHashMap<>();
            quizData.put("question", quizEntry.getQuestion());

            if (type == QuizType.Choice) {
                List<QuizEntry.QuizOption> optionList = quizEntry.getOptions();
                if (optionList != null && optionList.size() >= 4) {
                    quizData.put("option_a", optionList.get(0).text);
                    quizData.put("option_b", optionList.get(1).text);
                    quizData.put("option_c", optionList.get(2).text);
                    quizData.put("option_d", optionList.get(3).text);
                }
            }

            quizData.put("answer", quizEntry.getAnswer());
            quizData.put("score", quizEntry.getScore());
            quizData.put("reward", quizEntry.getReward() == null ? "" : quizEntry.getReward().getType().toString());
            result.add(quizData);
        }
        return result;
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

    // 解析题库类型
    private QuizType parseType(String value) {
        if (value == null) return null;
        String type = value.trim();
        if (type.equalsIgnoreCase("Choice")) return QuizType.Choice;
        if (type.equalsIgnoreCase("Blank")) return QuizType.Blank;
        return null;
    }

    // 读取顶层的值，键名忽略大小写
    private Object getConfigValue(FileConfiguration config, String key) {
        Object value = config.get(key);
        if (value != null) return value;
        for (String actual : config.getKeys(false)) {
            if (actual.equalsIgnoreCase(key)) {
                return config.get(actual);
            }
        }
        return null;
    }

    // 读取顶层的 Map 列表，键名忽略大小写
    private List<Map<?, ?>> getMapList(FileConfiguration config, String key) {
        List<Map<?, ?>> maps = new ArrayList<>();
        Object value = getConfigValue(config, key);
        if (value instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) maps.add(map);
            }
        }
        return maps;
    }

    // 读取 Map 中的值，键名忽略大小写
    private Object getObject(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value != null) return value;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() != null && entry.getKey().toString().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    // 读取 Map 中的字符串，缺失时返回 null
    private String value(Map<?, ?> map, String key) {
        Object value = getObject(map, key);
        return value == null ? null : value.toString();
    }

    // 读取 Map 中的整数，缺失或无法解析时返回 0
    private int getInt(Map<?, ?> map, String key) {
        Object value = getObject(map, key);
        if (value instanceof Number number) return number.intValue();
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
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

    // 按 config.table 中的名称查找题库
    private QuizBank findTargetBank(String name) {
        for (QuizBank bank : banks) {
            if (bank.name.equalsIgnoreCase(name)) return bank;
        }
        return null;
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
    public QuizType getTableType(String name) {
        QuizBank bank = findTargetBank(name);
        return bank == null ? null : bank.type;
    }

    @Override
    public boolean addChoiceQuiz(String name, String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(name);
        if (bank == null || bank.type != QuizType.Choice) {
            ServerQuiz.logger.warning("[" + name + "] is not a Choice quiz bank!");
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

        bank.entries.add(tempQuiz);
        quizList.add(tempQuiz);
        ServerQuiz.main.quizConfig.setMaxNum(quizList.size());
        return saveBank(bank);
    }

    @Override
    public boolean addBlankQuiz(String name, String question, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(name);
        if (bank == null || bank.type != QuizType.Blank) {
            ServerQuiz.logger.warning("[" + name + "] is not a Blank quiz bank!");
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

        bank.entries.add(tempQuiz);
        quizList.add(tempQuiz);
        ServerQuiz.main.quizConfig.setMaxNum(quizList.size());
        return saveBank(bank);
    }

    // 按全局编号查找题库中的条目
    private QuizEntry findEntry(QuizBank bank, int id) {
        for (QuizEntry entry : bank.entries) {
            if (entry.getId() == id) return entry;
        }
        return null;
    }

    @Override
    public boolean updateChoiceQuiz(String name, int id, String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(name);
        if (bank == null || bank.type != QuizType.Choice) return false;
        QuizEntry entry = findEntry(bank, id);
        if (entry == null) return false;

        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setScore(score);
        entry.setReward(reward);

        List<QuizEntry.QuizOption> optionList = new ArrayList<>();
        optionList.add(new QuizEntry.QuizOption("A", optionA));
        optionList.add(new QuizEntry.QuizOption("B", optionB));
        optionList.add(new QuizEntry.QuizOption("C", optionC));
        optionList.add(new QuizEntry.QuizOption("D", optionD));
        // 设置选项后会重新计算正确答案的内容集合
        entry.setOptions(optionList);

        return saveBank(bank);
    }

    @Override
    public boolean updateBlankQuiz(String name, int id, String question, String answer, int score, String reward) {
        QuizBank bank = findTargetBank(name);
        if (bank == null || bank.type != QuizType.Blank) return false;
        QuizEntry entry = findEntry(bank, id);
        if (entry == null) return false;

        entry.setQuestion(question);
        entry.setAnswer(answer);
        entry.setScore(score);
        entry.setReward(reward);

        return saveBank(bank);
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
