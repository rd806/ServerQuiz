package org.rd806.serverquiz.quiz.storage;

import org.rd806.serverquiz.ScoreData;
import org.rd806.serverquiz.quiz.content.QuizEntry;

import java.util.UUID;

public interface QuizStorage {

    void setQuiz();
    void closeQuiz();
    QuizEntry getQuizById(int num);

    boolean createScoreBoard(String name, UUID uuid);
    ScoreData getPlayerScore(String name);
    boolean updateScoreBoard(String name, boolean check);

    boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, String reward);
    boolean addBlankQuiz(String question, String answer, String reward);
}
