package org.rd806.quizplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.rd806.quizplugin.command.QuizCommand;
import org.rd806.quizplugin.command.QuizTabCompleter;
import org.rd806.quizplugin.gui.QuizEvent;
import org.rd806.quizplugin.gui.QuizGui;
import org.rd806.quizplugin.quiz.QuizConfig;
import org.rd806.quizplugin.quiz.QuizEntry;
import org.rd806.quizplugin.quiz.storage.QuizStorage;

import java.util.Objects;
import java.util.logging.Logger;

public final class QuizPlugin extends JavaPlugin {
    // 主类
    public static QuizPlugin main;
    public QuizEntry quiz;
    public QuizConfig quizConfig;
    public QuizGui quizGui;

    public QuizStorage storage;

    // 日志文件
    public static Logger logger;
    // 获取配置文件
    public static FileConfiguration config;

    public QuizPlugin() {
        main = this;
        logger = main.getLogger();
        config = main.getConfig();
        this.quiz = new QuizEntry();
        this.quizConfig = new QuizConfig();
        this.quizGui = new QuizGui();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger.info("QuizPlugin has been enabled!");
        logger.info(" ");
        logger.info("░██████╗░██╗░░░██╗██╗███████╗██████╗░██╗░░░░░██╗░░░██╗░██████╗░██╗███╗░░██╗");
        logger.info("██╔═══██╗██║░░░██║██║╚════██║██╔══██╗██║░░░░░██║░░░██║██╔════╝░██║████╗░██║");
        logger.info("██║██╗██║██║░░░██║██║░░███╔═╝██████╔╝██║░░░░░██║░░░██║██║░░██╗░██║██╔██╗██║");
        logger.info("╚██████╔╝██║░░░██║██║██╔══╝░░██╔═══╝░██║░░░░░██║░░░██║██║░░╚██╗██║██║╚████║");
        logger.info("░╚═██╔═╝░╚██████╔╝██║███████╗██║░░░░░███████╗╚██████╔╝╚██████╔╝██║██║░╚███║");
        logger.info("░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝╚═╝░░░░░╚══════╝░╚═════╝░░╚═════╝░╚═╝╚═╝░░╚══╝");
        logger.info(" ");

        // 注册命令
        Objects.requireNonNull(Bukkit.getPluginCommand("quizplugin")).setExecutor(new QuizCommand());
        Objects.requireNonNull(Bukkit.getPluginCommand("quizplugin")).setTabCompleter(new QuizTabCompleter());
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
        logger.info("QuizPlugin has been disabled!");
        logger.info(" ");
        logger.info("░██████╗░██╗░░░██╗██╗███████╗██████╗░██╗░░░░░██╗░░░██╗░██████╗░██╗███╗░░██╗");
        logger.info("██╔═══██╗██║░░░██║██║╚════██║██╔══██╗██║░░░░░██║░░░██║██╔════╝░██║████╗░██║");
        logger.info("██║██╗██║██║░░░██║██║░░███╔═╝██████╔╝██║░░░░░██║░░░██║██║░░██╗░██║██╔██╗██║");
        logger.info("╚██████╔╝██║░░░██║██║██╔══╝░░██╔═══╝░██║░░░░░██║░░░██║██║░░╚██╗██║██║╚████║");
        logger.info("░╚═██╔═╝░╚██████╔╝██║███████╗██║░░░░░███████╗╚██████╔╝╚██████╔╝██║██║░╚███║");
        logger.info("░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝╚═╝░░░░░╚══════╝░╚═════╝░░╚═════╝░╚═╝╚═╝░░╚══╝");
        logger.info(" ");

        quizConfig.closeQuiz();
    }
}
