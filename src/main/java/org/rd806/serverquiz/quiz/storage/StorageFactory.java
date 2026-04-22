package org.rd806.serverquiz.quiz.storage;

import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.database.DataSourceManager;
import org.rd806.serverquiz.database.DatabaseConfig;
import org.rd806.serverquiz.database.DatabaseInitial;

public class StorageFactory {

    public QuizStorage createQuizStorage(String type, DatabaseConfig databaseConfig) {
        if (type.equalsIgnoreCase("mysql")) {
            ServerQuiz.logger.info("Using MySQL QuizStorage for QuizPlugin");
            // 设置数据库信息
            databaseConfig.setHost(ServerQuiz.config.getString("config.host",  "localhost"));
            databaseConfig.setPort(ServerQuiz.config.getInt("config.port", 3036));
            databaseConfig.setDatabase(ServerQuiz.config.getString("config.database", "quizplugin"));
            databaseConfig.setUsername(ServerQuiz.config.getString("config.username", "root"));
            databaseConfig.setPassword(ServerQuiz.config.getString("config.password", ""));
            databaseConfig.setUseSSL(ServerQuiz.config.getBoolean("config.useSSL", false));
            // 建立数据库连接
            DataSourceManager dataSourceManager = new DataSourceManager(databaseConfig);
            DatabaseInitial databaseInitial = new DatabaseInitial(dataSourceManager);
            databaseInitial.initialTable();
            return new SqlControl(dataSourceManager);
        } else {
            ServerQuiz.logger.info("Using YAML QuizStorage for QuizPlugin");
            return new YamlControl();
        }
    }
}
