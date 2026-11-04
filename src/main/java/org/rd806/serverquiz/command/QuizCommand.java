package org.rd806.serverquiz.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rd806.serverquiz.quiz.QuizConfig;
import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.QuizEntry;

public class QuizCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        switch (args[0]) {
            // 发送Quiz
            case "send" -> { return sendQuiz(sender, args); }
            // 打开Quiz界面
            case "open" -> { return openQuiz(sender); }
            // 查找操作
            case "show" -> { return showInfo(sender, args); }
            // 修改操作
            case "edit" -> { return editQuiz(sender, args); }
            // 重载Quiz
            case "reload" -> { return reloadQuiz(sender); }
        }
        // 默认返回信息
        String version = ServerQuiz.main.getDescription().getVersion();
        sender.sendMessage("ServerQuiz version: " + version);
        return false;
    }

    /**
     * 发送间隔的展示文本：cron 模式显示表达式，否则显示秒数
     */
    private String getScheduleText() {
        String schedule = ServerQuiz.main.quizConfig.getSchedule();
        if (schedule != null) {
            return "The schedule of each Quiz: " + schedule;
        }
        return "No valid cron schedule, using the default " + QuizConfig.DEFAULT_INTERVAL_SECONDS + "s";
    }

    /**
     * 发送 Quiz
     */
    private boolean sendQuiz(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("serverquiz.edit")) {
                sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                return false;
            }
        }
        // 若有第2个参数，则发送特定Quiz
        if (args.length == 2) {
            int id = Integer.parseInt(args[1]);
            if (id > ServerQuiz.main.quizConfig.getMaxNum()) {
                sender.sendMessage("The chosen id is out of bounds or NOT FOUND!");
                return false;
            }
            ServerQuiz.main.quizConfig.sendSpecificQuiz(id);
            return true;
        }
        // 反之，发送随机Quiz
        ServerQuiz.main.quizConfig.sendRandomQuiz();
        return true;
    }

    /**
     * 打开 Quiz 界面
     */
    private boolean openQuiz(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("serverquiz.open")) {
                sender.sendMessage("§cYou do not have permission \"serverquiz.open\" to use this command.");
                return false;
            }
            Inventory quizMenu = ServerQuiz.main.quizGui.createQuizMenu(player);
            // 确保存在可用 Quiz
            if (quizMenu != null) {
                player.openInventory(quizMenu);
            }
            return true;
        }
        sender.sendMessage("Not a player! Skip the command.");
        return false;
    }

    /**
     * 展示 Quiz 信息
     */
    private boolean showInfo(CommandSender sender, String[] args) {
        // 权限检查
        if (sender instanceof Player player) {
            if (!player.hasPermission("serverquiz.show")) {
                sender.sendMessage("§cYou do not have permission \"serverquiz.show\" to use this command.");
                return false;
            }
        }
        // 显示默认信息
        if (args.length == 1) {
            sender.sendMessage("The total number of the Quiz: " + ServerQuiz.main.quizConfig.getMaxNum());
            sender.sendMessage(getScheduleText());
            sender.sendMessage("The storage type: " + ServerQuiz.main.quizConfig.getStorageType());
            return true;
        }
        // 获取特定信息
        switch (args[1]) {
            // 显示题目数量
            case "num" -> {
                sender.sendMessage("The total number of the Quiz is " + ServerQuiz.main.quizConfig.getMaxNum());
                return true;
            }
            // 显示存储格式
            case "storage" -> {
                String storage = ServerQuiz.main.quizConfig.getStorageType();
                sender.sendMessage("The storage type of the Quiz is " + storage);
                return true;
            }
            // 显示发送间隔
            case "interval" -> {
                sender.sendMessage(getScheduleText());
                return true;
            }
            // 展示题目信息
            case "info" -> {
                // 获取当前Quiz信息
                if (args.length == 2) {
                    ServerQuiz.main.quizConfig.getQuizInfo(sender, ServerQuiz.main.currentQuiz);
                    return true;
                }
                // 获取特定 ID 的 Quiz 信息
                int num = Integer.parseInt(args[2]);
                if (num > ServerQuiz.main.quizConfig.getMaxNum()) {
                    sender.sendMessage("The chosen id is out of bounds or NOT FOUND!");
                    return false;
                }
                QuizEntry temp = ServerQuiz.main.storage.getQuizById(num);
                ServerQuiz.main.quizConfig.getQuizInfo(sender, temp);
                return true;
            }
            // 获取玩家回答信息
            case "score" -> {
                // 默任获取完整计分表
                if (args.length == 2) {
                    return true;
                }
                String target = args[2];
                ScoreData scoreData = ServerQuiz.main.storage.getPlayerScore(target);
                if (scoreData == null) {
                    sender.sendMessage("§cNo score board found for " + target);
                    return false;
                }
                sender.sendMessage(target + "'s correct answers is " + scoreData.correctAnswers());
                sender.sendMessage(target + "'s total answers is " + scoreData.totalAnswers());
                sender.sendMessage(target + "'s scores is " + scoreData.scores());
                return true;
            }
            default -> {
                sender.sendMessage("The total number of the Quiz: " + ServerQuiz.main.quizConfig.getMaxNum());
                sender.sendMessage(getScheduleText());
                sender.sendMessage("The storage type: " + ServerQuiz.main.quizConfig.getStorageType());
                return true;
            }
        }
    }

    /**
     * 编辑信息
     */
    private boolean editQuiz(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("serverquiz.edit")) {
                sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                return false;
            }
        }
        // 修改存储格式
        if (args[1].equals("storage")) {
            String storage = ServerQuiz.main.quizConfig.getStorageType();
            ServerQuiz.main.quizConfig.setStorageType(storage);
            sender.sendMessage("The storage type of the Quiz has been set to" + storage);
        }
        // 修改选择题
        if (args[1].equals("choice")) {
            switch (args[2]) {
                case "insert" -> {
                    if (args.length < 11) {
                        sender.sendMessage("Usage: /quiz edit choice insert <question> <optionA> <optionB> <optionC> <optionD> <answer, e.g. A or A,C> <score> <reward>");
                        return false;
                    }
                    String question = args[3];
                    String optionA = args[4];
                    String optionB = args[5];
                    String optionC = args[6];
                    String optionD = args[7];
                    String answer = args[8];
                    int score;
                    try {
                        score = Integer.parseInt(args[9]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cThe score must be a number: " + args[9]);
                        return false;
                    }
                    String reward = args[10];
                    if (ServerQuiz.main.storage.addChoiceQuiz(question, optionA, optionB, optionC, optionD, answer, score, reward)) {
                        sender.sendMessage("The choice quiz has been added!");
                        sender.sendMessage("Please use /quiz reload to enable the change");
                        return true;
                    } else {
                        sender.sendMessage("Failed to add choice quiz!");
                        return false;
                    }
                }
                case "delete" -> {
                }
            }
        }
        // 修改填空题
        if (args[1].equals("blank")) {
            switch (args[2]) {
                case "insert" -> {
                    if (args.length < 7) {
                        sender.sendMessage("Usage: /quiz edit blank insert <question> <answer> <score> <reward>");
                        return false;
                    }
                    String question = args[3];
                    String answer = args[4];
                    int score;
                    try {
                        score = Integer.parseInt(args[5]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cThe score must be a number: " + args[5]);
                        return false;
                    }
                    String reward = args[6];
                    if (ServerQuiz.main.storage.addBlankQuiz(question, answer, score, reward)) {
                        sender.sendMessage("The blank quiz has been added!");
                        sender.sendMessage("Please use /quiz reload to enable the change");
                        return true;
                    } else {
                        sender.sendMessage("Failed to add blank quiz!");
                        return false;
                    }
                }
                case "delete" -> {
                    return true;
                }
            }
        }
        sender.sendMessage("Please use /quiz reload command to enable your changes!");
        return true;
    }

    /**
     * 重载 Quiz
     */
    private boolean reloadQuiz(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!player.hasPermission("serverquiz.edit")) {
                sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                return false;
            }
        }
        ServerQuiz.main.quizConfig.reload();
        sender.sendMessage("Reload successfully!");
        return true;
    }
}
