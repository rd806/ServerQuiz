package org.rd806.serverquiz;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.rd806.serverquiz.command.QuizCommand;
import org.rd806.serverquiz.command.QuizTabCompleter;
import org.rd806.serverquiz.compat.vault.Vault;
import org.rd806.serverquiz.gui.QuizEvent;
import org.rd806.serverquiz.gui.QuizGui;
import org.rd806.serverquiz.utils.PlaceHolder;
import org.rd806.serverquiz.quiz.QuizConfig;
import org.rd806.serverquiz.quiz.QuizEntry;
import org.rd806.serverquiz.storage.QuizStorage;
import org.rd806.serverquiz.utils.UpdateChecker;

import java.util.Objects;
import java.util.logging.Logger;

public final class ServerQuiz extends JavaPlugin {
    // 主类
    public static ServerQuiz main;
    // 配置变量
    public PlaceHolder placeHolder;
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
        this.placeHolder = new PlaceHolder();
        this.quiz = new QuizEntry();
        this.quizConfig = new QuizConfig();
        this.quizGui = new QuizGui();
    }

    @Override
    public void onEnable() {
        // 初始化 bstats 服务
        new Metrics(this, 31703);
        // 加载第三方库
        vault = new Vault();
        placeHolder.checkPlaceholderAPI();

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
        reloadPluginConfig();

        quizConfig.initial();
        quizConfig.sendQuiz();
    }

    /// 重新读取 config.yml，并同步静态配置引用
    public void reloadPluginConfig() {
        reloadConfig();
        config = getConfig();
    }

    @Override
    public void onDisable() {
        logger.info("ServerQuiz has been disabled!");
        logger.info(" ");
        logger.info("░██████╗███████╗██████╗░██╗░░░██╗███████╗██████╗░░██████╗░██╗░░░██╗██╗███████╗");
        logger.info("██╔════╝██╔════╝██╔══██╗██║░░░██║██╔════╝██╔══██╗██╔═══██╗██║░░░██║██║╚════██║");
        logger.info("╚█████╗░█████╗░░██████╔╝╚██╗░██╔╝█████╗░░██████╔╝██║██╗██║██║░░░██║██║░░███╔═╝");
        logger.info("░╚═══██╗██╔══╝░░██╔══██╗░╚████╔╝░██╔══╝░░██╔══██╗╚██████╔╝██║░░░██║██║██╔══╝░░");
        logger.info("██████╔╝███████╗██║░░██║░░╚██╔╝░░███████╗██║░░██║░╚═██╔═╝░╚██████╔╝██║███████╗");
        logger.info("╚═════╝░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░╚══════╝╚═╝░░╚═╝░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝");
        logger.info(" ");

        storage.closeQuiz();
    }
}
