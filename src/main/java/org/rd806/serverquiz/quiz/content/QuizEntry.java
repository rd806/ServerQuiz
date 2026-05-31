package org.rd806.serverquiz.quiz.content;

import java.util.*;

public class QuizEntry {
    // 基本信息
    private int id;
    private QuizType type;
    private String question;
    private String answer;
    private List<String> options;
    private RewardData reward;

    private Set<UUID> answeredPlayers;
    private UUID winner;

    public QuizEntry() {
        this.id = -1;
        this.options = new ArrayList<>();
        this.answeredPlayers = new HashSet<>();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public QuizType getType() { return type; }
    public void setType(QuizType type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public RewardData getReward() { return reward; }
    public void setReward(RewardData reward) { this.reward = reward; }

    public Set<UUID> getAnsweredPlayers() { return answeredPlayers; }
    public void setAnsweredPlayers(Set<UUID> answeredPlayers) { this.answeredPlayers = answeredPlayers; }

    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }

}
