package org.rd806.serverquiz.quiz.storage;

import org.rd806.serverquiz.quiz.content.QuizEntry;

public interface QuizStorage {

    void setQuiz();
    QuizEntry getQuizById(int num);
    void closeQuiz();
}
