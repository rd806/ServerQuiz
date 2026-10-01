package org.rd806.serverquiz.storage.database;

import org.rd806.serverquiz.ServerQuiz;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitial {
    private final DataSourceManager dataSourceManager;

    public DatabaseInitial(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
    }

    // 创建新表
    public void initialTable() {
        try(Connection connection = dataSourceManager.getConnection()) {
            createTables(connection);
            migrateTables(connection);

            ServerQuiz.logger.info("Database initiated!");
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Database initiated failed!");
            throw new RuntimeException(e);
        }
    }

    // 创建新表
    private void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
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
                        score INT,
                        reward VARCHAR(255)
                    )
                    """;
            // 创建填空题表
            String createBlankQuizTable = """
                    CREATE TABLE IF NOT EXISTS blank (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        question TEXT,
                        answer VARCHAR(255),
                        score INT,
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
                        all_answers INTEGER,
                        scores INT
                    )
                    """;

            statement.execute(createChoiceQuizTable);
            statement.execute(createBlankQuizTable);
            statement.execute(createScoreTable);
        }
    }

    // 为旧版本数据库补充新增的列
    private void migrateTables(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "choice", "score", "INT DEFAULT 0");
        addColumnIfMissing(connection, "blank", "score", "INT DEFAULT 0");
        addColumnIfMissing(connection, "score", "scores", "INT DEFAULT 0");
    }

    // 若列不存在则添加，并为旧数据填充默认值
    private void addColumnIfMissing(Connection connection, String table, String column, String definition) throws SQLException {
        if (hasColumn(connection, table, column)) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            // 旧数据填充默认值，避免累加运算结果为 NULL
            statement.execute("UPDATE " + table + " SET " + column + " = 0 WHERE " + column + " IS NULL");
        }
        ServerQuiz.logger.info("Added missing column " + column + " to table " + table);
    }

    // 检查列是否已经存在
    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        // 表名在不同系统上大小写敏感度不同，统一忽略大小写比较
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, table, null)) {
            while (columns.next()) {
                if (columns.getString("COLUMN_NAME").equalsIgnoreCase(column)) {
                    return true;
                }
            }
        }
        return false;
    }
}
