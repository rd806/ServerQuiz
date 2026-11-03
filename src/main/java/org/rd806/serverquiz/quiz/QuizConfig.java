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
import org.rd806.serverquiz.utils.CronSchedule;

import java.time.ZonedDateTime;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class QuizConfig {

    // cron 表达式缺失或解析失败时使用的默认间隔（秒）
    private static final int DEFAULT_INTERVAL_SECONDS = 900;
    // 秒数间隔，仅在未使用 cron 时有效
    private int interval;
    // cron 表达式原文，未使用 cron 时为 null
    private String schedule;
    // 解析后的 cron 表达式，未使用 cron 时为 null
    private CronSchedule cron;
    // 上一次 cron 触发的时间键，避免同一秒/同一分钟重复触发
    private long lastTriggeredKey = Long.MIN_VALUE;

    private String storageType;
    private int maxNum;

    // 定时任务
    private BukkitTask send;

    public QuizConfig() {
        this.storageType = ServerQuiz.config.getString("config.storage", "YAML");
        this.maxNum = 0;
        loadSchedule();
    }

    // 读取 broadcast.interval，只接受 cron 表达式；缺失或解析失败时回退到 900 秒
    private void loadSchedule() {
        this.interval = DEFAULT_INTERVAL_SECONDS;
        this.schedule = null;
        this.cron = null;

        Object raw = ServerQuiz.config.get("broadcast.interval");
        if (!(raw instanceof String text) || text.isBlank()) {
            ServerQuiz.logger.warning("broadcast.interval is not a cron expression, fallback to "
                    + DEFAULT_INTERVAL_SECONDS + "s");
            return;
        }

        String value = text.trim();
        try {
            this.cron = CronSchedule.parse(value);
            this.schedule = value;
        } catch (IllegalArgumentException e) {
            ServerQuiz.logger.warning("Invalid cron expression in broadcast.interval: \"" + value
                    + "\" (" + e.getMessage() + "), fallback to " + DEFAULT_INTERVAL_SECONDS + "s");
        }
    }

    /**
     * 初始化
     */
    public void initial() {
        // 确保启动时读取的是 reloadConfig 之后的最新配置
        loadSchedule();
        StorageFactory factory = new StorageFactory();
        ServerQuiz.main.storage = factory.createQuizStorage(this.storageType, new DatabaseConfig());
        ServerQuiz.main.storage.setQuiz();
    }

    /**
     * 发送随机 Quiz
     */
    public void sendRandomQuiz() {
        if (maxNum <= 0) {
            ServerQuiz.logger.warning("No quiz loaded, skip sending random quiz!");
            return;
        }
        Random random = new Random();
        int id = random.nextInt(maxNum) + 1;
        sendSpecificQuiz(id);
        ServerQuiz.logger.info("Sending random quiz: " + id);
    }

    /**
     * 发送特定 Quiz
     */
    public void sendSpecificQuiz(int id) {
        ServerQuiz.main.quiz = ServerQuiz.main.storage.getQuizById(id);
        sendText();
    }

    // 发送文本组件
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

    /**
     * 重新加载Quiz
     */
    public void reload() {
        // 重新读取 config.yml，使题库表等配置无需重启即可生效
        ServerQuiz.main.reloadPluginConfig();
        loadSchedule();
        ServerQuiz.main.storage.setQuiz();
        if (send != null) send.cancel();
        sendQuiz();
    }

    /**
     * 按配置发送 Quiz：cron 模式按表达式触发，否则按秒数间隔发送
     */
    public void sendQuiz() {
        if (send != null) send.cancel();
        lastTriggeredKey = Long.MIN_VALUE;

        if (cron != null) {
            CronSchedule cronSchedule = this.cron;
            // 每 10 tick 检查一次，足以覆盖到秒级的 cron 表达式
            send = new BukkitRunnable() {
                @Override
                public void run() {
                    ZonedDateTime now = ZonedDateTime.now();
                    if (!cronSchedule.matches(now.toLocalDateTime())) return;

                    // 6 段表达式按秒去重；5 段表达式在同一分钟内只触发一次
                    long key = cronSchedule.hasSeconds() ? now.toEpochSecond() : now.toEpochSecond() / 60;
                    if (key == lastTriggeredKey) return;
                    lastTriggeredKey = key;
                    sendRandomQuiz();
                }
            }.runTaskTimer(ServerQuiz.main, 0L, 10L);
            ServerQuiz.logger.info("Quiz schedule: cron \"" + this.schedule + "\"");
            return;
        }
        // cron 解析失败时的回退方法
        // 每 interval 秒发送一次
        final int period = Math.max(1, this.interval);
        send = new BukkitRunnable() {
            @Override
            public void run() {
                sendRandomQuiz();
            }
        }.runTaskTimer(ServerQuiz.main, 0L, 20L * period);
        ServerQuiz.logger.info("Quiz schedule: every " + period + "s");
    }

    // 检查填空题回答
    public void checkBlank(Player player, String response) {
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

    /**
     * 获取特定的 Quiz 信息
     */
    public void getQuizInfo(CommandSender sender, QuizEntry quiz) {
        // 及时判空
        if (quiz == null || quiz.getType() == null) {
            sender.sendMessage(ServerQuiz.config.getString("messages.noQuiz"));
            return;
        }
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
    public ScoreData getScoreData(String name) { return ServerQuiz.main.storage.getPlayerScore(name); }

    public String getStorageType() { return this.storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public int getInterval() { return this.interval; }
    // cron 表达式原文，未使用 cron 时返回 null
    public String getSchedule() { return this.schedule; }

    public int getMaxNum() { return this.maxNum; }
    public void setMaxNum(int maxNum) { this.maxNum = maxNum; }
}
