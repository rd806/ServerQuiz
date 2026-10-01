package org.rd806.serverquiz.storage;

import org.rd806.serverquiz.quiz.ScoreData;
import org.rd806.serverquiz.quiz.QuizEntry;

import java.util.UUID;

/// 存储接口
public interface QuizStorage {

    void setQuiz();
    void closeQuiz();
    QuizEntry getQuizById(int num);

    boolean createScoreBoard(String name, UUID uuid);
    ScoreData getPlayerScore(String name);
    /// 更新计分表，答对时额外累加 score 分数
    boolean updateScoreBoard(String name, boolean check, int score);

    boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, int score, String reward);
    boolean addBlankQuiz(String question, String answer, int score, String reward);
}
