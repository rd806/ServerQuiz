package org.rd806.quizplugin.quiz;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.rd806.quizplugin.QuizPlugin;
import org.rd806.quizplugin.database.DataSourceManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlControl {

    private final DataSourceManager dataSourceManager;
    private int maxId;

    public SqlControl(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.maxId = 0;
    }

    // 初始化Quiz列表
    public void setQuiz() {
        String sql = "SELECT MAX(id) FROM quiz";

        try(Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                this.maxId = resultSet.getInt(1);
            }

            QuizPlugin.logger.info("Quiz list initialized!");
            QuizPlugin.logger.info("Quiz number: " + this.maxId);

        } catch (SQLException e) {
            QuizPlugin.logger.warning("Quiz list initialized failed!" + e.getMessage());
        }
    }

    // 根据id获取对应的Quiz
    public void getQuizById(int id) {
        String sql = "SELECT * FROM quiz WHERE id = ?";

        try(Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                // 设置问题
                QuizPlugin.main.quiz.setQuestion(resultSet.getString("question"));
                QuizPlugin.main.quiz.setAnswer(resultSet.getString("answer"));
                // 设置选项
                List<String> options = new ArrayList<>();
                options.add(resultSet.getString("option_a"));
                options.add(resultSet.getString("option_b"));
                options.add(resultSet.getString("option_c"));
                options.add(resultSet.getString("option_d"));
                QuizPlugin.main.quiz.setOptions(options);
                // 设置奖励
                String rewardName = resultSet.getString("reward");
                Material rewardMaterial = Material.matchMaterial(rewardName.toUpperCase());
                if (rewardMaterial != null) {
                    ItemStack rewardItem = new ItemStack(rewardMaterial);
                    QuizPlugin.main.quiz.setReward(rewardItem);
                }
            }

        } catch (SQLException e) {
            QuizPlugin.logger.warning("Quiz list initialized failed!" + e.getMessage());
        }

    }

    // 关闭数据库连接
    public void closeQuiz() {
        dataSourceManager.close();
        QuizPlugin.logger.info("Quiz list closed!");
    }

    public int getMaxId() { return maxId; }
}
