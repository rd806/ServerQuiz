package org.rd806.quizplugin.quiz;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rd806.quizplugin.QuizPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlControl {

    private List<Map<?, ?>> quizMap;
    private final List<Quiz> quizList;
    private int maxId;


    public YamlControl() {
        this.quizMap = new ArrayList<>();
        this.quizList = new ArrayList<>();
        this.maxId = 0;
    }

    public void setQuizList() {
        // 创建配置文件
        File quizFile = new File(QuizPlugin.main.getDataFolder(), "Quiz.yml");
        if (!quizFile.exists()) {
            QuizPlugin.main.saveResource("Quiz.yml", false);
        }
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(quizFile);

        quizMap = configuration.getMapList("List");
        if (quizMap.isEmpty()) {
            QuizPlugin.logger.warning("Quiz List is empty!");
            return;
        }
        // 将文本转换成quizList
        for (Map<?, ?> tempMap : quizMap) {
            Quiz tempQuiz = new Quiz();
            // 设置问题
            tempQuiz.setQuestion((String) tempMap.get("question"));
            tempQuiz.setAnswer((String) tempMap.get("answer"));
            // 设置选项
            Map<?, ?> options = (Map<?, ?>) tempMap.get("options");
            List<String> optionList = new ArrayList<>();
            optionList.add((String) options.get("A"));
            optionList.add((String) options.get("B"));
            optionList.add((String) options.get("C"));
            optionList.add((String) options.get("D"));
            tempQuiz.setOptions(optionList);
            // 设置奖品
            Object rewardObj = tempMap.get("reward");
            String rewardName = rewardObj.toString();
            Material rewardMaterial = Material.matchMaterial(rewardName);
            if (rewardMaterial != null) {
                ItemStack rewardItem = new ItemStack(rewardMaterial);
                tempQuiz.setReward(rewardItem);
            }

            quizList.add(tempQuiz);
        }

        maxId = quizList.size();
        QuizPlugin.logger.info("Quiz list initialized!");
        QuizPlugin.logger.info("Quiz number: " + this.maxId);
    }

    // 获取特定的Quiz
    public void getQuizById(int id) {
        QuizPlugin.main.quiz = quizList.get(id);
    }

    // 清空Quiz
    public void closeQuiz() {
        quizList.clear();
        quizMap.clear();
    }
}
