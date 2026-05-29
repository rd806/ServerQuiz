package org.rd806.serverquiz.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rd806.serverquiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.content.QuizEntry;

public class QuizCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {

        Player player;

        // 加载Quiz
        if (args[0].equals("initial")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.edit")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                    return false;
                }
            }
            ServerQuiz.main.quizConfig.initial();
            sender.sendMessage("Quiz has been initialized.");
            return true;
        }

        // 发送Quiz
        if (args[0].equals("send")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.edit")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                    return false;
                }
            }
            // 若有第2个参数，则发送特定Quiz
            if (args.length == 2) {
                int id = Integer.parseInt(args[1]);
                if (id > ServerQuiz.main.quizConfig.getMaxNum()) {
                    ServerQuiz.logger.warning("The chosen id is out of bounds or NOT FOUND!");
                    return false;
                }
                ServerQuiz.main.quizConfig.sendSpecificQuiz(id);
                return true;
            }
            // 反之，发送随机Quiz
            ServerQuiz.main.quizConfig.sendRandomQuiz();
            return true;
        }

        // 打开Quiz界面
        if (args[0].equals("open")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.open")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.open\" to use this command.");
                    return false;
                }
                Inventory quizMenu = ServerQuiz.main.quizGui.createQuizMenu(player);
                player.openInventory(quizMenu);
                return true;
            }
            sender.sendMessage("Not a player! Skip the command.");
            return false;
        }

        // 查找操作
        if (args[0].equals("show")) {
            // 权限检查
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.show")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.show\" to use this command.");
                    return false;
                }
            }
            // 显示默认信息
            if (args.length == 1) {
                sender.sendMessage("The total number of the Quiz: " + ServerQuiz.main.quizConfig.getMaxNum());
                sender.sendMessage("The interval between each Quiz: " + ServerQuiz.main.quizConfig.getInterval() + "s");
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
                    int interval = ServerQuiz.main.quizConfig.getInterval();
                    sender.sendMessage("The interval between each Quiz is " + interval + "s");
                    return true;
                }
                // 展示题目信息
                case "info" -> {
                    // 获取当前Quiz信息
                    if (args.length == 2) {
                        ServerQuiz.main.quizConfig.getQuizInfo(sender, ServerQuiz.main.quiz);
                        return true;
                    }
                    // 获取特定 ID 的 Quiz 信息
                    int num = Integer.parseInt(args[2]);
                    if (num > ServerQuiz.main.quizConfig.getMaxNum()) {
                        ServerQuiz.logger.warning("The chosen id is out of bounds or NOT FOUND!");
                        return false;
                    }
                    QuizEntry temp = ServerQuiz.main.quizConfig.getQuizById(num);
                    ServerQuiz.main.quizConfig.getQuizInfo(sender, temp);
                    return true;
                }
                // 获取玩家回答信息
                case "score" -> {
                    String target;
                    if (!(sender instanceof  Player)) {
                        ServerQuiz.logger.info("You must be a player to use this command.");
                        return false;
                    }

                    // 默认发送自己的分数
                    if (args.length == 2) {
                        target = sender.getName();
                    } else {
                        target = args[2];
                    }
                    ScoreData scoreData = ServerQuiz.main.quizConfig.getScoreData(target);
                    sender.sendMessage(target + "'s correct answers is " + scoreData.correctAnswers());
                    sender.sendMessage(target + "'s total answers is " + scoreData.totalAnswers());
                    return true;
                }
                default -> {
                    sender.sendMessage("The total number of the Quiz: " + ServerQuiz.main.quizConfig.getMaxNum());
                    sender.sendMessage("The interval between each Quiz: " + ServerQuiz.main.quizConfig.getInterval() + "s");
                    sender.sendMessage("The storage type: " + ServerQuiz.main.quizConfig.getStorageType());
                    return true;
                }
            }
        }

        // 修改操作
        if (args[0].equals("edit")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.edit")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                    return false;
                }
            }
            // 修改发送间隔
            if (args[1].equals("interval")) {
                int interval = Integer.parseInt(args[2]);
                ServerQuiz.main.quizConfig.setInterval(interval);
                sender.sendMessage("The interval of quiz has been set to " + interval);
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
                        String question = args[3];
                        String optionA = args[4];
                        String optionB = args[5];
                        String optionC = args[6];
                        String optionD = args[7];
                        String answer = args[8];
                        String reward = args[9];
                        if (ServerQuiz.main.quizConfig.addChoiceQuiz(question, optionA, optionB, optionC, optionD, answer, reward)) {
                            sender.sendMessage("The choice quiz has been added!");
                            sender.sendMessage("Please use /quiz initial to enable the change");
                            return true;
                        } else {
                            sender.sendMessage("Failed to add choice quiz!");
                            return false;
                        }
                    }
                    case "delete" -> {}
                }
            }
            // 修改填空题
            if (args[1].equals("blank")) {
                switch (args[2]) {
                    case "insert" -> {
                        String question = args[3];
                        String answer = args[4];
                        String reward = args[5];
                        if (ServerQuiz.main.quizConfig.addBlankQuiz(question, answer, reward)) {
                            sender.sendMessage("The blank quiz has been added!");
                            sender.sendMessage("Please use /quiz initial to enable the change");
                            return true;
                        } else  {
                            sender.sendMessage("Failed to add blank quiz!");
                            return false;
                        }
                    }
                    case "delete" -> {}
                }
            }

            sender.sendMessage("Please use /quiz reload command to enable your changes!");
            return true;
        }

        // 重载Quiz
        if (args[0].equals("reload")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("serverquiz.edit")) {
                    sender.sendMessage("§cYou do not have permission \"serverquiz.edit\" to use this command.");
                    return false;
                }
            }
            ServerQuiz.main.quizConfig.reload();
            sender.sendMessage("Reload successful!");
            return true;
        }

        return false;
    }
}
