package org.rd806.quizplugin.quiz;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.rd806.quizplugin.QuizPlugin;
import org.rd806.quizplugin.database.DataSourceManager;
import org.rd806.quizplugin.database.DatabaseConfig;
import org.rd806.quizplugin.database.DatabaseInitial;

import java.util.Random;
import java.util.UUID;

public class QuizConfig {

    private String storageType;
    private int interval;
    private int maxNum;

    public QuizConfig() {
        this.storageType = QuizPlugin.config.getString("config.storage", "YAML");
        this.interval = QuizPlugin.config.getInt("interval", 900);
        this.maxNum = 0;
    }

    public void initial() {
        if (this.storageType.equalsIgnoreCase("YAML")) {
            YamlControl yamlControl = new YamlControl();
            yamlControl.setQuizList();
            QuizPlugin.logger.info("QuizPlugin uses YAML form!");
        } else if (this.storageType.equalsIgnoreCase("MYSQL")) {
            DatabaseConfig databaseConfig = new DatabaseConfig();
            // 设置数据库信息
            databaseConfig.setHost(QuizPlugin.config.getString("config.host",  "localhost"));
            databaseConfig.setPort(QuizPlugin.config.getInt("config.port", 3036));
            databaseConfig.setDatabase(QuizPlugin.config.getString("config.database", "quizplugin"));
            databaseConfig.setUsername(QuizPlugin.config.getString("config.username", "root"));
            databaseConfig.setPassword(QuizPlugin.config.getString("config.password", ""));
            databaseConfig.setUseSSL(QuizPlugin.config.getBoolean("config.useSSL", false));
            // 建立数据库连接
            DataSourceManager dataSourceManager = new DataSourceManager(databaseConfig);
            DatabaseInitial databaseInitial = new DatabaseInitial(dataSourceManager);
            QuizPlugin.main.sqlControl = new SqlControl(dataSourceManager);
            // 数据库初始化
            databaseInitial.initialTable();
            QuizPlugin.main.sqlControl.setQuiz();
            maxNum = QuizPlugin.main.sqlControl.getMaxId();

            QuizPlugin.logger.info("QuizPlugin uses MYSQL form!");
        }
    }

    // 发送随机Quzi
    public void sendRandomQuiz() {
        Random random = new Random();
        int id = random.nextInt(maxNum);

        if (storageType.equalsIgnoreCase("YAML")) {
            QuizPlugin.main.yamlControl.getQuizById(id);
            QuizPlugin.logger.info("Sending random quiz: " + id);
        }  else if (storageType.equalsIgnoreCase("MYSQL")) {
            int num =  id + 1;
            QuizPlugin.main.sqlControl.getQuizById(num);
            QuizPlugin.logger.info("Sending random quiz: " + num);
        }
        sendText();
    }

    // 发送特定Quiz
    public void sendSpecificQuiz(int id) {
        // 非空检查
        if (QuizPlugin.main.quiz == null || id > maxNum) {
            QuizPlugin.logger.warning("Quiz id " + id + " is out of bounds or NOT FOUND!");
            return;
        }

        if (storageType.equalsIgnoreCase("YAML")) {
            QuizPlugin.main.yamlControl.getQuizById(id);
        }  else if (storageType.equalsIgnoreCase("MYSQL")) {
            QuizPlugin.main.sqlControl.getQuizById(id);
        }
        sendText();
    }

    // 重新加载Quiz
    public void reload() {
        if (storageType.equalsIgnoreCase("YAML")) {
            QuizPlugin.main.yamlControl.setQuizList();
        }  else if (storageType.equalsIgnoreCase("MYSQL")) {
            QuizPlugin.main.sqlControl.setQuiz();
        }
    }

    // 关闭Quiz系统
    public void closeQuiz() {
        if (storageType.equalsIgnoreCase("YAML")) {
            QuizPlugin.main.yamlControl.closeQuiz();
        }  else if (storageType.equalsIgnoreCase("MYSQL")) {
            QuizPlugin.main.sqlControl.closeQuiz();
        }
    }

    // 发送文本组件
    private void sendText() {
        // 创建可点击的文本组件
        TextComponent message = new TextComponent("§e============\n§6§l新一轮Quiz开始啦！\n【点击这里】§r§e打开Quiz界面！\n============");
        // 点击事件：玩家点击后会自动输入并执行此命令
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/quizplugin open"));
        // 悬浮文本
        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("§7点击参加Quiz！")));
        QuizPlugin.main.getServer().spigot().broadcast(message);
    }

    // 检查玩家是否回答正确
    public void check(Player player, String response) {
        // 获取玩家UUID
        UUID uuid = player.getUniqueId();
        // 如果已有玩家回答正确
        if (QuizPlugin.main.quiz.getWinner() != null) {
            player.sendMessage(QuizPlugin.config.getString("messages.hasWinner", "The quiz has already been solved!"));
            player.closeInventory();
            return;
        }
        // 检查玩家是否已经回答过
        if (QuizPlugin.main.quiz.getAnsweredPlayers().contains(uuid)) {
            player.sendMessage(QuizPlugin.config.getString("messages.hasAnswered", "You have already answered this quiz!"));
            player.closeInventory();
            return;
        }
        // 记录回答信息
        QuizPlugin.main.quiz.getAnsweredPlayers().add(uuid);
        // 回答正确
        if (response.equals(QuizPlugin.main.quiz.getAnswer())) {
            // 赠送物品
            player.getInventory().addItem(QuizPlugin.main.quiz.getReward());
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.closeInventory();
            QuizPlugin.main.quiz.setWinner(uuid);
            player.sendMessage(QuizPlugin.config.getString("messages.correct", "You have solved the quiz!"));
            return;
        }

        player.closeInventory();
        player.sendMessage(QuizPlugin.config.getString("messages.wrong", "Your answer is wrong!"));
    }

    public String getStorageType() { return this.storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }

    public int getInterval() { return this.interval; }
    public void setInterval(int interval) { this.interval = interval; }

    public int getMaxNum() { return this.maxNum; }
    public void setMaxNum(int maxNum) { this.maxNum = maxNum; }

}
