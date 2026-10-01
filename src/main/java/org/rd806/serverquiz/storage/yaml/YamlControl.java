package org.rd806.serverquiz.storage.yaml;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.quiz.QuizType;
import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.QuizEntry;
import org.rd806.serverquiz.storage.QuizStorage;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlControl implements QuizStorage {

    private File quizFile;
    private File scoreFile;
    private YamlConfiguration quiz;
    private FileConfiguration score;

    private List<Map<?, ?>> choiceQuizMap;
    private List<Map<?, ?>> blankQuizMap;
    private List<Map<?, ?>> scoreRawMap;

    private final List<QuizEntry> choiceQuizList;
    private final List<QuizEntry> blankQuizList;
    private final List<QuizEntry> quizList;

    private final Map<String, ScoreData> scoreMap;

    public YamlControl() {
        this.choiceQuizMap = new ArrayList<>();
        this.blankQuizMap = new ArrayList<>();
        this.scoreRawMap = new ArrayList<>();

        this.choiceQuizList = new ArrayList<>();
        this.blankQuizList = new ArrayList<>();
        this.quizList = new ArrayList<>();
        this.scoreMap = new HashMap<>();
    }

    // 保存文件更改
    private void saveQuizFile() {
        try {
            updateChoiceQuizMap();
            updateBlankQuizMap();
            quiz.set("Choice", choiceQuizMap);
            quiz.set("Blank", blankQuizMap);
            quiz.save(quizFile);
        } catch (IOException e) {
            ServerQuiz.logger.warning("Could not save quiz file");
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

    // 更新选择题Map数据
    private void updateChoiceQuizMap() {
        choiceQuizMap.clear();
        for (QuizEntry quizEntry : choiceQuizList) {
            Map<String, Object> quizData = new LinkedHashMap<>();
            quizData.put("Question", quizEntry.getQuestion());
            quizData.put("Answer", quizEntry.getAnswer());

            // 设置选项
            Map<String, String> options = new LinkedHashMap<>();
            List<QuizEntry.QuizOption> optionList = quizEntry.getOptions();
            if (optionList != null && optionList.size() >= 4) {
                options.put("A", optionList.get(0).text);
                options.put("B", optionList.get(1).text);
                options.put("C", optionList.get(2).text);
                options.put("D", optionList.get(3).text);
                quizData.put("Options", options);
            }

            // 设置分数与奖励
            quizData.put("Score", quizEntry.getScore());
            String reward = quizEntry.getReward() == null ? "" : quizEntry.getReward().getType().toString();
            quizData.put("Reward", reward);

            choiceQuizMap.add(quizData);
        }
    }

    // 更新填空题Map数据
    private void updateBlankQuizMap() {
        blankQuizMap.clear();
        for (QuizEntry quizEntry : blankQuizList) {
            Map<String, Object> quizData = new LinkedHashMap<>();
            quizData.put("Question", quizEntry.getQuestion());
            quizData.put("Answer", quizEntry.getAnswer());

            // 设置分数与奖励
            quizData.put("Score", quizEntry.getScore());
            String reward = quizEntry.getReward() == null ? "" : quizEntry.getReward().getType().toString();
            quizData.put("Reward", reward);

            blankQuizMap.add(quizData);
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

    @Override
    public void setQuiz() {
        // 创建配置文件
        quizFile = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml");
        scoreFile = new File(ServerQuiz.main.getDataFolder(), "Score.yml");
        if (!quizFile.exists()) {
            ServerQuiz.main.saveResource("Quiz.yml", false);
            ServerQuiz.main.saveResource("Score.yml", false);
        }
        quiz = YamlConfiguration.loadConfiguration(quizFile);
        score = YamlConfiguration.loadConfiguration(scoreFile);
        // 清除quizList
        quizList.clear();
        scoreMap.clear();
        int i = 1;

        // 初始化
        choiceQuizMap = quiz.getMapList("Choice");
        blankQuizMap = quiz.getMapList("Blank");
        scoreRawMap = score.getMapList("Score");

        // 设置选择题
        if (choiceQuizMap.isEmpty()) {
            ServerQuiz.logger.warning("Choice quiz List is empty!");
            return;
        }

        for (Map<?, ?> tempMap : choiceQuizMap) {
            // 设置问题
            String question = (String) tempMap.get("Question");
            String answer = (String) tempMap.get("Answer");
            // 设置选项
            Map<?, ?> options = (Map<?, ?>) tempMap.get("Options");
            List<QuizEntry.QuizOption> optionList = new ArrayList<>();
            optionList.add(new QuizEntry.QuizOption("A", (String) options.get("A")));
            optionList.add(new QuizEntry.QuizOption("B", (String) options.get("B")));
            optionList.add(new QuizEntry.QuizOption("C", (String) options.get("C")));
            optionList.add(new QuizEntry.QuizOption("D", (String) options.get("D")));
            // 设置分数与奖品
            int score = (Integer) tempMap.get("Score");
            ItemStack reward = (ItemStack) tempMap.get("Reward");

            QuizEntry tempQuiz = new QuizEntry(i, QuizType.Choice, question, answer, optionList, score, reward);
            // 将Quiz加入集合中
            choiceQuizList.add(tempQuiz);
            i++;
        }

        // 设置填空题
        if (blankQuizMap.isEmpty()) {
            ServerQuiz.logger.warning("Blank quiz List is empty!");
            return;
        }

        for (Map<?, ?> tempMap : blankQuizMap) {
            QuizEntry tempQuiz = new QuizEntry();
            tempQuiz.setId(i);
            tempQuiz.setType(QuizType.Blank);
            tempQuiz.setQuestion((String) tempMap.get("Question"));
            tempQuiz.setAnswer((String) tempMap.get("Answer"));
            // 设置分数与奖品
            tempQuiz.setScore(getInt(tempMap, "Score"));
            tempQuiz.setReward(getString(tempMap));
            // 将Quiz加入集合中
            blankQuizList.add(tempQuiz);
            i++;
        }

        quizList.addAll(choiceQuizList);
        quizList.addAll(blankQuizList);

        int num = quizList.size();
        ServerQuiz.main.quizConfig.setMaxNum(num);
        ServerQuiz.logger.info("Quiz list initialized!");
        ServerQuiz.logger.info("Quiz number: " + num);

        // 获取玩家分数
        for (Map<?, ?> tempMap : scoreRawMap) {
            String name = (String) tempMap.get("Name");
            int correctAnswers = getInt(tempMap, "CorrectAnswers");
            int totalAnswers = getInt(tempMap, "TotalAnswers");
            int scores = getInt(tempMap, "Scores");
            ScoreData scoreData = new ScoreData(correctAnswers, totalAnswers, scores);

            scoreMap.put(name, scoreData);
        }
    }

    // 读取Map中的整数，缺失时返回0
    private int getInt(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    // 读取Map中的字符串，缺失时返回null
    private String getString(Map<?, ?> map) {
        Object value = map.get("Reward");
        return value == null ? null : value.toString();
    }

    // 获取特定的Quiz
    @Override
    public QuizEntry getQuizById(int id) {
        // List 默认从 0 开始编号
        QuizEntry tempQuiz = quizList.get(id-1);
        tempQuiz.setAnsweredPlayers(new HashSet<>());
        tempQuiz.setWinner(null);
        return tempQuiz;
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

        choiceQuizList.add(tempQuiz);
        saveQuizFile();
        return true;
    }

    @Override
    public boolean addBlankQuiz(String question, String answer, int score, String reward) {
        QuizEntry tempQuiz = new QuizEntry();
        tempQuiz.setType(QuizType.Blank);
        tempQuiz.setQuestion(question);
        tempQuiz.setAnswer(answer);
        tempQuiz.setScore(score);

        // 设置奖品
        tempQuiz.setReward(reward);

        blankQuizList.add(tempQuiz);
        saveQuizFile();
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
        saveQuizFile();
        saveScoreFile();
        quizList.clear();
        choiceQuizList.clear();
        blankQuizList.clear();
        scoreMap.clear();
        choiceQuizMap.clear();
        blankQuizMap.clear();
        scoreRawMap.clear();
    }
}
