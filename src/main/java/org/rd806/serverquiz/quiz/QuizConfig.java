package org.rd806.serverquiz.quiz;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.storage.database.DatabaseConfig;
import org.rd806.serverquiz.storage.StorageFactory;

import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class QuizConfig {

    private String storageType;
    private int interval;
    private int maxNum;
    private BukkitTask send;

    public QuizConfig() {
        this.storageType = ServerQuiz.config.getString("config.storage", "YAML");
        this.interval = ServerQuiz.config.getInt("broadcast.interval", 900);
        this.maxNum = 0;
    }

    /// 初始化
    public void initial() {
        StorageFactory factory = new StorageFactory();
        ServerQuiz.main.storage = factory.createQuizStorage(this.storageType, new DatabaseConfig());
        ServerQuiz.main.storage.setQuiz();
    }

    /// 创建计分表
    public boolean createScoreBoard(String name, UUID uuid) {
        return ServerQuiz.main.storage.createScoreBoard(name, uuid);
    }

    /// 发送随机Quiz
    public void sendRandomQuiz() {
        if (maxNum <= 0) {
            ServerQuiz.logger.warning("No quiz loaded, skip sending random quiz!");
            return;
        }
        Random random = new Random();
        int id = random.nextInt(maxNum) + 1;
        ServerQuiz.main.quiz = getQuizById(id);
        ServerQuiz.logger.info("Sending random quiz: " + id);
        sendText();
    }

    /// 发送特定Quiz
    public void sendSpecificQuiz(int id) {
        ServerQuiz.main.quiz = getQuizById(id);
        sendText();
    }

    /// 获取特定Quiz
    public QuizEntry getQuizById(int id) {
        return ServerQuiz.main.storage.getQuizById(id);
    }

    /// 添加选择题
    public boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        return ServerQuiz.main.storage.addChoiceQuiz(question, optionA, optionB, optionC, optionD, answer, score, reward);
    }

    /// 添加填空题
    public boolean addBlankQuiz(String question, String answer, int score, String reward) {
        return ServerQuiz.main.storage.addBlankQuiz(question, answer, score, reward);
    }

    /// 重新加载Quiz
    public void reload() {
        // 重新读取 config.yml，使题库表等配置无需重启即可生效
        ServerQuiz.main.reloadPluginConfig();
        this.interval = ServerQuiz.config.getInt("broadcast.interval", 900);
        ServerQuiz.main.storage.setQuiz();
        if (send != null) send.cancel();
        sendQuiz();
    }

    /// 关闭Quiz系统
    public void closeQuiz() {
        ServerQuiz.main.storage.closeQuiz();
    }

    /// 发送文本组件
    private void sendText() {
        // 创建可点击的文本组件
        TextComponent message = new TextComponent(ServerQuiz.config.getString(
                "broadcast.text",
                "§e============\n§6§lNew Quiz begins! \n【Click here】 to join§r§e\n============")
        );
        // 点击事件：玩家点击后会自动输入并执行此命令
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/serverquiz open"));
        // 悬浮文本
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ServerQuiz.config.getString(
                "broadcast.hover",
                "§7Click to join Server Quiz!")))
        );
        ServerQuiz.main.getServer().spigot().broadcast(message);
    }

   // 定时发送Quiz
   public void sendQuiz() {
        if (send != null) send.cancel();
        // 每 interval 秒发送一次
        send = new BukkitRunnable() {
            @Override
            public void run() {
                sendRandomQuiz();
            }
        }.runTaskTimer(ServerQuiz.main, 0L, 20L * this.interval);
   }

    // 检查填空题回答
    public void check(Player player, String response) {
        String answer = ServerQuiz.main.quiz.getAnswer();
        handleAnswer(player, response.equals(answer));
    }

    // 检查选择题回答：将用户提交的内容集合与正确答案的内容集合比较，顺序无关
    public void checkChoice(Player player, Set<String> selectedContents) {
        Set<String> answerContents = ServerQuiz.main.quiz.getAnswerContents();
        handleAnswer(player, !answerContents.isEmpty() && answerContents.equals(selectedContents));
    }

    // 统一处理回答结果
    private void handleAnswer(Player player, boolean correct) {
        // 获取玩家UUID
        UUID uuid = player.getUniqueId();
        // 如果已有玩家回答正确
        if (ServerQuiz.main.quiz.getWinner() != null) {
            player.sendMessage(ServerQuiz.config.getString("messages.hasWinner", "The quiz has already been solved!"));
            player.closeInventory();
            return;
        }
        // 检查玩家是否已经回答过
        if (ServerQuiz.main.quiz.getAnsweredPlayers().contains(uuid)) {
            player.sendMessage(ServerQuiz.config.getString("messages.hasAnswered", "You have already answered this quiz!"));
            player.closeInventory();
            return;
        }
        // 记录回答信息
        ServerQuiz.main.quiz.getAnsweredPlayers().add(uuid);

        // 检查回答信息
        if (correct) {
            player.sendMessage(ServerQuiz.config.getString("messages.correct", "You have solved the quiz!"));
            // 赠送物品奖励
            ItemStack reward = ServerQuiz.main.quiz.getReward();
            if (reward != null) {
                player.getInventory().addItem(reward);
                player.sendMessage(ServerQuiz.config.getString("messages.sendReward", "Reward has been sent to you: ")
                        + reward.getType());
            }
            // 赠送经济奖励，仅当 Vault 可用时发放
            int score = ServerQuiz.main.quiz.getScore();
            if (score > 0 && !ServerQuiz.main.vault.isEconomyNotAvailable()) {
                ServerQuiz.main.vault.addPlayerEconomy(player, score);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            // 设置回答正确者
            ServerQuiz.main.quiz.setWinner(uuid);
        } else {
            player.sendMessage(ServerQuiz.config.getString("messages.wrong", "Your answer is wrong!"));
        }

        // 更新计分表，答对时累加题目的分数
        if (ServerQuiz.main.storage.updateScoreBoard(player.getName(), correct, ServerQuiz.main.quiz.getScore())) {
            player.sendMessage(ServerQuiz.config.getString("messages.score", "Your score board has been updated!"));
        }
        player.closeInventory();
    }

    // 获取特定的Quiz信息
    public void getQuizInfo(CommandSender sender, QuizEntry quiz) {
        sender.sendMessage("The chosen quiz id is: " + quiz.getId());
        sender.sendMessage("-----------------------");
        sender.sendMessage("Question: " + quiz.getQuestion());
        switch (quiz.getType()) {
            case Choice -> {
                sender.sendMessage("Option A: " + quiz.getOptions().get(0).text);
                sender.sendMessage("Option B: " + quiz.getOptions().get(1).text);
                sender.sendMessage("Option C: " + quiz.getOptions().get(2).text);
                sender.sendMessage("Option D: " + quiz.getOptions().get(3).text);
                sender.sendMessage("Answer: " + quiz.getAnswer());
            }
            case Blank -> sender.sendMessage("Answer: " + quiz.getAnswer());
        }
        sender.sendMessage("Score: " + quiz.getScore());
        sender.sendMessage("Reward: " + quiz.getRewardName());
        sender.sendMessage("-----------------------");
    }

    // 获取玩家回答信息
    public ScoreData getScoreData(String name) {
        return ServerQuiz.main.storage.getPlayerScore(name);
    }

    public String getStorageType() { return this.storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public int getInterval() { return this.interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getMaxNum() { return this.maxNum; }
    public void setMaxNum(int maxNum) { this.maxNum = maxNum; }
}
