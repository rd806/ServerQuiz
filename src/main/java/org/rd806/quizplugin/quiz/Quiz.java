package org.rd806.quizplugin.quiz;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Quiz {
    // 基本信息
    private String question;
    private String answer;
    private List<String> options;
    private ItemStack reward;

    private final Set<UUID> answeredPlayers;
    private UUID winner;

    public Quiz() {
        this.options = new ArrayList<>();
        this.answeredPlayers = new HashSet<>();
    }


    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public ItemStack getReward() { return reward; }
    public void setReward(ItemStack reward) { this.reward = reward; }

    public Set<UUID> getAnsweredPlayers() { return answeredPlayers; }

    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }

}
