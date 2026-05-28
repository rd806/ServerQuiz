package org.rd806.serverquiz.quiz.storage;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.content.QuizEntry;
import org.rd806.serverquiz.quiz.content.QuizType;

import java.io.File;
import java.util.*;

public class YamlControl implements QuizStorage {

    private List<Map<?, ?>> choiceQuizMap;
    private List<Map<?, ?>> blankQuizMap;

    private final List<QuizEntry> quizList;

    public YamlControl() {
        this.choiceQuizMap = new ArrayList<>();
        this.blankQuizMap = new ArrayList<>();
        this.quizList = new ArrayList<>();
    }

    @Override
    public void setQuiz() {
        // 创建配置文件
        File quizFile = new File(ServerQuiz.main.getDataFolder(), "Quiz.yml");
        if (!quizFile.exists()) {
            ServerQuiz.main.saveResource("Quiz.yml", false);
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(quizFile);
        // 清除quizList
        quizList.clear();
        int i = 1;

        // 初始化
        choiceQuizMap = configuration.getMapList("Choice");
        blankQuizMap = configuration.getMapList("Blank");

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
            Material rewardMaterial = Material.matchMaterial(rewardName);
            if (rewardMaterial != null) {
                ItemStack rewardItem = new ItemStack(rewardMaterial);
                tempQuiz.setReward(rewardItem);
            }
            // 将Quiz加入集合中
            quizList.add(tempQuiz);
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
            Material rewardMaterial = Material.matchMaterial(rewardName);
            if (rewardMaterial != null) {
                ItemStack rewardItem = new ItemStack(rewardMaterial);
                tempQuiz.setReward(rewardItem);
            }
            // 将Quiz加入集合中
            quizList.add(tempQuiz);
            i++;
        }

        int num = quizList.size();
        ServerQuiz.main.quizConfig.setMaxNum(num);
        ServerQuiz.logger.info("Quiz list initialized!");
        ServerQuiz.logger.info("Quiz number: " + num);
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
        return false;
    }

    @Override
    public ScoreData getPlayerScore(String name) {
        return new ScoreData(0, 0);
    }

    @Override
    public boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, String reward) {
        return false;
    }

    @Override
    public boolean addBlankQuiz(String question, String answer, String reward) {
        return false;
    }

    @Override
    public boolean updateScoreBoard(String name, boolean check) {
        return false;
    }

    // 清空Quiz
    @Override
    public void closeQuiz() {
        quizList.clear();
        choiceQuizMap.clear();
        blankQuizMap.clear();
    }
}
