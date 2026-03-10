package org.rd806.quizplugin.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rd806.quizplugin.QuizPlugin;

public class QuizCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {

        Player player;

        // 发送Quiz
        if (args[0].equals("send")) {
            if (args.length == 2) {
                int id = Integer.parseInt(args[1]);
                QuizPlugin.main.quizConfig.sendSpecificQuiz(id);
                return true;
            }
            QuizPlugin.main.quizConfig.sendRandomQuiz();
            return true;
        }

        // 打开Quiz界面
        if (args[0].equals("open")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                Inventory quizMenu = QuizPlugin.main.quizGui.createQuizMenu(player);
                player.openInventory(quizMenu);
                return true;
            }
        }

        // 查找操作
        if (args[0].equals("show")) {
            if (sender instanceof Player) {
                player = (Player) sender;
                if (!player.hasPermission("quizplugin.show")) return false;
            }
            // 获取题目数量
            if (args[1].equals("num")) {
                sender.sendMessage("The total number of the Quiz is " + QuizPlugin.main.quizConfig.getMaxNum());
                return true;
            }
        }


        // 重载Quiz
        if (args[0].equals("reload")) {
            QuizPlugin.main.quizConfig.reload();
            sender.sendMessage("Reload successful!");
            return true;
        }

        return false;
    }
}
