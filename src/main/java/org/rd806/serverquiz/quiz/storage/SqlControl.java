package org.rd806.serverquiz.quiz.storage;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.rd806.serverquiz.ServerQuiz;
import org.rd806.serverquiz.database.DataSourceManager;
import org.rd806.serverquiz.quiz.QuizEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SqlControl implements QuizStorage {

    private final DataSourceManager dataSourceManager;
    private int maxId;

    public SqlControl(DataSourceManager dataSourceManager) {
        this.dataSourceManager = dataSourceManager;
        this.maxId = 0;
    }

    // 初始化Quiz列表
    @Override
    public void setQuiz() {
        String sql = "SELECT MAX(id) FROM quiz";

        try(Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                this.maxId = resultSet.getInt(1);
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
        String sql = "SELECT * FROM quiz WHERE id = ?";

        try (Connection connection = dataSourceManager.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                QuizEntry temp = new QuizEntry();
                // 设置问题
                temp.setId(resultSet.getInt("id"));
                temp.setQuestion(resultSet.getString("question"));
                temp.setAnswer(resultSet.getString("answer"));
                // 设置选项
                List<String> options = new ArrayList<>();
                options.add(resultSet.getString("option_a"));
                options.add(resultSet.getString("option_b"));
                options.add(resultSet.getString("option_c"));
                options.add(resultSet.getString("option_d"));
                temp.setOptions(options);
                // 设置奖励
                String rewardName = resultSet.getString("reward");
                Material rewardMaterial = Material.matchMaterial(rewardName.toUpperCase());
                if (rewardMaterial != null) {
                    ItemStack rewardItem = new ItemStack(rewardMaterial);
                    temp.setReward(rewardItem);
                }
                return temp;
            }
        } catch (SQLException e) {
            ServerQuiz.logger.warning("Quiz list initialized failed!" + e.getMessage());
        }
        return null;
    }

    // 关闭数据库连接
    @Override
    public void closeQuiz() {
        dataSourceManager.close();
        ServerQuiz.logger.info("Quiz list closed!");
    }
}
