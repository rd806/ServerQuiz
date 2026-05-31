package org.rd806.serverquiz.quiz.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.rd806.serverquiz.quiz.content.RewardData;
import org.rd806.serverquiz.quiz.content.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.content.QuizEntry;
import org.rd806.serverquiz.quiz.content.QuizType;

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
            List<String> optionList = quizEntry.getOptions();
            if (optionList != null && optionList.size() >= 4) {
                options.put("A", optionList.get(0));
                options.put("B", optionList.get(1));
                options.put("C", optionList.get(2));
                options.put("D", optionList.get(3));
                quizData.put("Options", options);
            }

            // 设置奖励
            String reward = quizEntry.getReward().rawData();
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

            // 设置奖励
            String reward = quizEntry.getReward().rawData();
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
            QuizEntry tempQuiz = new QuizEntry();
            // 设置问题
            tempQuiz.setId(i);
            tempQuiz.setType(QuizType.Choice);
            tempQuiz.setQuestion((String) tempMap.get("Question"));
            tempQuiz.setAnswer((String) tempMap.get("Answer"));
            // 设置选项
            Map<?, ?> options = (Map<?, ?>) tempMap.get("Options");

            List<String> optionList = new ArrayList<>();
            optionList.add((String) options.get("A"));
            optionList.add((String) options.get("B"));
            optionList.add((String) options.get("C"));
            optionList.add((String) options.get("D"));
            tempQuiz.setOptions(optionList);
            // 设置奖品
            Object rewardObj = tempMap.get("Reward");
            String rewardName = rewardObj.toString();
            RewardData.setRewardData(rewardName, tempQuiz);
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
            // 设置奖品
            Object rewardObj = tempMap.get("Reward");
            String rewardName = rewardObj.toString();
            RewardData.setRewardData(rewardName, tempQuiz);
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
            int correctAnswers = (int) tempMap.get("CorrectAnswers");
            int totalAnswers = (int) tempMap.get("TotalAnswers");
            ScoreData scoreData = new ScoreData(correctAnswers, totalAnswers);

            scoreMap.put(name, scoreData);
        }
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
        scoreMap.put(name, new ScoreData(0, 0));
        saveScoreFile();
        return true;
    }

    // 获取玩家分数
    @Override
    public ScoreData getPlayerScore(String name) {
        return scoreMap.get(name);
    }

    @Override
    public boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, String reward) {
        QuizEntry tempQuiz = new QuizEntry();
        tempQuiz.setType(QuizType.Choice);
        tempQuiz.setQuestion(question);
        tempQuiz.setAnswer(answer);

        List<String> optionList = new ArrayList<>();
        optionList.add(optionA);
        optionList.add(optionB);
        optionList.add(optionC);
        optionList.add(optionD);
        tempQuiz.setOptions(optionList);

        RewardData.setRewardData(reward, tempQuiz);

        choiceQuizList.add(tempQuiz);
        saveQuizFile();
        return true;
    }

    @Override
    public boolean addBlankQuiz(String question, String answer, String reward) {
        QuizEntry tempQuiz = new QuizEntry();
        tempQuiz.setType(QuizType.Blank);
        tempQuiz.setQuestion(question);
        tempQuiz.setAnswer(answer);

        // 设置奖品
        RewardData.setRewardData(reward, tempQuiz);

        blankQuizList.add(tempQuiz);
        saveQuizFile();
        return true;
    }

    @Override
    public boolean updateScoreBoard(String name, boolean check) {
        ScoreData oldData = scoreMap.get(name);
        ScoreData newData;

        if (check) {
            newData = new ScoreData(oldData.correctAnswers() + 1, oldData.totalAnswers() + 1);
        } else {
            newData = new ScoreData(oldData.correctAnswers(), oldData.totalAnswers() + 1);
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
