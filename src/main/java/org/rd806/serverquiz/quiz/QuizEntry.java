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
    // 答案。选择题为标签字符串（单个 "A" 或多个 "A,C"），填空题为答案文本
    private String answer;
    private List<QuizOption> options;
    // 选择题正确答案的内容集合，与标签及选项顺序无关
    private Set<String> answerContents;
    private int score;
    private ItemStack reward;

    // 回答过的玩家
    private Set<UUID> answeredPlayers;
    // 回答正确的玩家
    private UUID winner;

    public QuizEntry() {
        this.id = -1;
        this.options = new ArrayList<>();
        this.answerContents = new LinkedHashSet<>();
        this.score = 0;
        this.answeredPlayers = new HashSet<>();
    }

    public QuizEntry(int id, QuizType type, String question, String answer, List<QuizOption> options, int score, ItemStack reward) {
        this.id = id;
        this.type = type;
        this.question = question;
        this.answer = answer;
        this.options = options;
        this.answerContents = new LinkedHashSet<>();
        this.score = score;
        this.reward = reward;
        refreshAnswerContents();
    }

    /**
     * 选项类型，格式：{标签，内容}
     */
    public static class QuizOption {
        public String label;
        public String text;

        public QuizOption(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    /**
     * 将答案字符串解析为标签集合，支持 "A"、"A,C"、"A C"、"A;C" 等写法
     */
    private static Set<String> parseAnswerLabels(String answer) {
        Set<String> labels = new LinkedHashSet<>();
        if (answer == null) return labels;
        for (String part : answer.split("[,;|\\s]+")) {
            String label = part.trim().toUpperCase(Locale.ROOT);
            if (!label.isEmpty()) labels.add(label);
        }
        return labels;
    }

    /**
     * 根据当前选项与答案标签重新计算正确答案的内容集合
     */
    public void refreshAnswerContents() {
        this.answerContents = new LinkedHashSet<>(labelsToContents(parseAnswerLabels(answer)));
    }

    /// 根据标签查找选项内容
    public String getContentByLabel(String label) {
        if (label == null || options == null) return null;
        for (QuizOption option : options) {
            if (option.label.equalsIgnoreCase(label)) return option.text;
        }
        return null;
    }

    /// 根据内容查找标签
    public String getLabelByContent(String content) {
        if (content == null || options == null) return null;
        for (QuizOption option : options) {
            if (Objects.equals(option.text, content)) return option.label;
        }
        return null;
    }

    /**
     * 将标签集合转换为内容集合
     */
    public Set<String> labelsToContents(Collection<String> labels) {
        Set<String> contents = new LinkedHashSet<>();
        if (labels == null) return contents;
        for (String label : labels) {
            String content = getContentByLabel(label);
            if (content != null) contents.add(content);
        }
        return contents;
    }

    /**
     * 打乱后按位置重新映射标签（A、B、C、D），保证标签与界面位置一致
     */
    public void remapLabels() {
        if (options == null) return;
        // 先按内容记录正确答案，标签重新分配后再回填，保证 answer 与内容集合一致
        Set<String> contents = new LinkedHashSet<>(getAnswerContents());
        for (int i = 0; i < options.size(); i++) {
            options.get(i).label = String.valueOf((char) ('A' + i));
        }
        if (!contents.isEmpty()) {
            StringJoiner correctLabels = new StringJoiner(",");
            for (QuizOption option : options) {
                if (contents.contains(option.text)) correctLabels.add(option.label);
            }
            this.answer = correctLabels.toString();
            refreshAnswerContents();
        }
    }

    /**
     * 复制一份 QuizEntry，用于单独打乱选项而不影响原始数据
     */
    public QuizEntry copy() {
        List<QuizOption> copiedOptions = new ArrayList<>();
        if (options != null) {
            for (QuizOption option : options) {
                copiedOptions.add(new QuizOption(option.label, option.text));
            }
        }
        QuizEntry copy = new QuizEntry(id, type, question, answer, copiedOptions, score,
                reward == null ? null : reward.clone());
        copy.answerContents = new LinkedHashSet<>(getAnswerContents());
        return copy;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public QuizType getType() { return type; }
    public void setType(QuizType type) { this.type = type; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) {
        this.answer = answer;
        refreshAnswerContents();
    }

    /**
     * 获取选择题正确答案的内容集合
     */
    public Set<String> getAnswerContents() {
        if (answerContents.isEmpty() && answer != null && options != null && !options.isEmpty()) {
            refreshAnswerContents();
        }
        return answerContents;
    }
    public void setAnswerContents(Set<String> answerContents) { this.answerContents = answerContents; }

    public List<QuizOption> getOptions() { return options; }
    public void setOptions(List<QuizOption> options) {
        this.options = options;
        refreshAnswerContents();
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public ItemStack getReward() { return reward; }
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
    // 奖励的物品名称，无奖励时返回 None
    public String getRewardName() {
        return reward == null ? "None" : reward.getType().toString();
    }

    public Set<UUID> getAnsweredPlayers() { return answeredPlayers; }
    public void setAnsweredPlayers(Set<UUID> answeredPlayers) { this.answeredPlayers = answeredPlayers; }

    public UUID getWinner() { return winner; }
    public void setWinner(UUID winner) { this.winner = winner; }
}
