package org.rd806.quizplugin.quiz.storage;

import org.rd806.quizplugin.quiz.QuizEntry;

public interface QuizStorage {

    void setQuiz();
    QuizEntry getQuizById(int num);
    void closeQuiz();
}
