package org.rd806.serverquiz.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rd806.serverquiz.quiz.QuizConfig;
import org.rd806.serverquiz.quiz.QuizType;
import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.quiz.QuizEntry;

public class QuizCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NonNull CommandSender sender, @NonNull Command command, @NonNull String label, String @NonNull [] args) {
        if (args.length == 0) {
            sender.sendMessage("ServerQuiz version: " + ServerQuiz.main.getDescription().getVersion());
            return false;
        }
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
                // 获取特定玩家的分数
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
        if (args.length < 2) {
            sender.sendMessage("Usage: /quiz edit storage|entry ...");
            return false;
        }

        switch (args[1]) {
            // 修改存储格式
            case "storage" -> {
                String storage = ServerQuiz.main.quizConfig.getStorageType();
                ServerQuiz.main.quizConfig.setStorageType(storage);
                sender.sendMessage("The storage type of the Quiz has been set to " + storage);
                return true;
            }
            // 修改题目
            case "entry" -> {
                return editEntry(sender, args);
            }
            default -> {
                sender.sendMessage("Unknown edit target: " + args[1]);
                return false;
            }
        }
    }

    /**
     * 编辑题库条目
     * <p>
     * 选择题：
     * /quiz edit entry &lt;table&gt; insert &lt;question&gt; &lt;optionA&gt; &lt;optionB&gt; &lt;optionC&gt; &lt;optionD&gt; &lt;answer&gt; &lt;score&gt; &lt;reward&gt;
     * /quiz edit entry &lt;table&gt; alter &lt;id&gt; &lt;question&gt; &lt;optionA&gt; &lt;optionB&gt; &lt;optionC&gt; &lt;optionD&gt; &lt;answer&gt; &lt;score&gt; &lt;reward&gt;
     * <p>
     * 填空题：
     * /quiz edit entry &lt;table&gt; insert &lt;question&gt; &lt;answer&gt; &lt;score&gt; &lt;reward&gt;
     * /quiz edit entry &lt;table&gt; alter &lt;id&gt; &lt;question&gt; &lt;answer&gt; &lt;score&gt; &lt;reward&gt;
     */
    private boolean editEntry(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("Usage: /quiz edit entry <table> insert|alter ...");
            return false;
        }

        String table = args[2];
        String operation = args[3];
        QuizType type = ServerQuiz.main.storage.getTableType(table);
        if (type == null) {
            sender.sendMessage("The table is not valid or does not exist!");
            return false;
        }

        boolean success;
        switch (operation) {
            case "insert" -> success = insertEntry(sender, args, table, type);
            case "alter" -> success = alterEntry(sender, args, table, type);
            default -> {
                sender.sendMessage("Unknown operation: " + operation + ", use insert or alter.");
                return false;
            }
        }

        if (success) {
            sender.sendMessage("The quiz has been " + (operation.equals("insert") ? "inserted" : "updated") + "!");
            sender.sendMessage("Please use /quiz reload to enable the change");
        } else {
            sender.sendMessage("Failed to edit the quiz!");
        }
        return success;
    }

    /**
     * 新增条目
     */
    private boolean insertEntry(CommandSender sender, String[] args, String table, QuizType type) {
        if (type == QuizType.Choice) {
            // /quiz edit entry <table> insert <question> <A> <B> <C> <D> <answer> <score> <reward>
            if (args.length < 12) {
                sender.sendMessage("Usage: /quiz edit entry <table> insert <question> <optionA> <optionB> <optionC> <optionD> <answer> <score> <reward>");
                return false;
            }
            Integer score = parseScore(sender, args[10]);
            if (score == null) return false;
            return ServerQuiz.main.storage.addChoiceQuiz(table,
                    args[4], args[5], args[6], args[7], args[8], args[9], score, args[11]);
        }

        // /quiz edit entry <table> insert <question> <answer> <score> <reward>
        if (args.length < 8) {
            sender.sendMessage("Usage: /quiz edit entry <table> insert <question> <answer> <score> <reward>");
            return false;
        }
        Integer score = parseScore(sender, args[6]);
        if (score == null) return false;
        return ServerQuiz.main.storage.addBlankQuiz(table, args[4], args[5], score, args[7]);
    }

    /**
     * 修改条目，id 为全局编号（可通过 /quiz show info &lt;id&gt; 查看）
     */
    private boolean alterEntry(CommandSender sender, String[] args, String table, QuizType type) {
        if (args.length < 5) {
            sender.sendMessage("Usage: /quiz edit entry <table> alter <id> ...");
            return false;
        }
        Integer id = parseId(sender, args[4]);
        if (id == null) return false;

        if (type == QuizType.Choice) {
            // /quiz edit entry <table> alter <id> <question> <A> <B> <C> <D> <answer> <score> <reward>
            if (args.length < 13) {
                sender.sendMessage("Usage: /quiz edit entry <table> alter <id> <question> <optionA> <optionB> <optionC> <optionD> <answer> <score> <reward>");
                return false;
            }
            Integer score = parseScore(sender, args[11]);
            if (score == null) return false;
            return ServerQuiz.main.storage.updateChoiceQuiz(table, id,
                    args[5], args[6], args[7], args[8], args[9], args[10], score, args[12]);
        }

        // /quiz edit entry <table> alter <id> <question> <answer> <score> <reward>
        if (args.length < 9) {
            sender.sendMessage("Usage: /quiz edit entry <table> alter <id> <question> <answer> <score> <reward>");
            return false;
        }
        Integer score = parseScore(sender, args[7]);
        if (score == null) return false;
        return ServerQuiz.main.storage.updateBlankQuiz(table, id, args[5], args[6], score, args[8]);
    }

    // 解析分数
    private Integer parseScore(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cThe score must be a number: " + value);
            return null;
        }
    }

    // 解析条目编号
    private Integer parseId(CommandSender sender, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cThe id must be a number: " + value);
            return null;
        }
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
