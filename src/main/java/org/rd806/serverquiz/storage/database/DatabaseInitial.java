package org.rd806.serverquiz.storage.database;

import org.rd806.serverquiz.ServerQuiz;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DatabaseInitial {

    /// 表名仅允许字母、数字和下划线，避免拼接 SQL 时被注入
    private static final String TABLE_NAME_PATTERN = "[A-Za-z0-9_]+";

    /// 默认题库表
    private static final List<String> DEFAULT_TABLES = List.of("choice", "blank");

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
                        answer VARCHAR(255),
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

        migrateConfiguredTables(connection);
    }

    // 为 config.yml 中配置的题库表补齐缺少的列（启动与 /quiz reload 时均可调用）
    public void migrateConfiguredTables(Connection connection) throws SQLException {
        for (String table : getConfiguredTables()) {
            // 使用数据库中的实际表名，避免大小写差异导致 ALTER 失败
            String actual = getActualTableName(connection, table);
            if (actual == null) continue;
            try {
                addColumnIfMissing(connection, actual, "score", "INT DEFAULT 0");
                addColumnIfMissing(connection, actual, "reward", "VARCHAR(255)", null);
                expandColumnIfTooShort(connection, actual, "answer", 255);
            } catch (SQLException e) {
                // 单张表迁移失败不应阻止插件启动
                ServerQuiz.logger.warning("Failed to migrate quiz table [" + actual + "]: " + e.getMessage());
            }
        }
    }

    // 读取 config.yml 中配置的题库表，未配置时使用默认表
    private List<String> getConfiguredTables() {
        List<String> configured = ServerQuiz.config.getStringList("config.table");
        if (configured.isEmpty()) return DEFAULT_TABLES;

        List<String> tables = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String rawName : configured) {
            String name = rawName == null ? "" : rawName.trim();
            if (name.isEmpty() || !name.matches(TABLE_NAME_PATTERN)) {
                ServerQuiz.logger.warning("Invalid quiz table name: " + rawName);
                continue;
            }
            if (seen.add(name.toLowerCase(Locale.ROOT))) {
                tables.add(name);
            }
        }
        return tables;
    }

    // 若列长度不足则扩展，保留原有数据
    private void expandColumnIfTooShort(Connection connection, String table, String column, int minSize) throws SQLException {
        int size = columnSize(connection, table, column);
        // 列不存在时跳过
        if (size <= 0 || size >= minSize) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " MODIFY COLUMN " + column + " VARCHAR(" + minSize + ")");
        }
        ServerQuiz.logger.info("Expanded column " + column + " of table " + table + " to VARCHAR(" + minSize + ")");
    }

    // 获取列长度，列不存在时返回 -1
    private int columnSize(Connection connection, String table, String column) throws SQLException {
        Integer size = getColumns(connection, table).get(column.toLowerCase(Locale.ROOT));
        return size == null ? -1 : size;
    }

    // 若列不存在则添加，并为旧数据填充默认值
    private void addColumnIfMissing(Connection connection, String table, String column, String definition) throws SQLException {
        addColumnIfMissing(connection, table, column, definition, "0");
    }

    // 若列不存在则添加；fillValue 为 null 时不填充旧数据
    private void addColumnIfMissing(Connection connection, String table, String column, String definition, String fillValue) throws SQLException {
        if (hasColumn(connection, table, column)) return;

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
            // 旧数据填充默认值，避免累加运算结果为 NULL
            if (fillValue != null) {
                statement.execute("UPDATE " + table + " SET " + column + " = " + fillValue + " WHERE " + column + " IS NULL");
            }
        }
        ServerQuiz.logger.info("Added missing column " + column + " to table " + table);
    }

    // 检查列是否已经存在
    private boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        return getColumns(connection, table).containsKey(column.toLowerCase(Locale.ROOT));
    }

    // 获取数据库中实际存在的表名（忽略大小写），不存在时返回 null
    private String getActualTableName(Connection connection, String table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, null, new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String actual = resultSet.getString("TABLE_NAME");
                if (actual.equalsIgnoreCase(table)) return actual;
            }
        }
        return null;
    }

    // 读取表的所有列及列长度，表不存在时返回空 Map
    private Map<String, Integer> getColumns(Connection connection, String table) throws SQLException {
        Map<String, Integer> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        // 使用 null 模式读取全部列后精确匹配表名，避免表名中的 "_" 被当作通配符
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, null, null)) {
            while (resultSet.next()) {
                if (!resultSet.getString("TABLE_NAME").equalsIgnoreCase(table)) continue;
                columns.put(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                        resultSet.getInt("COLUMN_SIZE"));
            }
        }
        return columns;
    }
}
