package org.rd806.quizplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.rd806.quizplugin.command.QuizCommand;
import org.rd806.quizplugin.gui.QuizEvent;
import org.rd806.quizplugin.gui.QuizGui;
import org.rd806.quizplugin.quiz.QuizConfig;
import org.rd806.quizplugin.quiz.Quiz;
import org.rd806.quizplugin.quiz.SqlControl;
import org.rd806.quizplugin.quiz.YamlControl;

import java.util.Objects;
import java.util.logging.Logger;

public final class QuizPlugin extends JavaPlugin {
    // 主类
    public static QuizPlugin main;
    public Quiz quiz;
    public QuizConfig quizConfig;
    public QuizGui quizGui;

    public YamlControl yamlControl;
    public SqlControl sqlControl;

    // 日志文件
    public static Logger logger;
    // 获取配置文件
    public static FileConfiguration config;


    public QuizPlugin() {
        main = this;
        logger = main.getLogger();
        config = main.getConfig();
        this.quiz = new Quiz();
        this.quizConfig = new QuizConfig();
        this.quizGui = new QuizGui();
        this.yamlControl = new YamlControl();
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        logger.info("QuizPlugin has been enabled!");
        logger.info("░██████╗░██╗░░░██╗██╗███████╗██████╗░██╗░░░░░██╗░░░██╗░██████╗░██╗███╗░░██╗");
        logger.info("██╔═══██╗██║░░░██║██║╚════██║██╔══██╗██║░░░░░██║░░░██║██╔════╝░██║████╗░██║");
        logger.info("██║██╗██║██║░░░██║██║░░███╔═╝██████╔╝██║░░░░░██║░░░██║██║░░██╗░██║██╔██╗██║");
        logger.info("╚██████╔╝██║░░░██║██║██╔══╝░░██╔═══╝░██║░░░░░██║░░░██║██║░░╚██╗██║██║╚████║");
        logger.info("░╚═██╔═╝░╚██████╔╝██║███████╗██║░░░░░███████╗╚██████╔╝╚██████╔╝██║██║░╚███║");
        logger.info("░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝╚═╝░░░░░╚══════╝░╚═════╝░░╚═════╝░╚═╝╚═╝░░╚══╝");

        
        Objects.requireNonNull(Bukkit.getPluginCommand("quizplugin")).setExecutor(new QuizCommand());

        Bukkit.getPluginManager().registerEvents(new QuizEvent(), this);

        // 生成配置文件
        saveDefaultConfig();
        reloadConfig();

        quizConfig.initial();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logger.info("QuizPlugin has been disabled!");
        logger.info("QuizPlugin has been enabled!");
        logger.info("░██████╗░██╗░░░██╗██╗███████╗██████╗░██╗░░░░░██╗░░░██╗░██████╗░██╗███╗░░██╗");
        logger.info("██╔═══██╗██║░░░██║██║╚════██║██╔══██╗██║░░░░░██║░░░██║██╔════╝░██║████╗░██║");
        logger.info("██║██╗██║██║░░░██║██║░░███╔═╝██████╔╝██║░░░░░██║░░░██║██║░░██╗░██║██╔██╗██║");
        logger.info("╚██████╔╝██║░░░██║██║██╔══╝░░██╔═══╝░██║░░░░░██║░░░██║██║░░╚██╗██║██║╚████║");
        logger.info("░╚═██╔═╝░╚██████╔╝██║███████╗██║░░░░░███████╗╚██████╔╝╚██████╔╝██║██║░╚███║");
        logger.info("░░░╚═╝░░░░╚═════╝░╚═╝╚══════╝╚═╝░░░░░╚══════╝░╚═════╝░░╚═════╝░╚═╝╚═╝░░╚══╝");

        quizConfig.closeQuiz();
    }
}
