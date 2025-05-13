package com.IT4A.langhub;

import android.provider.BaseColumns;

final class QuizContract {

    private QuizContract() {}

    public static class QuizEntry implements BaseColumns {
        public static final String TABLE_NAME = "quiz_results";
        public static final String COLUMN_NAME_QUESTION = "question";
        public static final String COLUMN_NAME_USER_ANSWER = "user_answer";
        public static final String COLUMN_NAME_CORRECT_ANSWER = "correct_answer";
        public static final String COLUMN_NAME_SCORE = "score";
        public static final String COLUMN_NAME_USERNAME = "username";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_TOTAL_QUESTIONS = "total_questions";
        public static final String COLUMN_NAME_QUIZ_TYPE = "quiz_type";
        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_ANSWER = "answer";




            // ... other code ...



    }
}


