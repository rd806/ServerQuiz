package org.rd806.quizplugin.quiz.storage;

import org.rd806.quizplugin.QuizPlugin;
import org.rd806.quizplugin.database.DataSourceManager;
import org.rd806.quizplugin.database.DatabaseConfig;
import org.rd806.quizplugin.database.DatabaseInitial;

public class StorageFactory {

    public QuizStorage createQuizStorage(String type, DatabaseConfig databaseConfig) {
        if (type.equalsIgnoreCase("mysql")) {
            QuizPlugin.logger.info("Using MySQL QuizStorage for QuizPlugin");
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
            databaseInitial.initialTable();
            return new SqlControl(dataSourceManager);
        } else {
            QuizPlugin.logger.info("Using YAML QuizStorage for QuizPlugin");
            return new YamlControl();
        }
    }
}
