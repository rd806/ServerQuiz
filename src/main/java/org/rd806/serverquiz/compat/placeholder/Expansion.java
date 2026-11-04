package org.rd806.serverquiz.compat.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.ScoreData;

/// 使用 PlaceholderAPI 扩展
public class Expansion extends PlaceholderExpansion {

    private final ServerQuiz plugin;

    public Expansion(ServerQuiz plugin) {
        this.plugin = plugin;
    }


    @Override
    public String getIdentifier() {
        return "ServerQuiz";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }


    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NonNull String params) {

        if (player == null) return "Player Not Found";

        ScoreData scoreData = ServerQuiz.main.storage.getPlayerScore(player.getName());
        int correctAnswers = scoreData.correctAnswers();
        int allAnswers = scoreData.totalAnswers();
        int scores = scoreData.scores();
        double accuracy = allAnswers == 0 ? 0 : (double) correctAnswers / allAnswers * 100;

        if (params.equalsIgnoreCase("name")) {
            return player.getName();
        }

        if (params.equalsIgnoreCase("correct")) {
            return String.valueOf(correctAnswers);
        }

        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(allAnswers);
        }

        if (params.equalsIgnoreCase("accuracy")) {
            return String.format("%.2f", accuracy) + "%";
        }

        if (params.equalsIgnoreCase("scores")) {
            return String.valueOf(scores);
        }

        return null;
    }
}
