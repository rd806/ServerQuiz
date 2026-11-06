package org.rd806.serverquiz.storage.database;

import org.rd806.serverquiz.quiz.QuizType;
import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.storage.QuizStorage;
import org.rd806.serverquiz.quiz.QuizEntry;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SqlControl implements QuizStorage {

    // 表名仅允许字母、数字和下划线，避免拼接 SQL 时被注入
    private static final String TABLE_NAME_PATTERN = "[A-Za-z0-9_]+";

    private final DataSourceManager dataSourceManager;
    // config.yml 中配置并成功加载的题库表
    private final List<QuizTable> tables;
    private int maxId;

    public SqlControl(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.tables = new ArrayList<>();
        this.maxId = 0;
    }

    // 一个题库表：表内 id 列表与它在全局编号中的起始位置
    private static class QuizTable {
        private final String name;
        private final QuizType type;
        private final List<Integer> ids;
        // 该表在全局编号中的起始下标（0 开始）
        private int offset;

        private QuizTable(String name, QuizType type, List<Integer> ids) {
            this.name = name;
            this.type = type;
            this.ids = ids;
        }
    }

    /**
     * 初始化Quiz列表
     */
    @Override
    public void setQuiz() {
        tables.clear();
        maxId = 0;

        List<String> tableNames = ServerQuiz.config.getStringList("config.table");
        // 兼容旧配置：未配置 table 时使用默认的两张表
        if (tableNames.isEmpty()) {
            tableNames = List.of("choice", "blank");
        }

        try (Connection connection = dataSourceManager.getConnection()) {
            // 为热插拔新增的表补齐列（score/reward），并扩展 answer 以支持多选题
            new DatabaseInitial(dataSourceManager).migrateConfiguredTables(connection);

            Set<String> loaded = new HashSet<>();
            for (String rawName : tableNames) {
                String name = rawName == null ? "" : rawName.trim();
                if (name.isEmpty() || !name.matches(TABLE_NAME_PATTERN)) {
                    ServerQuiz.logger.warning("Invalid quiz table name: " + rawName);
                    continue;
                }
                // MySQL 表名在部分系统上不区分大小写，按小写去重
                if (!loaded.add(name.toLowerCase(Locale.ROOT))) continue;

                // 使用数据库中的实际表名，避免大小写差异导致查询失败
                String actual = getActualTableName(connection, name);
                if (actual == null) {
                    ServerQuiz.logger.warning("Quiz table [" + name + "] does not exist, skipped!");
                    continue;
                }

                Map<String, Integer> columns = getColumns(connection, actual);
                QuizType type = detectTableType(columns);
                if (type == null) {
                    ServerQuiz.logger.warning("Quiz table [" + actual + "] has an incompatible schema, skipped!");
                    continue;
                }

                try {
                    List<Integer> ids = loadIds(connection, actual);
                    QuizTable table = new QuizTable(actual, type, ids);
                    table.offset = maxId;
                    tables.add(table);
                    maxId += ids.size();
                    ServerQuiz.logger.info("Loaded quiz table [" + actual + "] (" + type + "): " + ids.size() + " quiz(zes)");
                } catch (SQLException e) {
                    // 单张表加载失败不影响其他题库
                    ServerQuiz.logger.warning("Failed to load quiz table [" + actual + "]: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Quiz list initialized failed! " + e.getMessage());
        }

        ServerQuiz.main.quizConfig.setMaxNum(this.maxId);
        ServerQuiz.logger.info("Quiz list initialized!");
        ServerQuiz.logger.info("Quiz number: " + this.maxId);
    }

    /// 根据表结构推断题库类型：包含 option_a ~ option_d 视为选择题表，否则视为填空题表
    private QuizType detectTableType(Map<String, Integer> columns) {
        if (!columns.containsKey("id") || !columns.containsKey("question") || !columns.containsKey("answer")) return null;

        if (columns.containsKey("option_a")) {
            boolean completeChoice = columns.containsKey("option_b")
                    && columns.containsKey("option_c")
                    && columns.containsKey("option_d");
            return completeChoice ? QuizType.Choice : null;
        }
        return QuizType.Blank;
    }

    /// 读取表内所有 id（按升序），用于建立全局编号到表内 id 的映射
    private List<Integer> loadIds(Connection connection, String table) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM `" + table + "` ORDER BY id";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
            }
        }
        return ids;
    }

    /// 获取数据库中实际存在的表名（忽略大小写），不存在时返回 null
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

    /// 读取表的所有列及列长度，表不存在时返回空 Map
    private Map<String, Integer> getColumns(Connection connection, String table) throws SQLException {
        Map<String, Integer> columns = new LinkedHashMap<>();
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, null, null)) {
            while (resultSet.next()) {
                if (!resultSet.getString("TABLE_NAME").equalsIgnoreCase(table)) continue;
                columns.put(resultSet.getString("COLUMN_NAME").toLowerCase(Locale.ROOT),
                        resultSet.getInt("COLUMN_SIZE"));
            }
        }
        return columns;
    }

    /// 根据全局下标找到所属题库表
    private QuizTable findTable(int globalIndex) {
        for (QuizTable table : tables) {
            if (globalIndex >= table.offset && globalIndex < table.offset + table.ids.size()) {
                return table;
            }
        }
        return null;
    }

    // 根据全局下标获取表内 id
    private int findLocalId(QuizTable table, int globalIndex) {
        return table.ids.get(globalIndex - table.offset);
    }

    // 查找指定类型的题库表
    private QuizTable findTargetTable(String name) {
        for (QuizTable table : tables) {
            if (table.name.equalsIgnoreCase(name)) return table;
        }
        return null;
    }

    @Override
    public QuizEntry getQuizById(int id) {
        if (id < 1 || id > maxId) {
            ServerQuiz.logger.warning("Quiz id out of range!");
            return null;
        }

        int globalIndex = id - 1;
        QuizTable table = findTable(globalIndex);
        if (table == null) {
            ServerQuiz.logger.warning("Quiz id not found: " + id);
            return null;
        }
        int localId = findLocalId(table, globalIndex);

        String sql = table.type == QuizType.Choice
                ? "SELECT question, option_a, option_b, option_c, option_d, answer, score, reward FROM `" + table.name + "` WHERE id = ?"
                : "SELECT question, answer, score, reward FROM `" + table.name + "` WHERE id = ?";

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, localId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return null;

                QuizEntry temp = new QuizEntry();
                // 使用全局编号，保证 send <id> / show info <id> 与内部编号一致
                temp.setId(id);
                temp.setType(table.type);
                temp.setQuestion(resultSet.getString("question"));
                temp.setAnswer(resultSet.getString("answer"));
                temp.setScore(resultSet.getInt("score"));
                temp.setReward(resultSet.getString("reward"));

                if (table.type == QuizType.Choice) {
                    List<QuizEntry.QuizOption> options = new ArrayList<>();
                    options.add(new QuizEntry.QuizOption("A", resultSet.getString("option_a")));
                    options.add(new QuizEntry.QuizOption("B", resultSet.getString("option_b")));
                    options.add(new QuizEntry.QuizOption("C", resultSet.getString("option_c")));
                    options.add(new QuizEntry.QuizOption("D", resultSet.getString("option_d")));
                    // 设置选项后会根据答案标签重新计算正确答案的内容集合
                    temp.setOptions(options);
                }
                return temp;
            }
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Failed to load quiz " + id + " from table [" + table.name + "]: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean createScoreBoard(String name, UUID uuid) {
        String sql = """
              INSERT INTO score (name, uuid, correct_answers, all_answers, scores) VALUES (?, ?, 0, 0, 0) ON DUPLICATE KEY UPDATE id = id;
              """;

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            statement.setString(2, uuid.toString());

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Failed to create new score board!" + e.getMessage());
            return false;
        }
    }

    // 获取玩家分数
    @Override
    public ScoreData getPlayerScore(String name) {
        ScoreData score = new ScoreData(0, 0, 0);
        String sql = """
                SELECT correct_answers, all_answers, scores FROM score WHERE name = ?;
                """;

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int correctAnswers = resultSet.getInt("correct_answers");
                int allAnswers = resultSet.getInt("all_answers");
                int scores = resultSet.getInt("scores");
                return new ScoreData(correctAnswers, allAnswers, scores);
            }

            return score;
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Failed to get score data!" + e.getMessage());
            return null;
        }

    }

    @Override
    public QuizType getTableType(String name) {
        for (QuizTable table : tables) {
            if (table.name.equalsIgnoreCase(name)) {
                return table.type;
            }
        }
        return null;
    }


    @Override
    public boolean addChoiceQuiz(String name, String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        QuizTable table = findTargetTable(name);
        if (table == null || table.type != QuizType.Choice) {
            ServerQuiz.logger.warning("[" + name + "] is not a Choice quiz table!");
            return false;
        }

        String sql = "INSERT INTO `" + table.name + "`(question, option_a, option_b, option_c, option_d, answer, score, reward) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, optionA);
            statement.setString(3, optionB);
            statement.setString(4, optionC);
            statement.setString(5, optionD);
            statement.setString(6, answer);
            statement.setInt(7, score);
            statement.setString(8, reward);

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Insert choice quiz failed!" + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addBlankQuiz(String name, String question, String answer, int score, String reward) {
        QuizTable table = findTargetTable(name);
        if (table == null || table.type != QuizType.Blank) {
            ServerQuiz.logger.warning("[" + name + "] is not a Blank quiz table!");
            return false;
        }

        String sql = "INSERT INTO `" + table.name + "`(question, answer, score, reward) VALUES (?, ?, ?, ?)";

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, answer);
            statement.setInt(3, score);
            statement.setString(4, reward);

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Insert blank quiz failed!" + e.getMessage());
            return false;
        }
    }

    // 根据全局编号找到指定表内的 id，不属于该表时返回 null
    private Integer findLocalIdForGlobal(QuizTable table, int globalId) {
        int index = globalId - 1 - table.offset;
        if (index < 0 || index >= table.ids.size()) return null;
        return table.ids.get(index);
    }

    @Override
    public boolean updateChoiceQuiz(String name, int id, String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward) {
        QuizTable table = findTargetTable(name);
        if (table == null || table.type != QuizType.Choice) return false;
        Integer localId = findLocalIdForGlobal(table, id);
        if (localId == null) return false;

        String sql = "UPDATE `" + table.name + "` SET question = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?, answer = ?, score = ?, reward = ? WHERE id = ?";

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, optionA);
            statement.setString(3, optionB);
            statement.setString(4, optionC);
            statement.setString(5, optionD);
            statement.setString(6, answer);
            statement.setInt(7, score);
            statement.setString(8, reward);
            statement.setInt(9, localId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Update choice quiz failed! " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean updateBlankQuiz(String name, int id, String question, String answer, int score, String reward) {
        QuizTable table = findTargetTable(name);
        if (table == null || table.type != QuizType.Blank) return false;
        Integer localId = findLocalIdForGlobal(table, id);
        if (localId == null) return false;

        String sql = "UPDATE `" + table.name + "` SET question = ?, answer = ?, score = ?, reward = ? WHERE id = ?";

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, answer);
            statement.setInt(3, score);
            statement.setString(4, reward);
            statement.setInt(5, localId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Update blank quiz failed! " + e.getMessage());
            return false;
        }
    }

    // 更新计分表
    @Override
    public boolean updateScoreBoard(String name, boolean check, int score) {
        String sql;

        if (check) {
            sql = """
                  UPDATE score
                  SET correct_answers = correct_answers+1, all_answers = all_answers+1, scores = scores+?
                  WHERE name = ?;
                 """;
        } else {
            sql = """
                 UPDATE score
                 SET all_answers = all_answers+1
                 WHERE name = ?;
                """;
        }

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (check) {
                statement.setInt(1, score);
                statement.setString(2, name);
            } else {
                statement.setString(1, name);
            }

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Failed to update score board!" + e.getMessage());
            return false;
        }
    }

    // 关闭数据库连接
    @Override
    public void closeQuiz() {
        tables.clear();
        maxId = 0;
        dataSourceManager.close();
        ServerQuiz.logger.info("Quiz list closed!");
    }
}
