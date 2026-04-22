package org.rd806.serverquiz.quiz;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.database.DatabaseConfig;
import org.rd806.serverquiz.quiz.storage.StorageFactory;

import java.util.Random;
import java.util.UUID;

public class QuizConfig {

    private String storageType;
    private int interval;
    private int maxNum;
    private BukkitTask send;

    public QuizConfig() {
        this.storageType = ServerQuiz.config.getString("config.storage", "YAML");
        this.interval = ServerQuiz.config.getInt("interval", 900);
        this.maxNum = 0;
    }

    // 初始化
    public void initial() {
        StorageFactory factory = new StorageFactory();
        ServerQuiz.main.storage = factory.createQuizStorage(this.storageType, new DatabaseConfig());
        ServerQuiz.main.storage.setQuiz();
    }

    // 发送随机Quiz
    public void sendRandomQuiz() {
        Random random = new Random();
        int id = random.nextInt(maxNum) + 1;
        ServerQuiz.main.quiz = getQuizById(id);
        ServerQuiz.logger.info("Sending random quiz: " + id);
        sendText();
    }

    // 发送特定Quiz
    public void sendSpecificQuiz(int id) {
        ServerQuiz.main.quiz = getQuizById(id);
        sendText();
    }

    // 获取特定Quiz
    public QuizEntry getQuizById(int id) {
        return ServerQuiz.main.storage.getQuizById(id);
    }

    // 重新加载Quiz
    public void reload() {
        ServerQuiz.main.storage.setQuiz();
        send.cancel();
        sendQuiz();
    }

    // 关闭Quiz系统
    public void closeQuiz() {
        ServerQuiz.main.storage.closeQuiz();
    }

    // 发送文本组件
    private void sendText() {
        // 创建可点击的文本组件
        TextComponent message = new TextComponent("§e============\n§6§l新一轮Quiz开始啦！\n【点击这里】§r§e打开Quiz界面！\n============");
        // 点击事件：玩家点击后会自动输入并执行此命令
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/serverquiz open"));
        // 悬浮文本
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7点击参加Quiz！")));
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

    // 检查玩家是否回答正确
    public void check(Player player, String response) {
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
        // 回答正确
        if (response.equals(ServerQuiz.main.quiz.getAnswer())) {
            // 赠送物品
            player.getInventory().addItem(ServerQuiz.main.quiz.getReward());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            ServerQuiz.main.quiz.setWinner(uuid);
            player.sendMessage(ServerQuiz.config.getString("messages.correct", "You have solved the quiz!"));
            return;
        }

        player.closeInventory();
        player.sendMessage(ServerQuiz.config.getString("messages.wrong", "Your answer is wrong!"));
    }

    // 获取特定的Quiz信息
    public void getQuizInfo(CommandSender sender, QuizEntry quiz) {
        sender.sendMessage("The chosen quiz id is: " + quiz.getId());
        sender.sendMessage("-----------------------");
        sender.sendMessage("Question: " + quiz.getQuestion());
        sender.sendMessage("Option A: " + quiz.getOptions().get(0));
        sender.sendMessage("Option B: " + quiz.getOptions().get(1));
        sender.sendMessage("Option C: " + quiz.getOptions().get(2));
        sender.sendMessage("Option D: " + quiz.getOptions().get(3));
        sender.sendMessage("Answer: " + quiz.getAnswer());
        sender.sendMessage("Reward: " + quiz.getReward().toString());
        sender.sendMessage("-----------------------");
    }

    public String getStorageType() { return this.storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public int getInterval() { return this.interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getMaxNum() { return this.maxNum; }
    public void setMaxNum(int maxNum) { this.maxNum = maxNum; }
}
