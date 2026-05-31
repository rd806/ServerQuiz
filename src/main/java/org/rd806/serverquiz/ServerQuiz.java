package org.rd806.serverquiz;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.rd806.serverquiz.command.QuizCommand;
import org.rd806.serverquiz.command.QuizTabCompleter;
import org.rd806.serverquiz.gui.QuizEvent;
import org.rd806.serverquiz.gui.QuizGui;
import org.rd806.serverquiz.quiz.QuizConfig;
import org.rd806.serverquiz.quiz.content.QuizEntry;
import org.rd806.serverquiz.quiz.storage.QuizStorage;

import java.util.Objects;
import java.util.logging.Logger;

public final class ServerQuiz extends JavaPlugin {
    // 主类
    public static ServerQuiz main;
    // 配置变量
    public Placeholder placeholder;
    public Vault vault;

    public QuizEntry quiz;
    public QuizConfig quizConfig;
    public QuizGui quizGui;

    public QuizStorage storage;

    // 日志文件
    public static Logger logger;
    // 配置文件
    public static FileConfiguration config;

    public ServerQuiz() {
        main = this;
        logger = main.getLogger();
        config = main.getConfig();
        this.placeholder = new Placeholder();
        this.quiz = new QuizEntry();
        this.quizConfig = new QuizConfig();
        this.quizGui = new QuizGui();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        // 初始化 bstats 服务
        new Metrics(this, 31703);
        // 加载 Vault 库
        vault = new Vault();

        logger.info("ServerQuiz has been enabled!");
        logger.info(" ");
        logger.info("░██████╗███████╗██████╗░██╗░░░██╗███████╗██████╗░░██████╗░██╗░░░██╗██╗███████╗");
        logger.info("██╔════╝██╔════╝██╔══██╗██║░░░██║██╔════╝██╔══██╗██╔═══██╗██║░░░██║██║╚════██║");
        logger.info("╚█████╗░█████╗░░██████╔╝╚██╗░██╔╝█████╗░░██████╔╝██║██╗██║██║░░░██║██║░░███╔═╝");
        logger.info("░╚═══██╗██╔══╝░░██╔══██╗░╚████╔╝░██╔══╝░░██╔══██╗╚██████╔╝██║░░░██║██║██╔══╝░░");
        logger.info("██████╔╝███████╗██║░░██║░░╚██╔╝░░███████╗██║░░██║░╚═██╔═╝░╚██████╔╝██║███████╗");
        logger.info("╚═════╝░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝");
        logger.info(" ");

        // 检查更新
        UpdateChecker.checkUpdate();

        // 注册命令
        Objects.requireNonNull(Bukkit.getPluginCommand("serverquiz")).setExecutor(new QuizCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("serverquiz")).setTabCompleter(new QuizTabCompleter());
        // 注册事件
        Bukkit.getPluginManager().registerEvents(new QuizEvent(), this);

        // 生成配置文件
        saveDefaultConfig();
        reloadConfig();

        quizConfig.initial();
        quizConfig.sendQuiz();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info("ServerQuiz has been disabled!");
        logger.info(" ");
        logger.info("░██████╗███████╗██████╗░██╗░░░██╗███████╗██████╗░░██████╗░██╗░░░██╗██╗███████╗");
        logger.info("██╔════╝██╔════╝██╔══██╗██║░░░██║██╔════╝██╔══██╗██╔═══██╗██║░░░██║██║╚════██║");
        logger.info("╚█████╗░█████╗░░██████╔╝╚██╗░██╔╝█████╗░░██████╔╝██║██╗██║██║░░░██║██║░░███╔═╝");
        logger.info("░╚═══██╗██╔══╝░░██╔══██╗░╚████╔╝░██╔══╝░░██╔══██╗╚██████╔╝██║░░░██║██║██╔══╝░░");
        logger.info("██████╔╝███████╗██║░░██║░░╚██╔╝░░███████╗██║░░██║░╚═██╔═╝░╚██████╔╝██║███████╗");
        logger.info("╚═════╝░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝");
        logger.info(" ");

        quizConfig.closeQuiz();
    }
}
