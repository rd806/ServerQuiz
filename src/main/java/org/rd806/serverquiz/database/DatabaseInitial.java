package org.rd806.serverquiz.database;

import org.rd806.serverquiz.ServerQuiz;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitial {
    private final DataSourceManager dataSourceManager;

    public DatabaseInitial(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    // 创建新表
    public void initialTable() {
        try(Connection connection = dataSourceManager.getConnection();
            Statement statement = connection.createStatement()) {
            // 创建选择题表
            String createChoiceQuizTable = """
                    CREATE TABLE IF NOT EXISTS choice (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        question TEXT,
                        option_a VARCHAR(255),
                        option_b VARCHAR(255),
                        option_c VARCHAR(255),
                        option_d VARCHAR(255),
                        answer CHAR(1),
                        reward VARCHAR(255)
                    )
                    """;
            // 创建填空题表
            String createBlankQuizTable = """
                    CREATE TABLE IF NOT EXISTS blank (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        question TEXT,
                        answer VARCHAR(255),
                        reward VARCHAR(255)
                    )
                    """;
            // 创建积分表
            String createScoreTable = """
                    CREATE TABLE IF NOT EXISTS score (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(255) UNIQUE,
                        uuid TEXT,
                        correct_answers INTEGER,
                        all_answers INTEGER
                    )
                    """;

            statement.execute(createChoiceQuizTable);
            statement.execute(createBlankQuizTable);
            statement.execute(createScoreTable);

            ServerQuiz.logger.info("Database initiated!");
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Database initiated failed!");
            throw new RuntimeException(e);
        }
    }
}
