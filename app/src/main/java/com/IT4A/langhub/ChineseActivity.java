package com.IT4A.langhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ChineseActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private List<Question> questionsList;
    private int currentQuestionIndex = 0;
    private int score = 0;

    private TextView questionTextView, timerTextView;
    private RadioGroup choicesRadioGroup, difficultyGroup;
    private RadioButton easyRadio, normalRadio, hardRadio;
    private Button nextButton, submitButton;
    private EditText fixedAnswerEditText;
    private ImageButton micButton;
    private ImageView questionImageView;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis = 1000; // 1 minute
    private LinearLayout quizContentLayout;
    private ProgressBar progressBar;

    private QuizDatabaseHelper dbHelper;

    private List<String> userAnswers = new ArrayList<>();
    private List<String> correctAnswers = new ArrayList<>();

    private String selectedDifficulty = "Easy";  // Default difficulty
    private String selectedLanguage = "Chinese"; // Default language
    private boolean quizStarted = false;

    private static final String PREFS_NAME = "QuizScores";
    private static final String CHINESE_EASY_PASSED = "chineseEasyPassed";
    private static final String CHINESE_NORMAL_PASSED = "chineseNormalPassed";

    private boolean easyPassed = false;
    private boolean normalPassed = false;

    private Button retakeButton, startQuizButton;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SpeechRecognizer speechRecognizer;
    private MediaPlayer mediaPlayer;

    private static final String KEY_CHINESE_QUIZ_SCORE = "ChineseQuizScore";

    // TextToSpeech and SpeechRecognizer

    private TextView difficultyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chinese);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chinese Quiz");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new QuizDatabaseHelper(this);
        db = FirebaseFirestore.getInstance();
        questionsList = new ArrayList<>();

        // Initialize UI components
        questionTextView = findViewById(R.id.questionTextView);
        timerTextView = findViewById(R.id.timerTextView);
        choicesRadioGroup = findViewById(R.id.choicesRadioGroup);
        difficultyGroup = findViewById(R.id.difficulty_group);
        easyRadio = findViewById(R.id.easy_radio);
        normalRadio = findViewById(R.id.normal_radio);
        hardRadio = findViewById(R.id.hard_radio);
        nextButton = findViewById(R.id.nextButton);
        fixedAnswerEditText = findViewById(R.id.fixedAnswerEditText);
        micButton = findViewById(R.id.micButton);
        questionImageView = findViewById(R.id.questionImageView);
        retakeButton = findViewById(R.id.retakeButton);
        startQuizButton = findViewById(R.id.start_quiz_button);
        progressBar = findViewById(R.id.progressBar);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        easyPassed = prefs.getBoolean(CHINESE_EASY_PASSED, false);
        normalPassed = prefs.getBoolean(CHINESE_NORMAL_PASSED, false);

        easyRadio.setEnabled(true);

        Button proceedButton = findViewById(R.id.proceedButton);
        proceedButton.setOnClickListener(v -> {
            Log.d("ChineseActivity", "Proceed button clicked. Current difficulty: " + selectedDifficulty);

            // Change difficulty level based on current level
            if (selectedDifficulty.equalsIgnoreCase("easy")) {
                selectedDifficulty = "Normal";
                normalRadio.setChecked(true);
                easyRadio.setChecked(false);
                hardRadio.setChecked(false);
                updateDifficultyText("Normal");
            } else if (selectedDifficulty.equalsIgnoreCase("normal")) {
                selectedDifficulty = "Hard";
                hardRadio.setChecked(true);
                easyRadio.setChecked(false);
                normalRadio.setChecked(false);
                updateDifficultyText("Hard");
            }

            Log.d("ChineseActivity", "New difficulty set to: " + selectedDifficulty);

            // Hide result view and proceed button
            findViewById(R.id.resultView).setVisibility(View.GONE);
            proceedButton.setVisibility(View.GONE);

            // Reset quiz state completely
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null;
            }

            // Clear all data
            currentQuestionIndex = 0;
            score = 0;
            userAnswers.clear();
            correctAnswers.clear();

            // Important: Create a new questions list instead of just clearing
            questionsList = new ArrayList<>();

            Log.d("ChineseActivity", "Quiz state reset. Questions list size: " + questionsList.size());

            // Show quiz content
            quizContentLayout.setVisibility(View.VISIBLE);
            questionTextView.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.VISIBLE);
            nextButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);

            // Load questions for the new difficulty
            loadQuestions();
        });

        startQuizButton.setOnClickListener(v -> {
            int selectedDifficultyId = difficultyGroup.getCheckedRadioButtonId();
            if (selectedDifficultyId == -1) {
                Toast.makeText(ChineseActivity.this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedDifficultyButton = findViewById(selectedDifficultyId);
            selectedDifficulty = selectedDifficultyButton.getText().toString().toLowerCase();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("selectedLevel", selectedDifficulty);
            editor.apply();

            startQuizButton.setVisibility(View.GONE);
            difficultyGroup.setVisibility(View.GONE);
            submitButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);

            loadQuestions();
        });

        normalRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!easyPassed) {
                    // Show popup message
                    Toast.makeText(ChineseActivity.this, "You need to pass the easy level to unlock this", Toast.LENGTH_SHORT).show();
                    normalRadio.setChecked(false);
                    // Ensure ang easy radio button remains checked
                    easyRadio.setChecked(true);
                }
            }
        });

        hardRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!normalPassed) {
                    Toast.makeText(ChineseActivity.this, "You need to pass the normal level to unlock this", Toast.LENGTH_SHORT).show();
                    hardRadio.setChecked(false);
                    if (easyPassed) {
                        normalRadio.setChecked(true);
                    } else {
                        easyRadio.setChecked(true);
                    }
                }
            }
        });

        // We're removing the click listener here since we'll set it in finishQuiz()

        difficultyTextView = findViewById(R.id.difficulty);
        updateDifficultyText("Easy");

        // Find the quiz content layout - this is the LinearLayout inside the second CardView
        // that has visibility="gone" in the XML
        CardView quizCardView = (CardView) findViewById(R.id.choicesRadioGroup).getParent().getParent().getParent();
        quizContentLayout = (LinearLayout) quizCardView.getChildAt(0);

        // Set default difficulty
        easyRadio.setChecked(true);

        // Initialize SpeechRecognizer
        initializeSpeechRecognizer();

        // Setup difficulty selection
        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.easy_radio) {
                selectedDifficulty = "Easy";
                updateDifficultyText("Easy");
            } else if (checkedId == R.id.normal_radio) {
                selectedDifficulty = "Normal";
                updateDifficultyText("Normal");
            } else if (checkedId == R.id.hard_radio) {
                selectedDifficulty = "Hard";
                updateDifficultyText("Hard");
            }

            // If quiz is already started, reset it when difficulty changes
            if (quizStarted) {
                resetQuiz();
                loadQuestions();
            }
        });

        // Setup start quiz button
        startQuizButton.setOnClickListener(v -> {
            startQuiz();
            SharedPreferences.Editor editor = prefs.edit();
        });

        nextButton.setOnClickListener(v -> handleNextButton());

        // Handle mic button click for voice input
        micButton.setOnClickListener(v -> startVoiceInput());
    }

    private void updateDifficultyText(String difficulty) {
        if (difficultyTextView != null) {
            String text = "Choose level of Difficulty: " + difficulty;

            // Set color based on difficulty
            int color;
            if (difficulty.equals("Easy")) {
                color = getResources().getColor(R.color.easy_color);
            } else if (difficulty.equals("Normal")) {
                color = getResources().getColor(R.color.medium_color);
            } else { // Hard
                color = getResources().getColor(R.color.hard_color);
            }

            // Set the color for the difficulty part only
            String fullText = "Choose level of Difficulty: ";
            android.text.SpannableString spannableString = new android.text.SpannableString(text);
            spannableString.setSpan(
                    new android.text.style.ForegroundColorSpan(color),
                    fullText.length(),
                    text.length(),
                    android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            difficultyTextView.setText(spannableString);
        }
    }

    private void resetQuiz() {
        Log.d("ChineseActivity", "Resetting quiz state");

        // Cancel timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }

        // Reset quiz state
        currentQuestionIndex = 0;
        score = 0;
        userAnswers.clear();
        correctAnswers.clear();

        // Create a new questions list instead of just clearing
        questionsList = new ArrayList<>();

        // Reset timer
        timeLeftInMillis = 0;
        updateTimer();

        // Reset progress bar
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);

        Log.d("ChineseActivity", "Quiz reset complete. Questions list size: " + questionsList.size());
    }

    private void startVoiceInput() {
        // Cancel any ongoing recognition
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }

        // Create intent for speech recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        // Force Chinese language with multiple approaches
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true);
        intent.putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, "zh-CN");

        // Show a toast to indicate recording is starting
        Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show(); // "Listening..." in Chinese

        // Set up recognition listener
        RecognitionListener listener = new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // UI feedback that speech recognition is ready
            }

            @Override
            public void onBeginningOfSpeech() {
                // UI feedback that speech has begun
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Update UI with voice level if desired
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Not typically used
            }

            @Override
            public void onEndOfSpeech() {
                // UI feedback that speech has ended
            }

            @Override
            public void onError(int error) {
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    fixedAnswerEditText.setText(spokenText);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                // Handle partial results if needed
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Not typically used
            }
        };

        // Start listening
        speechRecognizer.setRecognitionListener(listener);

        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(this, "错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ChineseActivity", "Error starting voice input: " + e.getMessage());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenAnswer = matches.get(0);
                fixedAnswerEditText.setText(spokenAnswer);
            }
        }
    }

    private void startTimer() {
        switch (selectedDifficulty.toLowerCase()) {
            case "easy":
                timeLeftInMillis = 20000;
                break;
            case "normal":
                timeLeftInMillis = 15000;
                break;
            case "hard":
                timeLeftInMillis = 10000;
                break;
            default:
                timeLeftInMillis = 10000;
                break;
        }

        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {
                // Instead of finishing the quiz, proceed to the next question
                if (currentQuestionIndex < questionsList.size()) {
                    // Record the current question as unanswered/incorrect
                    Question currentQuestion = questionsList.get(currentQuestionIndex);
                    userAnswers.add(""); // Empty answer since time ran out
                    correctAnswers.add(currentQuestion.getCorrectAnswer().toLowerCase());

                    // Move to the next question
                    currentQuestionIndex++;

                    // Update progress bar
                    progressBar.setProgress(currentQuestionIndex);

                    if (currentQuestionIndex < questionsList.size()) {
                        // Display the next question and start a new timer
                        displayQuestion();

                        // Add a small delay before starting the new timer
                        new android.os.Handler().postDelayed(() -> {
                            startTimer();
                        }, 100);
                    } else {
                        // If we've gone through all questions, finish the quiz
                        finishQuiz();
                    }
                } else {
                    // If we're somehow beyond the questions list, finish the quiz
                    finishQuiz();
                }
            }
        }.start();
    }

    private void updateTimer() {
        int seconds = (int) (timeLeftInMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        timerTextView.setText(timeFormatted);
    }

    private void initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                // Called when the recognizer is ready to begin listening
            }

            @Override
            public void onBeginningOfSpeech() {
                // Called when the user starts speaking
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // Called when the RMS (root mean square) of the audio input changes
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // Called when partial recognition results are available
            }

            @Override
            public void onEndOfSpeech() {
                // Called when the user stops speaking
            }

            @Override
            public void onError(int error) {
                // Called when an error occurs during recognition
                String errorMessage = "Unstable internet, kindly type you answer " + error;
                Toast.makeText(ChineseActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                // Called when partial recognition results are available
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                // Called when a recognition-related event occurs
            }
        });
    }

    private void loadQuestions() {
        // Show loading state
        questionTextView.setText("Loading questions...");

        // IMPORTANT: Clear the questions list before loading new questions
        questionsList.clear();

        // Cancel any existing timer
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Reset current question index
        currentQuestionIndex = 0;

        // Reset progress bar
        progressBar.setProgress(0);

        // Add logging to help debug
        Log.d("ChineseActivity", "Loading questions for difficulty: " + selectedDifficulty);
        Log.d("ChineseActivity", "Questions list size before Firestore query: " + questionsList.size());

        // Use a regular query instead of a transaction
        db.collection("questions")
                .whereEqualTo("difficulty", selectedDifficulty)
                .whereEqualTo("language", selectedLanguage)
                // Force a server query to avoid cache issues
                .get(com.google.firebase.firestore.Source.SERVER)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        // Create a temporary list to hold the questions
                        List<Question> tempQuestionsList = new ArrayList<>();

                        QuerySnapshot querySnapshot = task.getResult();

                        // Log the number of questions found
                        Log.d("ChineseActivity", "Found " + querySnapshot.size() + " questions");

                        // Use a Set to track document IDs to prevent duplicates
                        Set<String> processedDocIds = new HashSet<>();

                        for (QueryDocumentSnapshot document : querySnapshot) {

                            String imgName = document.getString("imgName");
                            Log.d("ChineseActivity", "Document ID: " + document.getId() + ", imgName: " + imgName);
                            String docId = document.getId();

                            // Skip if we've already processed this document
                            if (processedDocIds.contains(docId)) {
                                Log.d("ChineseActivity", "Skipping duplicate document: " + docId);
                                continue;
                            }

                            // Add to processed set
                            processedDocIds.add(docId);

                            // Log each document ID to check for duplicates
                            Log.d("ChineseActivity", "Processing document ID: " + docId);

                            String questionText = document.getString("question");
                            List<String> choicesList = (List<String>) document.get("choices");
                            String correctAnswer = document.getString("correctAnswer");
                            String questionType = document.getString("questionType");
                            String url = document.getString("url");
                            String imgUrl = document.getString("imgUrl");

// Get drawable resource name if specified in Firestore
                            String drawableResourceName = document.getString("drawableResource");


                            if (!TextUtils.isEmpty(drawableResourceName)) {
                                // Get the resource ID from the name
                                int resourceId = getResources().getIdentifier(
                                        drawableResourceName, "drawable", getPackageName());

                                if (resourceId != 0) {
                                    // Get the actual resource name from the ID
                                    imgName = getResources().getResourceEntryName(resourceId);
                                }
                            }

                            if (questionText != null && correctAnswer != null && questionType != null) {
                                ArrayList<String> choices = new ArrayList<>();
                                if (choicesList != null) {
                                    choices.addAll(choicesList);
                                }
                                tempQuestionsList.add(new Question(questionText, choices, correctAnswer,
                                        selectedDifficulty, selectedLanguage, questionType, url, imgUrl, imgName));
                            }
                        }

                        // Shuffle all the questions
                        java.util.Collections.shuffle(tempQuestionsList);

                        // Determine how many questions to use based on difficulty
                        int questionLimit;
                        if (selectedDifficulty.equalsIgnoreCase("easy")) {
                            questionLimit = 15;
                        } else if (selectedDifficulty.equalsIgnoreCase("normal")) {
                            questionLimit = 20;
                        } else { // Hard
                            questionLimit = 25;
                        }

                        // Take only the required number of questions
                        int numQuestionsToUse = Math.min(questionLimit, tempQuestionsList.size());
                        List<Question> limitedQuestionsList = tempQuestionsList.subList(0, numQuestionsToUse);

                        // Now safely update the main questions list
                        questionsList = new ArrayList<>(limitedQuestionsList);

                        // Log the final number of questions in the list
                        Log.d("ChineseActivity", "Final questionsList size: " + questionsList.size());

                        if (!questionsList.isEmpty()) {
                            // Reset user answers and score
                            userAnswers.clear();
                            correctAnswers.clear();
                            score = 0;

                            // Initialize progress bar with total questions
                            progressBar.setMax(questionsList.size());
                            progressBar.setProgress(1); // Start at 1 for first question

                            // Display the first question
                            displayQuestion();
                            startTimer();
                        } else {
                            questionTextView.setText("No questions available for this quiz.");
                            Toast.makeText(ChineseActivity.this, "No questions available for this quiz.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        questionTextView.setText("Failed to load questions. Please try again.");
                        Toast.makeText(ChineseActivity.this, "Failed to load questions", Toast.LENGTH_SHORT).show();
                        Log.e("ChineseActivity", "Error loading questions: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void displayQuestion() {
        if (currentQuestionIndex < questionsList.size()) {
            ChineseActivity.Question question = questionsList.get(currentQuestionIndex);
            String questionText = question.getQuestion();
            questionTextView.setText(questionText);

            // Reset input fields
            choicesRadioGroup.clearCheck();
            fixedAnswerEditText.setText("");

            // Update progress bar
            progressBar.setMax(questionsList.size());
            progressBar.setProgress(currentQuestionIndex + 1);

            // UPDATED IMAGE HANDLING
            // First check if we have a local asset image name
            String imgName = question.getImgName();
            String imgUrl = question.getImgUrl();

            Log.d("ChineseActivity", "Question image data - imgName: " + imgName + ", imgUrl: " + imgUrl);
            if (!TextUtils.isEmpty(imgName)) {
                int resourceId = getResources().getIdentifier(imgName, "drawable", getPackageName());
                Log.d("ChineseActivity", "Resource ID for " + imgName + ": " + resourceId);
                // Rest of your code...
                // Load from drawable resources or assets
                loadImageFromAssets(imgName);
                questionImageView.setVisibility(View.VISIBLE);
            } else if (!TextUtils.isEmpty(imgUrl)) {
                // Load from URL
                loadImageFromUrl(imgUrl);
                questionImageView.setVisibility(View.VISIBLE);
            } else {
                questionImageView.setVisibility(View.GONE);
            }

            // Set up the appropriate question type UI
            if ("Multiple Choice".equals(question.getQuestionType())) {
                choicesRadioGroup.setVisibility(View.VISIBLE);
                fixedAnswerEditText.setVisibility(View.GONE);
                micButton.setVisibility(View.GONE);

                choicesRadioGroup.removeAllViews();

                // Create a copy of the choices list so we can shuffle it without affecting the original
                ArrayList<String> shuffledChoices = new ArrayList<>(question.getChoices());
                // Shuffle the choices
                java.util.Collections.shuffle(shuffledChoices);

                // Add the shuffled choices to the radio group
                for (String choice : shuffledChoices) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setText(choice);
                    radioButton.setId(View.generateViewId());
                    choicesRadioGroup.addView(radioButton);
                }
            } else if ("Fixed Answer".equals(question.getQuestionType())) {
                choicesRadioGroup.setVisibility(View.GONE);

                // Default hint text
                fixedAnswerEditText.setHint("Enter your answer here");

                // Show mic button based on difficulty and question content
                if (selectedDifficulty.equalsIgnoreCase("easy")) {
                    // Always show mic button in Easy difficulty
                    micButton.setVisibility(View.VISIBLE);
                } else if (selectedDifficulty.equalsIgnoreCase("hard") &&
                        questionText != null &&
                        questionText.startsWith("How do you say")) {
                    // Show mic button in Hard difficulty only for "How do you say" questions
                    micButton.setVisibility(View.VISIBLE);
                    // Change hint text when mic is visible in hard difficulty
                    fixedAnswerEditText.setHint("Press the mic to speak");
                } else {
                    // Hide mic button for all other cases
                    micButton.setVisibility(View.GONE);
                }

                // Show fixedAnswerEditText for all difficulty levels
                fixedAnswerEditText.setVisibility(View.VISIBLE);
            }
        } else {
            // Handle end of questions
            questionTextView.setText("Quiz completed!");
            choicesRadioGroup.setVisibility(View.GONE);
            fixedAnswerEditText.setVisibility(View.GONE);
            submitButton.setVisibility(View.GONE);
            micButton.setVisibility(View.GONE);
        }
    }

    // Replace the loadImageFromAssets method
    private void loadImageFromAssets(String imgName) {
        if (imgName == null || imgName.isEmpty()) {
            questionImageView.setVisibility(View.GONE);
            return;
        }

        try {
            // First try to load from drawable resources
            int resourceId = getResources().getIdentifier(
                    imgName, "drawable", getPackageName());

            if (resourceId != 0) {
                // Resource exists in drawable, load it
                // Let the ImageView handle the sizing with its layout parameters
                questionImageView.setImageResource(resourceId);
                questionImageView.setVisibility(View.VISIBLE);
                Log.d("ChineseActivity", "Successfully loaded drawable: " + imgName);
            } else {
                // If not in drawable, try to load from assets
                try {
                    InputStream is = getAssets().open("images/" + imgName);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    is.close();

                    // Set the bitmap directly - ImageView will handle sizing
                    questionImageView.setImageBitmap(bitmap);
                    questionImageView.setVisibility(View.VISIBLE);
                    Log.d("ChineseActivity", "Successfully loaded from assets: " + imgName);
                } catch (IOException assetException) {
                    Log.e("ChineseActivity", "Error loading from assets: " + assetException.getMessage());

                    // If we have an image URL as fallback, try to load it
                    String imgUrl = questionsList.get(currentQuestionIndex).getImgUrl();
                    if (!TextUtils.isEmpty(imgUrl)) {
                        loadImageFromUrl(imgUrl);
                    } else {
                        questionImageView.setVisibility(View.GONE);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("ChineseActivity", "Error loading image: " + e.getMessage());
            questionImageView.setVisibility(View.GONE);
        }
    }

    // Update the loadImageFromUrl method
    private void loadImageFromUrl(String imageUrl) {
        // Show a loading indicator or placeholder
        questionImageView.setVisibility(View.VISIBLE);
        questionImageView.setImageResource(R.drawable.placeholder_image); // Use a placeholder image

        // Use an AsyncTask to download the image
        new android.os.AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String imageUrl = params[0];
                Bitmap bitmap = null;
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    Log.e("ChineseActivity", "Error loading image: " + e.getMessage());
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    // Set the bitmap directly - ImageView will handle sizing
                    questionImageView.setImageBitmap(result);
                    questionImageView.setVisibility(View.VISIBLE);
                } else {
                    questionImageView.setVisibility(View.GONE);
                    Toast.makeText(ChineseActivity.this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(imageUrl);
    }



    private void handleNextButton() {
        if (currentQuestionIndex >= questionsList.size()) {
            finishQuiz();
            return;
        }

        Question currentQuestion = questionsList.get(currentQuestionIndex);
        boolean answerProvided = false;

        if ("Multiple Choice".equals(currentQuestion.getQuestionType())) {
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

            userAnswers.add(selectedAnswer.toLowerCase());
            correctAnswers.add(currentQuestion.getCorrectAnswer().toLowerCase());
            answerProvided = true;

        } else if ("Fixed Answer".equals(currentQuestion.getQuestionType())) {
            String userAnswer = fixedAnswerEditText.getText().toString().trim();
            if (userAnswer.isEmpty()) {
                Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userAnswer.equalsIgnoreCase(currentQuestion.getCorrectAnswer())) {
                score++;
            }

            userAnswers.add(userAnswer.toLowerCase());
            correctAnswers.add(currentQuestion.getCorrectAnswer().toLowerCase());
            answerProvided = true;
        }

        if (answerProvided) {
            // First, cancel the existing timer to prevent multiple timers
            if (countDownTimer != null) {
                countDownTimer.cancel();
                countDownTimer = null; // Set to null to ensure it's fully cleared
            }

            currentQuestionIndex++;

            // Update progress bar
            progressBar.setProgress(currentQuestionIndex);

            if (currentQuestionIndex < questionsList.size()) {
                displayQuestion();

                // Add a small delay before starting the new timer to ensure the previous one is fully canceled
                new android.os.Handler().postDelayed(() -> {
                    // Start a new timer for the next question
                    startTimer();
                }, 100); // 100ms delay

            } else {
                finishQuiz();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void startQuiz() {
        // Show the quiz content
        quizContentLayout.setVisibility(View.VISIBLE);

        // Hide or disable the start button after starting
        startQuizButton.setEnabled(false);
        startQuizButton.setVisibility(View.GONE);

        // Hide the difficulty selection radio group
        difficultyGroup.setVisibility(View.GONE);

        // Make sure progress bar is visible and reset
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        // Reset quiz state
        resetQuiz();

        // Load questions based on selected difficulty
        loadQuestions();

        // Set quiz as started
        quizStarted = true;
    }

    private void saveScore(int score) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_CHINESE_QUIZ_SCORE, score);
        editor.apply();
    }

    private void showScore() {
        int totalQuestions = questionsList.size();
        double scorePercentage = (double) score / totalQuestions * 100;

        String resultMessage;
        boolean levelPassed = false;
        Button proceedButton = findViewById(R.id.proceedButton);
        proceedButton.setVisibility(View.GONE); // Hide by default

        // Determine if the level is passed based on difficulty
        if (selectedDifficulty.equalsIgnoreCase("easy")) {
            if (scorePercentage >= 53.33) {
                levelPassed = true;
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(CHINESE_EASY_PASSED, true);
                editor.apply();
                resultMessage = "<font color='#4CAF50'>Congratulations! You can proceed to the next level.</font>";

                // Show the proceed button for Easy level pass
                proceedButton.setVisibility(View.VISIBLE);
            } else {
                resultMessage = "<font color='#F44336'>You need to improve your score to proceed to the next level.</font>";
            }
        } else if (selectedDifficulty.equalsIgnoreCase("normal")) {
            if (scorePercentage >= 60) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(CHINESE_NORMAL_PASSED, true);
                editor.apply();
                normalPassed = true;
                levelPassed = true;
                resultMessage = "<font color='#4CAF50'>Passed - Congratulations! You can proceed to the next level.</font>";

                // Show the proceed button for Normal level pass
                proceedButton.setVisibility(View.VISIBLE);
            } else {
                resultMessage = "<font color='#F44336'>You need to improve your score to proceed to the next level.</font>";
                // Ensure easy level remains unlocked even if normal is failed
                easyPassed = true;
            }
        } else if (selectedDifficulty.equalsIgnoreCase("hard")) {
            if (scorePercentage >= 50) {
                levelPassed = true;
                resultMessage = "<font color='#4CAF50'>Congratulations! You have passed all levels!</font>";
                // No proceed button for Hard level since it's the last level
            } else {
                resultMessage = "<font color='#F44336'>Please review the lesson and try again.</font>";
            }
            easyPassed = true;
            normalPassed = true;
        } else {
            resultMessage = "Quiz completed!";
        }

        // Get UI elements
        Button retakeButton = findViewById(R.id.retakeButton);
        TextView resultTextView = findViewById(R.id.resultTextView);

        // Show/hide buttons based on results
        if (levelPassed) {
            if (selectedDifficulty.equalsIgnoreCase("easy")) {
                playSound(true);
                Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();
            } else if (selectedDifficulty.equalsIgnoreCase("normal")) {
                easyPassed = true;
                normalPassed = true;
                playSound(true);
                Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();

            }
            else if (selectedDifficulty.equalsIgnoreCase("hard")) {
                easyPassed = true;
                normalPassed = true;
                playSound(true);
                Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();
            }
        } else {
            if (selectedDifficulty.equalsIgnoreCase("easy")) {
                playSound(false); // Play fail sound
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            } else if (selectedDifficulty.equalsIgnoreCase("normal")) {
                playSound(false);
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            }

            else if (selectedDifficulty.equalsIgnoreCase("hard")) {
                playSound(false);
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            }
        }

        // Hide microphone button for higher difficulty levels
        if (selectedDifficulty.equalsIgnoreCase("normal")) {
            micButton.setVisibility(View.GONE);
        }

        if (selectedDifficulty.equalsIgnoreCase("hard")) {
            micButton.setVisibility(View.GONE);
        }

        // Save score
        saveScore(score);

        // Get the ScrollView that contains the resultTextView
        ScrollView scrollView = (ScrollView) findViewById(R.id.resultView);

        // Create a LinearLayout to hold all the results
        LinearLayout resultContainer = new LinearLayout(this);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        resultContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Replace the content of the ScrollView
        scrollView.removeAllViews();
        scrollView.addView(resultContainer);

        // Add score text at the top
        TextView scoreTextView = new TextView(this);
        scoreTextView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        String scoreText = "<br>Your Score: " + score + "/" + totalQuestions + " - " + resultMessage + "<br><br>";
        scoreText += "--------------------------------------------------------------------------<br>";

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            scoreTextView.setText(android.text.Html.fromHtml(scoreText, android.text.Html.FROM_HTML_MODE_COMPACT));
        } else {
            scoreTextView.setText(android.text.Html.fromHtml(scoreText));
        }

        resultContainer.addView(scoreTextView);

        // For each question, create a separate layout with the exact format requested
        for (int i = 0; i < questionsList.size(); i++) {
            Question question = questionsList.get(i);
            String userAnswer = i < userAnswers.size() ? userAnswers.get(i) : "";
            String correctAnswer = question.getCorrectAnswer();
            boolean isCorrect = userAnswer.equalsIgnoreCase(correctAnswer);

            // Create a LinearLayout for this question
            LinearLayout questionLayout = new LinearLayout(this);
            questionLayout.setOrientation(LinearLayout.VERTICAL);
            questionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            // 1. Question text
            TextView questionTextView = new TextView(this);
            questionTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            String questionText = "<p></b><br>" + question.getQuestion() + "</p>";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                questionTextView.setText(android.text.Html.fromHtml(questionText, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                questionTextView.setText(android.text.Html.fromHtml(questionText));
            }

            questionLayout.addView(questionTextView);

            // 2. Options
            if ("Multiple Choice".equals(question.getQuestionType())) {
                for (String option : question.getChoices()) {
                    TextView optionTextView = new TextView(this);
                    optionTextView.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));

                    String optionText = "<p>";
                    if (option.equalsIgnoreCase(userAnswer)) {
                        // User selected this option
                        if (option.equalsIgnoreCase(correctAnswer)) {
                            // Correct answer - green circle
                            optionText += "&#128994; "; // Green circle emoji 🟢
                        } else {
                            // Wrong answer - red circle
                            optionText += "&#128308; "; // Red circle emoji 🔴
                        }
                        optionText += option + "";

                        // Add color based on correctness
                        if (option.equalsIgnoreCase(correctAnswer)) {
                            optionText = "<font color='#4CAF50'>" + optionText + "</font>";
                        } else {
                            optionText = "<font color='#F44336'>" + optionText + "</font>";
                        }
                    } else {
                        // Unselected option - empty circle
                        optionText += "&#9675; "; // Empty circle ○
                        optionText += option;
                    }
                    optionText += "</p>";

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        optionTextView.setText(android.text.Html.fromHtml(optionText, android.text.Html.FROM_HTML_MODE_COMPACT));
                    } else {
                        optionTextView.setText(android.text.Html.fromHtml(optionText));
                    }

                    questionLayout.addView(optionTextView);
                }
            } else {
                // For fixed answer questions
                TextView answerTextView = new TextView(this);
                answerTextView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                String answerText = "<p>Your Answer - ";
                if (isCorrect) {
                    answerText += "<font color='#4CAF50'>" + userAnswer + "</font>";
                } else {
                    answerText += "<font color='#F44336'>" + userAnswer + "</font>";
                }
                answerText += "</p>";

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    answerTextView.setText(android.text.Html.fromHtml(answerText, android.text.Html.FROM_HTML_MODE_COMPACT));
                } else {
                    answerTextView.setText(android.text.Html.fromHtml(answerText));
                }

                questionLayout.addView(answerTextView);
            }

            // 3. Correct answer
            TextView correctAnswerTextView = new TextView(this);
            correctAnswerTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            String correctAnswerText;
            if (isCorrect) {
                // User answered correctly - show in green
                correctAnswerText = "<p><font color='#4CAF50'><b>CORRECT ANSWER: </b></font>" +
                        "<font color='#4CAF50'>" + correctAnswer + "</font></p>";
            } else {
                // User answered incorrectly - show in red
                correctAnswerText = "<p><font color='#4CAF50'><b>CORRECT ANSWER: </b></font>" +
                        "<font color='#F44336'>" + correctAnswer + "</font></p>";
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                correctAnswerTextView.setText(android.text.Html.fromHtml(correctAnswerText, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                correctAnswerTextView.setText(android.text.Html.fromHtml(correctAnswerText));
            }

            questionLayout.addView(correctAnswerTextView);

            if ((question.getImgUrl() != null && !question.getImgUrl().isEmpty()) ||
                    (question.getImgName() != null && !question.getImgName().isEmpty())) {
                // Add a label for the image
                TextView imageLabel = new TextView(this);
                imageLabel.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                imageLabel.setText("Question Image:");
                imageLabel.setTextColor(getResources().getColor(R.color.secondary_text_color));
                imageLabel.setTextSize(14);
                imageLabel.setPadding(0, 8, 0, 8);

                questionLayout.addView(imageLabel);

                // Create ImageView
                // When creating ImageView in showScore method
                ImageView imageView = new ImageView(this);
                imageView.setId(View.generateViewId());
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        200, // Width 200dp
                        200  // Height 200dp
                );
                params.gravity = Gravity.CENTER;
                imageView.setLayoutParams(params);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_XY);

                questionLayout.addView(imageView);

                // Load the image - pass the question object directly
                loadQuestionImageForScore(question, imageView);
            }


            // 5. Separator
            TextView separatorTextView = new TextView(this);
            separatorTextView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            String separatorText = "<br>_____________________________________________<br>";

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                separatorTextView.setText(android.text.Html.fromHtml(separatorText, android.text.Html.FROM_HTML_MODE_COMPACT));
            } else {
                separatorTextView.setText(android.text.Html.fromHtml(separatorText));
            }

            questionLayout.addView(separatorTextView);

            // Add this question's layout to the main container
            resultContainer.addView(questionLayout);
        }

        // Show the result view
        findViewById(R.id.resultView).setVisibility(View.VISIBLE);

        // Hide question UI elements
        choicesRadioGroup.setVisibility(View.GONE);
        fixedAnswerEditText.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        timerTextView.setVisibility(View.GONE);
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        questionTextView.setVisibility(View.GONE);
        questionImageView.setVisibility(View.GONE);

        retakeButton.setVisibility(levelPassed ? View.GONE : View.VISIBLE);

        dbHelper.insertQuizResult(score, questionsList.size(), selectedDifficulty, selectedDifficulty, "Chinese");
    }

    private void loadQuestionImageForScore(Question question, final ImageView imageView) {
        // First try to load from drawable resources if imgName is available
        String imgName = question.getImgName();
        if (!TextUtils.isEmpty(imgName)) {
            int resourceId = getResources().getIdentifier(
                    imgName, "drawable", getPackageName());

            if (resourceId != 0) {
                // Resource exists in drawable, load it
                imageView.setImageResource(resourceId);
                imageView.setVisibility(View.VISIBLE);
                Log.d("ChineseActivity", "Score screen: Successfully loaded drawable: " + imgName);
                return;
            } else {
                // Try to load from assets
                try {
                    InputStream is = getAssets().open("images/" + imgName);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                    is.close();
                    Log.d("ChineseActivity", "Score screen: Successfully loaded from assets: " + imgName);
                    return;
                } catch (IOException e) {
                    Log.e("ChineseActivity", "Score screen: Error loading from assets: " + e.getMessage());
                    // Fall through to URL loading
                }
            }
        }

        // If we get here, either there was no imgName or it failed to load
        // Try loading from URL as fallback
        String imageUrl = question.getImgUrl();
        if (TextUtils.isEmpty(imageUrl)) {
            imageView.setVisibility(View.GONE);
            return;
        }

        // Use an AsyncTask to download the image
        new android.os.AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String imageUrl = params[0];
                Bitmap bitmap = null;
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    Log.e("ChineseActivity", "Error loading image: " + e.getMessage());
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    imageView.setImageBitmap(result);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    imageView.setVisibility(View.GONE);
                }
            }
        }.execute(imageUrl);
    }


    // Helper method to load images for question results
    private void loadQuestionImage(String imageUrl, final ImageView imageView) {
        Question currentQuestion = null;

        // Find the question that matches this imageUrl
        for (Question q : questionsList) {
            if (q.getImgUrl() != null && q.getImgUrl().equals(imageUrl)) {
                currentQuestion = q;
                break;
            }
        }

        if (currentQuestion != null && !TextUtils.isEmpty(currentQuestion.getImgName())) {
            // First try to load from drawable resources
            String imgName = currentQuestion.getImgName();
            int resourceId = getResources().getIdentifier(
                    imgName, "drawable", getPackageName());

            if (resourceId != 0) {
                // Resource exists in drawable, load it
                imageView.setImageResource(resourceId);
                imageView.setVisibility(View.VISIBLE);
                Log.d("ChineseActivity", "Score screen: Successfully loaded drawable: " + imgName);
                return;
            } else {
                // Try to load from assets
                try {
                    InputStream is = getAssets().open("images/" + imgName);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    imageView.setImageBitmap(bitmap);
                    imageView.setVisibility(View.VISIBLE);
                    is.close();
                    Log.d("ChineseActivity", "Score screen: Successfully loaded from assets: " + imgName);
                    return;
                } catch (IOException e) {
                    Log.e("ChineseActivity", "Score screen: Error loading from assets: " + e.getMessage());
                    // Fall through to URL loading
                }
            }
        }

        // If we get here, either there was no imgName or it failed to load
        // Try loading from URL as fallback
        if (imageUrl == null || imageUrl.isEmpty()) {
            imageView.setVisibility(View.GONE);
            return;
        }

        // Use an AsyncTask to download the image
        new android.os.AsyncTask<String, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(String... params) {
                String imageUrl = params[0];
                Bitmap bitmap = null;
                try {
                    java.net.URL url = new java.net.URL(imageUrl);
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    java.io.InputStream input = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(input);
                } catch (Exception e) {
                    Log.e("ChineseActivity", "Error loading image: " + e.getMessage());
                }
                return bitmap;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                if (result != null) {
                    imageView.setImageBitmap(result);
                    imageView.setVisibility(View.VISIBLE);
                } else {
                    imageView.setVisibility(View.GONE);
                }
            }
        }.execute(imageUrl);
    }

    private void playSound(boolean passed) {
        MediaPlayer mediaPlayer = MediaPlayer.create(
                this,
                passed ? R.raw.passed : R.raw.fail
        );
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.release();
        });
        mediaPlayer.start();
    }

    // Modify your finishQuiz method to use showScore instead
    private void finishQuiz() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        // Hide progress bar when quiz is finished
        progressBar.setVisibility(View.GONE);

        // Show the score screen
        showScore();

        // Set up retake button
        retakeButton.setOnClickListener(v -> {
            findViewById(R.id.resultView).setVisibility(View.GONE);
            findViewById(R.id.proceedButton).setVisibility(View.GONE); // Hide proceed button on retake
            startQuizButton.setVisibility(View.GONE);
            startQuizButton.setEnabled(false);

            retakeButton.setVisibility(View.GONE);

            nextButton.setVisibility(View.VISIBLE);
            timerTextView.setVisibility(View.VISIBLE);
            questionTextView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            quizStarted = true;

            resetQuiz();
            loadQuestions();

            quizContentLayout.setVisibility(View.VISIBLE);
            difficultyGroup.setVisibility(View.GONE);
        });
    }

    // Question class
    // Question class
    private static class Question {
        private String question;
        private List<String> choices;
        private String correctAnswer;
        private String difficulty;
        private String language;
        private String questionType;
        private String url;
        private String imgUrl;
        private String imgName;

        public Question(String question, List<String> choices, String correctAnswer, String difficulty, String language, String questionType, String url, String imgUrl, String imgName) {
            this.question = question;
            this.choices = choices;
            this.correctAnswer = correctAnswer;
            this.difficulty = difficulty;
            this.language = language;
            this.questionType = questionType;
            this.url = url;
            this.imgUrl = imgUrl;
            this.imgName = imgName;
        }

        public String getQuestion() {
            return question;
        }

        public List<String> getChoices() {
            return choices;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public String getLanguage() {
            return language;
        }

        public String getQuestionType() {
            return questionType;
        }

        public String getUrl() {
            return url;
        }

        public String getImgUrl() {
            return imgUrl;
        }

        public String getImgName() {
            return imgName;
        }
    }
}

