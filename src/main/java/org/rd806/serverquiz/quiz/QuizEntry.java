package org.rd806.serverquiz.quiz;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.ServerQuiz;

import java.util.*;

public class QuizEntry {
    // 基本信息
    private int id;
    private QuizType type;
    private String question;
    private String answer;
    private List<QuizOption> options;
    private int score;
    private ItemStack reward;

    /// 回答过的玩家
    private Set<UUID> answeredPlayers;
    /// 回答正确的玩家
    private UUID winner;

    public QuizEntry() {
        this.id = -1;
        this.options = new ArrayList<>();
        this.score = 0;
        this.answeredPlayers = new HashSet<>();
    }

    public QuizEntry(int id, QuizType type, String question, String answer, List<QuizOption> options, int score, ItemStack reward) {
        this.id = id;
        this.type = type;
        this.question = question;
        this.answer = answer;
        this.options = options;
        this.score = score;
        this.reward = reward;
    }

    /// 选项类型
    public static class QuizOption {
        public String label;
        public String text;

        public QuizOption(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public QuizType getType() { return type; }
    public void setType(QuizType type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<QuizOption> getOptions() { return options; }
    public void setOptions(List<QuizOption> options) { this.options = options; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    /// 获取奖励
    public ItemStack getReward() { return reward; }
    /// 设置奖励
    public void setReward(String rewardName) {
        if (rewardName == null || rewardName.isEmpty()) {
            ServerQuiz.logger.warning("Reward of the quiz is empty!");
            return;
        }
        Material rewardMaterial = Material.matchMaterial(rewardName.toUpperCase());
        if (rewardMaterial == null) {
            ServerQuiz.logger.warning("Invalid reward material: " + rewardName);
            rewardMaterial = Material.AIR;
        }
        this.reward = new ItemStack(rewardMaterial);
    }

    /// 奖励的物品名称，无奖励时返回 None
    public String getRewardName() {
        return reward == null ? "None" : reward.getType().toString();
    }

    public Set<UUID> getAnsweredPlayers() { return answeredPlayers; }
    public void setAnsweredPlayers(Set<UUID> answeredPlayers) { this.answeredPlayers = answeredPlayers; }

    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
}
