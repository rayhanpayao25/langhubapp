package com.IT4A.langhub;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class QuizDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "quiz_database";
    private static final int DATABASE_VERSION = 4;

    public static final String TABLE_NAME = "quiz_results";
    public static final String COLUMN_NAME_ID = "id";
    public static final String COLUMN_NAME_SCORE = "score";
    public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_TOTAL_QUESTIONS = "total_questions";
    public static final String COLUMN_NAME_LEVEL = "level";
    public static final String COLUMN_NAME_DIFFICULTY = "difficulty";
    public static final String COLUMN_NAME_LANGUAGE = "language";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_NAME_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_NAME_SCORE + " INTEGER," +
                    COLUMN_NAME_TOTAL_QUESTIONS + " INTEGER," +
                    COLUMN_NAME_TIMESTAMP + " INTEGER," +
                    COLUMN_NAME_LEVEL + " TEXT," +
                    COLUMN_NAME_DIFFICULTY + " TEXT," +
                    COLUMN_NAME_LANGUAGE + " TEXT)";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;

    public QuizDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    public void insertQuizResult( int score, int totalQuestions, String difficulty, String level, String language) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME_SCORE, score);
        values.put(COLUMN_NAME_TOTAL_QUESTIONS, totalQuestions);
        values.put(COLUMN_NAME_TIMESTAMP, System.currentTimeMillis());
        values.put(COLUMN_NAME_LEVEL, level);
        values.put(COLUMN_NAME_DIFFICULTY, difficulty);
        values.put(COLUMN_NAME_LANGUAGE, language);

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    public void deleteQuizHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void deleteAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
        db.close();
    }

    public void deleteHistoryByLanguage(String language) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = COLUMN_NAME_LANGUAGE + " = ?";
        String[] whereArgs = {language};
        db.delete(TABLE_NAME, whereClause, whereArgs);
        db.close();
    }

    public Cursor getTopQuizHistory(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                COLUMN_NAME_TIMESTAMP + " DESC",
                String.valueOf(limit)
        );
    }

    public List<QuizResult> getAllQuizResults() {
        List<QuizResult> quizResults = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_NAME,
                null,
                null,
                null,
                null,
                null,
                COLUMN_NAME_TIMESTAMP + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") QuizResult result = new QuizResult(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),

                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_SCORE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_TOTAL_QUESTIONS)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_LEVEL)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DIFFICULTY)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_LANGUAGE))
                );
                quizResults.add(result);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return quizResults;
    }

    public List<QuizResult> getQuizResultsByLanguage(String language) {
        List<QuizResult> quizResults = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String selection = COLUMN_NAME_LANGUAGE + " = ?";
        String[] selectionArgs = {language};

        Cursor cursor = db.query(
                TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                COLUMN_NAME_TIMESTAMP + " DESC"
        );

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") QuizResult result = new QuizResult(
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_ID)),

                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_SCORE)),
                        cursor.getInt(cursor.getColumnIndex(COLUMN_NAME_TOTAL_QUESTIONS)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_NAME_TIMESTAMP)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_LEVEL)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_DIFFICULTY)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_NAME_LANGUAGE))
                );
                quizResults.add(result);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return quizResults;
    }

    public int getAverageScoreByLanguage(String language) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT AVG(" + COLUMN_NAME_SCORE + ") FROM " + TABLE_NAME +
                " WHERE " + COLUMN_NAME_LANGUAGE + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{language});

        int averageScore = 0;
        if (cursor.moveToFirst()) {
            averageScore = cursor.getInt(0);
        }

        cursor.close();
        db.close();
        return averageScore;
    }

    public static class QuizResult {
        private int id;
        private int score;
        private int totalQuestions;
        private long timestamp;
        private String level;
        private String difficulty;
        private String language;

        public QuizResult(int id, int score, int totalQuestions, long timestamp, String level, String difficulty, String language) {
            this.id = id;
            this.score = score;
            this.totalQuestions = totalQuestions;
            this.timestamp = timestamp;
            this.level = level;
            this.difficulty = difficulty;
            this.language = language;
        }

        // Getters
        public int getId() { return id; }
        public int getScore() { return score; }
        public int getTotalQuestions() { return totalQuestions; }
        public long getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getDifficulty() { return difficulty; }
        public String getLanguage() { return language; }
    }
}