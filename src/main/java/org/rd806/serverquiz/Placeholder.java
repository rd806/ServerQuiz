package org.rd806.serverquiz;

import org.bukkit.entity.Player;
import org.rd806.serverquiz.quiz.content.ScoreData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Placeholder {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([^%]+)%");
    private final Map<String, VariableProvider> providers = new HashMap<>();
    private VariableProvider defaultProvider = (key, player) -> "%" + key + "%";

    public Placeholder() {
        registerProvider("serverquiz_", (key, player) -> {
            if (player == null) return "?";
            ScoreData scoreData = ServerQuiz.main.quizConfig.getScoreData(player.getName());
            if (scoreData == null) return "?";

            int correctAnswers = scoreData.correctAnswers();
            int allAnswers = scoreData.totalAnswers();
            double accuracy = allAnswers == 0 ? 0 : (double) correctAnswers / allAnswers * 100;

            return switch (key.toLowerCase()) {
                case "name" -> player.getName();
                case "correct" -> String.valueOf(correctAnswers);
                case "total" -> String.valueOf(allAnswers);
                case "accuracy" -> String.format("%.2f", accuracy) + "%";
                default -> null;
            };
        });

        setDefaultProvider((key, player) -> {
            ServerQuiz.logger.warning("Placeholder provider not found: " + key);
            return "?" + key + "?";
        });
    }

    @FunctionalInterface
    public interface VariableProvider {
        String getValue(String key, Player player);
    }

    public void registerProvider(String prefix, VariableProvider provider) {
        providers.put(prefix.toLowerCase(), provider);
    }

    public void setDefaultProvider(VariableProvider provider) {
        this.defaultProvider = provider;
    }

    public String resolve(String text, Player player) {
        if (text == null || text.isEmpty()) return text;

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            String replacement = getReplacement(placeholder, player);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    public List<String> resolve(List<String> texts, Player player) {
        if (texts == null || texts.isEmpty()) return new ArrayList<>();

        List<String> result = new ArrayList<>();
        for (String text : texts) {
            result.add(resolve(text, player));
        }
        return result;
    }

    private String getReplacement(String placeholder, Player player) {
        for (Map.Entry<String, VariableProvider> entry : providers.entrySet()) {
            String prefix = entry.getKey();
            if (placeholder.toLowerCase().startsWith(prefix)) {
                String key = placeholder.substring(prefix.length());
                String value = entry.getValue().getValue(key, player);
                if (value != null) return value;
            }
        }
        return defaultProvider.getValue(placeholder, player);
    }
}
