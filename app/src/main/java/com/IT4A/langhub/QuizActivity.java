package com.IT4A.langhub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private List<Question> questionsList;
    private int currentQuestionIndex = 0;
    private int score = 0;

    private TextView questionTextView, timerTextView;
    private RadioGroup choicesRadioGroup;
    private Button nextButton;
    private EditText fixedAnswerEditText;  // EditText for Fixed Answer questions
    private Button micButton;  // Button for voice input (if required)
    private ImageView questionImageView;  // ImageView for displaying the downloaded image
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 60000; // 1 minute

    private List<String> userAnswers = new ArrayList<>();
    private List<String> correctAnswers = new ArrayList<>();

    private String selectedDifficulty;
    private String selectedLanguage;

    // TextToSpeech and SpeechRecognizer
    private TextToSpeech textToSpeech;
    private SpeechRecognizer speechRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        db = FirebaseFirestore.getInstance();
        questionsList = new ArrayList<>();

        questionTextView = findViewById(R.id.questionTextView);
        timerTextView = findViewById(R.id.timerTextView);
        choicesRadioGroup = findViewById(R.id.choicesRadioGroup);
        nextButton = findViewById(R.id.nextButton);
        fixedAnswerEditText = findViewById(R.id.fixedAnswerEditText);  // Initialize EditText
        micButton = findViewById(R.id.micButton);  // Initialize micButton for voice input
        questionImageView = findViewById(R.id.questionImageView); // Initialize ImageView

        selectedDifficulty = getIntent().getStringExtra("difficulty");
        selectedLanguage = getIntent().getStringExtra("language");

        if (selectedDifficulty == null || selectedLanguage == null) {
            Toast.makeText(this, "Error: Missing quiz details!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Initialize TTS
        initializeTTS();

        // Initialize SpeechRecognizer
        initializeSpeechRecognizer();

        // Load questions based on selected difficulty and language
        loadQuestions();

        nextButton.setOnClickListener(v -> handleNextButton());

        // Handle mic button click for voice input
        micButton.setOnClickListener(v -> startVoiceInput());
    }

    private void initializeTTS() {
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int langResult = textToSpeech.setLanguage(Locale.getDefault());
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language is not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());  // Remove the 'this' parameter
    }
    
    private void startVoiceInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenAnswer = matches.get(0);
                fixedAnswerEditText.setText(spokenAnswer);
                textToSpeech.speak("You said: " + spokenAnswer, TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                finishQuiz();
            }
        }.start();
    }

    private void updateTimer() {
        int seconds = (int) (timeLeftInMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        timerTextView.setText(timeFormatted);
    }

    private void loadQuestions() {
        db.collection("questions")
            .whereEqualTo("difficulty", selectedDifficulty)
            .whereEqualTo("language", selectedLanguage)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    QuerySnapshot querySnapshot = task.getResult();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String questionText = document.getString("question");
                        List<String> choicesList = (List<String>) document.get("choices");
                        String correctAnswer = document.getString("correctAnswer");
                        String questionType = document.getString("questionType");
                        String url = document.getString("url");  // Make sure this is retrieved from Firestore
                        String imgUrl = document.getString("imgUrl");  // Retrieve the image URL
    
                        // Create a Question object
                        if (questionText != null && choicesList != null && correctAnswer != null && questionType != null) {
                            ArrayList<String> choices = new ArrayList<>(choicesList);
    
                            // Make sure you're calling the correct constructor
                            questionsList.add(new Question(questionText, choices, correctAnswer, selectedDifficulty, selectedLanguage, questionType, url, imgUrl));
                        }
                    }
    
                    if (!questionsList.isEmpty()) {
                        displayQuestion();
                        startTimer();
                    } else {
                        Toast.makeText(QuizActivity.this, "No questions available for this quiz.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else {
                    Toast.makeText(QuizActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
                }
            });
    }
    

    private void displayQuestion() {
        if (currentQuestionIndex < questionsList.size()) {
            Question question = questionsList.get(currentQuestionIndex);
            questionTextView.setText(question.getQuestion());
    
            // Check if an image is available for this question
            String imgUrl = question.getImgUrl();
            if (imgUrl != null && !imgUrl.isEmpty()) {
                // Replace non-alphanumeric characters and use the same filename as before
                String safeFileName = question.getQuestion().replaceAll("[^a-zA-Z0-9]", "_") + "_quiz_image.jpg";
                File imageFile = new File(getFilesDir(), safeFileName);
    
                // Check if the image exists
                if (imageFile.exists()) {
                    // Image exists, decode and show it
                    Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                    if (bitmap != null) {
                        questionImageView.setImageBitmap(bitmap);
                        questionImageView.setVisibility(View.VISIBLE); // Make ImageView visible
                    } else {
                        // If the bitmap is null (corrupted image or failure to decode)
                        questionImageView.setVisibility(View.GONE);
                        Log.e("QuizActivity", "Failed to decode image");
                    }
                } else {
                    // Image file does not exist
                    questionImageView.setVisibility(View.GONE);
                    Log.e("QuizActivity", "Image file not found: " + imageFile.getAbsolutePath());
                }
            } else {
                // No image URL provided, hide ImageView
                questionImageView.setVisibility(View.GONE);
            }
    
            // Continue with displaying question and choices (same as before)
            if (question.getQuestionType().equals("Multiple Choice")) {
                choicesRadioGroup.setVisibility(View.VISIBLE);
                fixedAnswerEditText.setVisibility(View.GONE);
                micButton.setVisibility(View.GONE);
    
                choicesRadioGroup.removeAllViews();
                for (String choice : question.getChoices()) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setText(choice);
                    radioButton.setId(View.generateViewId());
                    choicesRadioGroup.addView(radioButton);
                }
            } else if (question.getQuestionType().equals("Fixed Answer")) {
                choicesRadioGroup.setVisibility(View.GONE);
                fixedAnswerEditText.setVisibility(View.VISIBLE);
                micButton.setVisibility(View.VISIBLE);
            }
        }
    }
    

    private void handleNextButton() {
        Question currentQuestion = questionsList.get(currentQuestionIndex);

        if (currentQuestion.getQuestionType().equals("Multiple Choice")) {
            int selectedId = choicesRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, "Please select an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedId);
            String selectedAnswer = selectedRadioButton.getText().toString().trim();
            if (selectedAnswer.equalsIgnoreCase(currentQuestion.getCorrectAnswer())) {
                score++;
            }

            if (!userAnswers.contains(selectedAnswer.toLowerCase())) {
                userAnswers.add(selectedAnswer.toLowerCase());
            }
            correctAnswers.add(currentQuestion.getCorrectAnswer().toLowerCase());

        } else if (currentQuestion.getQuestionType().equals("Fixed Answer")) {
            String userAnswer = fixedAnswerEditText.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userAnswer.equalsIgnoreCase(currentQuestion.getCorrectAnswer())) {
                score++;
            }

            if (!userAnswers.contains(userAnswer.toLowerCase())) {
                userAnswers.add(userAnswer.toLowerCase());
            }
            correctAnswers.add(currentQuestion.getCorrectAnswer().toLowerCase());
        }

        currentQuestionIndex++;

        if (currentQuestionIndex < questionsList.size()) {
            displayQuestion();
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {
        countDownTimer.cancel();

        Intent intent = new Intent(QuizActivity.this, ScoreActivity.class);
        intent.putExtra("score", score);
        intent.putStringArrayListExtra("userAnswers", new ArrayList<>(userAnswers));
        intent.putStringArrayListExtra("correctAnswers", new ArrayList<>(correctAnswers));
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Release resources
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (textToSpeech != null) {
            textToSpeech.shutdown();
        }
    }
}
