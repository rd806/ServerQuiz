package org.rd806.serverquiz.quiz.storage;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.quiz.content.RewardData;
import org.rd806.serverquiz.quiz.content.ScoreData;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.database.DataSourceManager;
import org.rd806.serverquiz.quiz.content.QuizEntry;
import org.rd806.serverquiz.quiz.content.QuizType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqlControl implements QuizStorage {

    private final DataSourceManager dataSourceManager;
    private int choiceId;
    private int blankId;
    private int maxId;

    public SqlControl(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.choiceId = 0;
        this.blankId = 0;
        this.maxId = 0;
    }

    // 初始化Quiz列表
    @Override
    public void setQuiz() {
        String sql1 = "SELECT MAX(id) FROM choice";
        String sql2 = "SELECT MAX(id) FROM blank";

        try(Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement1 = connection.prepareStatement(sql1);
            PreparedStatement statement2 = connection.prepareStatement(sql2);
            ResultSet resultSet1 = statement1.executeQuery();
            ResultSet resultSet2 = statement2.executeQuery()) {

            if (resultSet1.next() && resultSet2.next()) {
                this.choiceId = resultSet1.getInt(1);
                this.blankId = resultSet2.getInt(1);
                this.maxId = choiceId + blankId;
                ServerQuiz.main.quizConfig.setMaxNum(this.maxId);
            }

            ServerQuiz.logger.info("Quiz list initialized!");
            ServerQuiz.logger.info("Quiz number: " + this.maxId);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Quiz list initialized failed!" + e.getMessage());
        }
    }

    // 根据id获取对应的Quiz
    @Override
    public QuizEntry getQuizById(int id) {
        String sql;

        if (id < 1 || id > maxId) {
            ServerQuiz.logger.warning("Quiz id out of range!");
            return null;
        }

        if (id <= choiceId) {
            sql = """
                SELECT 'choice' AS type, id, question, option_a, option_b, option_c, option_d, answer, reward
                FROM choice
                WHERE id = ?
                """;
        } else {
            sql = """
                SELECT 'blank' AS type, id, question, NULL AS option_a, NULL AS option_b, NULL AS option_c, NULL AS option_d, answer, reward
                FROM blank
                WHERE id = ?
                """;
        }

        try (Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            if (id <= choiceId) {
                statement.setInt(1, id);
            } else {
                statement.setInt(1, id - choiceId);
            }

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                QuizEntry temp = new QuizEntry();
                // 设置问题
                temp.setId(resultSet.getInt("id"));
                temp.setQuestion(resultSet.getString("question"));
                temp.setAnswer(resultSet.getString("answer"));

                // 设置奖励
                String rewardName = resultSet.getString("reward");
                if (RewardData.numberCheck(rewardName)) {
                    // 设置为经济Vault格式
                    temp.setReward(new RewardData(rewardName, RewardData.RewardType.Vault,  null, Integer.parseInt(rewardName)));
                } else {
                    // 设置为实物格式
                    Material rewardMaterial = Material.matchMaterial(rewardName.toUpperCase());
                    if (rewardMaterial != null) {
                        ItemStack rewardItem = new ItemStack(rewardMaterial);
                        temp.setReward(new RewardData(rewardName, RewardData.RewardType.Item,  rewardItem, 0));
                    }
                }

                String type = resultSet.getString("type");
                if (type.equals("choice")) {
                    // 设置选项
                    temp.setType(QuizType.Choice);
                    List<String> options = new ArrayList<>();
                    options.add(resultSet.getString("option_a"));
                    options.add(resultSet.getString("option_b"));
                    options.add(resultSet.getString("option_c"));
                    options.add(resultSet.getString("option_d"));
                    temp.setOptions(options);
                } else {
                    temp.setType(QuizType.Blank);
                }

                return temp;
            }
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Quiz list initialized failed!" + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean createScoreBoard(String name, UUID uuid) {
        String sql = """
              INSERT INTO score (name, uuid, correct_answers, all_answers) VALUES (?, ?, 0, 0) ON DUPLICATE KEY UPDATE id = id;
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
        ScoreData score = new ScoreData(0, 0);
        String sql = """
                SELECT correct_answers, all_answers FROM score WHERE name = ?;
                """;

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int correctAnswers = resultSet.getInt("correct_answers");
                int allAnswers = resultSet.getInt("all_answers");
                return new ScoreData(correctAnswers, allAnswers);
            }

            return score;
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Failed to get score data!" + e.getMessage());
            return null;
        }

    }

    @Override
    public boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, String reward) {
        String sql = """
               INSERT INTO choice(question, option_a, option_b, option_c, option_d, answer, reward) VALUES (?, ?, ?, ?, ?, ?, ?);
               """;

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, optionA);
            statement.setString(3, optionB);
            statement.setString(4, optionC);
            statement.setString(5, optionD);
            statement.setString(6, answer);
            statement.setString(7, reward);

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Insert choice quiz failed!" + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean addBlankQuiz(String question, String answer, String reward) {
        String sql = """
               INSERT INTO blank(question, answer, reward) VALUES (?, ?, ?);
               """;

        try (Connection connection = dataSourceManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, question);
            statement.setString(2, answer);
            statement.setString(3, reward);

            int result = statement.executeUpdate();
            return (result > 0);

        } catch (SQLException e) {
            ServerQuiz.logger.warning("Insert blank quiz failed!" + e.getMessage());
            return false;
        }
    }

    // 更新计分表
    @Override
    public boolean updateScoreBoard(String name, boolean check) {
        String sql;

        if (check) {
            sql = """
                  UPDATE score
                  SET correct_answers = correct_answers+1, all_answers = all_answers+1
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

            statement.setString(1, name);

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
        dataSourceManager.close();
        ServerQuiz.logger.info("Quiz list closed!");
    }
}
