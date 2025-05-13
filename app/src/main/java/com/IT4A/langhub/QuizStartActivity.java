package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class QuizStartActivity extends AppCompatActivity {

    private Button startQuizButton;
    private Button cancelButton;
    private TextView selectedDifficultyTextView;
    private TextView selectedLanguageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_start);

        startQuizButton = findViewById(R.id.startQuizButton);
        cancelButton = findViewById(R.id.cancelButton);
        selectedDifficultyTextView = findViewById(R.id.selectedDifficultyTextView);
        selectedLanguageTextView = findViewById(R.id.selectedLanguageTextView);

        // Retrieve selected difficulty and language from intent
        String selectedDifficulty = getIntent().getStringExtra("difficulty");
        String selectedLanguage = getIntent().getStringExtra("language");

        // Ensure data is received, otherwise show error
        if (selectedDifficulty == null || selectedLanguage == null) {
            Toast.makeText(this, "Error: Missing quiz details!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Display selected difficulty and language
        selectedDifficultyTextView.setText("Difficulty: " + selectedDifficulty);
        selectedLanguageTextView.setText("Language: " + selectedLanguage);

        // Start quiz when the user confirms they're ready
        startQuizButton.setOnClickListener(view -> {
            Intent intent = new Intent(QuizStartActivity.this, QuizActivity.class);
            intent.putExtra("difficulty", selectedDifficulty);
            intent.putExtra("language", selectedLanguage);
            startActivity(intent);
            finish();
        });

        // Go back to selection if the user cancels
        cancelButton.setOnClickListener(view -> {
            Intent intent = new Intent(QuizStartActivity.this, QuizSelectionActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
