package org.rd806.serverquiz.quiz.storage;

import org.rd806.serverquiz.quiz.content.QuizEntry;

public interface QuizStorage {

    void setQuiz();
    void closeQuiz();
    QuizEntry getQuizById(int num);

    boolean addChoiceQuiz(String question, String optionA, String optionB, String optionC, String optionD, String answer, String reward);
    boolean addBlankQuiz(String question, String answer, String reward);
}
