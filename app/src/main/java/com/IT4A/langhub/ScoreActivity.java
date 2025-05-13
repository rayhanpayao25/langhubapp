package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class ScoreActivity extends AppCompatActivity {

    private TextView scoreTextView, answersTextView;
    private Button returnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        scoreTextView = findViewById(R.id.scoreTextView);
        answersTextView = findViewById(R.id.answersTextView);
        returnButton = findViewById(R.id.returnButton);

        // Retrieve the score, correct answers, and user answers from QuizActivity
        int score = getIntent().getIntExtra("score", 0);
        List<String> correctAnswers = getIntent().getStringArrayListExtra("correctAnswers");
        List<String> userAnswers = getIntent().getStringArrayListExtra("userAnswers");

        // Display the score
        scoreTextView.setText("Your Score: " + score);

        // Display the correct and incorrect answers
        StringBuilder answersDisplay = new StringBuilder();
        for (int i = 0; i < correctAnswers.size(); i++) {
            String correctAnswer = correctAnswers.get(i);
            String userAnswer = userAnswers.get(i);

            answersDisplay.append("Question " + (i + 1) + ":\n");
            answersDisplay.append("Correct Answer: " + correctAnswer + "\n");
            answersDisplay.append("Your Answer: " + userAnswer + "\n");

            // Check if the answer is correct or wrong
            if (!correctAnswer.equals(userAnswer)) {
                answersDisplay.append("Result: Incorrect\n\n");
            } else {
                answersDisplay.append("Result: Correct\n\n");
            }
        }

        // Set the answers display text
        answersTextView.setText(answersDisplay.toString());

        // Set the return button to go back to the home screen
        returnButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScoreActivity.this, HomepageActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
