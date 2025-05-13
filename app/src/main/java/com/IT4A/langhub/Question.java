package com.IT4A.langhub;

import java.util.ArrayList;

public class Question {
    private String id;
    private String question;
    private ArrayList<String> choices;
    private String correctAnswer;
    private String difficulty;
    private String language;
    private String questionType; // To track if it's MCQ or Fixed Answer
    private String url; // URL field
    private String imgUrl; // Image URL field
    private String imgName; // NEW: For local asset images
    private long timestamp; // Timestamp field for sorting

    // Empty constructor (required for Firestore)
    public Question() {}

    // Constructor for Multiple Choice with imgName
    public Question(String question, ArrayList<String> choices, String correctAnswer, String difficulty,
                    String language, String questionType, String url, String imgUrl, String imgName) {
        this.question = question;
        this.choices = choices;
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
        this.language = language;
        this.questionType = questionType;
        this.url = url;
        this.imgUrl = imgUrl;
        this.imgName = imgName;
        this.timestamp = System.currentTimeMillis(); // Initialize with current time
    }

    // Constructor for backward compatibility (without imgName)
    public Question(String question, ArrayList<String> choices, String correctAnswer, String difficulty,
                    String language, String questionType, String url, String imgUrl) {
        this(question, choices, correctAnswer, difficulty, language, questionType, url, imgUrl, "");
    }

    // Constructor for Fixed Answer with imgName
    public Question(String question, String correctAnswer, String difficulty, String language,
                    String questionType, String url, String imgUrl, String imgName) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.difficulty = difficulty;
        this.language = language;
        this.questionType = questionType;
        this.url = url;
        this.imgUrl = imgUrl;
        this.imgName = imgName;
        this.choices = new ArrayList<>(); // Empty choices list for Fixed Answer
        this.timestamp = System.currentTimeMillis(); // Initialize with current time
    }

    // Constructor for Fixed Answer without imgName (backward compatibility)
    public Question(String question, String correctAnswer, String difficulty, String language,
                    String questionType, String url, String imgUrl) {
        this(question, correctAnswer, difficulty, language, questionType, url, imgUrl, "");
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public ArrayList<String> getChoices() {
        return choices;
    }

    public void setChoices(ArrayList<String> choices) {
        this.choices = choices;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImgUrl() {
        return imgUrl;
    }

    public void setImgUrl(String imgUrl) {
        this.imgUrl = imgUrl;
    }

    public String getImgName() {
        return imgName;
    }

    public void setImgName(String imgName) {
        this.imgName = imgName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}