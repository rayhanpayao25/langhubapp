package com.IT4A.langhub;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Button;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;

public class QuizSelectionActivity extends AppCompatActivity {

    private LinearLayout quizButtonsLayout;
    private FirebaseFirestore db;
    private HashSet<String> quizCombinations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_selection);

        db = FirebaseFirestore.getInstance();
        quizButtonsLayout = findViewById(R.id.quizButtonsLayout);
        quizCombinations = new HashSet<>();

        fetchQuizCombinations();
    }

    private void fetchQuizCombinations() {
        db.collection("questions").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String difficulty = document.getString("difficulty");
                    String language = document.getString("language");
                    String imgUrl = document.getString("imgUrl");
                    String question = document.getString("question");

                    // Check if the image already exists before downloading
                    if (imgUrl != null) {
                        checkAndDownloadImage(imgUrl, question); // Pass both imgUrl and question
                    }

                    // Adding buttons for quiz
                    if (difficulty != null && language != null) {
                        String combination = difficulty + " - " + language;
                        if (!quizCombinations.contains(combination)) {
                            quizCombinations.add(combination);
                            addQuizButton(difficulty, language);
                        }
                    }
                }
            }
        });
    }

    private void addQuizButton(String difficulty, String language) {
        Button quizButton = new Button(this);
        quizButton.setText("Quiz - " + difficulty + " " + language);
        quizButton.setOnClickListener(view -> startQuizConfirmation(difficulty, language));

        quizButtonsLayout.addView(quizButton);
    }

    private void startQuizConfirmation(String difficulty, String language) {
        Intent intent = new Intent(QuizSelectionActivity.this, QuizStartActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("language", language);
        startActivity(intent);
    }

    private void checkAndDownloadImage(String imageUrl, String question) {
        new Thread(() -> {
            File directory = getFilesDir(); // App's internal storage
            String fileName = question.replaceAll("[^a-zA-Z0-9]", "_") + "_quiz_image.jpg";
            File imageFile = new File(directory, fileName);

            // Check if the image file already exists
            if (!imageFile.exists()) {
                // If it doesn't exist, download the image
                downloadImage(imageUrl, question);
            } else {
                // If the image exists, show a toast message
                runOnUiThread(() -> Toast.makeText(QuizSelectionActivity.this, "Resources are up to date", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void downloadImage(String imageUrl, String question) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                Bitmap bitmap = BitmapFactory.decodeStream(url.openStream());

                // Save image to internal storage with the question name as part of the filename
                File directory = getFilesDir(); // App's internal storage
                String fileName = question.replaceAll("[^a-zA-Z0-9]", "_") + "_quiz_image.jpg";
                File imageFile = new File(directory, fileName);

                try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                }

                // Display a toast to confirm download
                runOnUiThread(() -> Toast.makeText(QuizSelectionActivity.this, "Resources Downloaded", Toast.LENGTH_SHORT).show());

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(QuizSelectionActivity.this, "Error downloading image", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
