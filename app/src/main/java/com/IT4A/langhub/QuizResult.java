package com.IT4A.langhub;

public class QuizResult {
    private String username;
    private int score;
    private long timestamp;
    private int totalQuestions;

    // Constructor with 3 parameters
    public QuizResult(String username, int score, long timestamp) {
        this.username = username;
        this.score = score;
        this.timestamp = timestamp;
        this.totalQuestions = 0;  // Assign a default value if not provided
    }

    // Constructor with 4 parameters
    public QuizResult(String username, int score, int totalQuestions, long timestamp) {
        this.username = username;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.timestamp = timestamp;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }
}
