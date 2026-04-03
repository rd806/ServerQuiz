package org.rd806.quizplugin.quiz.storage;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.rd806.quizplugin.QuizPlugin;
import org.rd806.quizplugin.quiz.QuizEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlControl implements QuizStorage {

    private List<Map<?, ?>> quizMap;
    private final List<QuizEntry> quizList;

    public YamlControl() {
        this.quizMap = new ArrayList<>();
        this.quizList = new ArrayList<>();
    }

    @Override
    public void setQuiz() {
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
        int i = 1;
        for (Map<?, ?> tempMap : quizMap) {
            QuizEntry tempQuiz = new QuizEntry();
            // 设置问题
            tempQuiz.setId(i);
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
            // 将Quiz加入集合中
            quizList.add(tempQuiz);
            i++;
        }
        int num = quizList.size() + 1;

        QuizPlugin.main.quizConfig.setMaxNum(num);
        QuizPlugin.logger.info("Quiz list initialized!");
        QuizPlugin.logger.info("Quiz number: " + num);
    }

    // 获取特定的Quiz
    @Override
    public QuizEntry getQuizById(int id) {
        // List 默认从 0 开始编号
        return quizList.get(id - 1);
    }

    // 清空Quiz
    @Override
    public void closeQuiz() {
        quizList.clear();
        quizMap.clear();
    }
}
