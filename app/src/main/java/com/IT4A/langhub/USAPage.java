package com.IT4A.langhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.speech.tts.TextToSpeech;


public class USAPage extends AppCompatActivity {

    private TextView questionText;
    private TextView timerTextView;
    private RadioGroup answerGroup;
    private EditText answerInput;
    private CardView quizcard;
    private Button submitButton, startQuizButton, retakeButton, spaceButton, micButton;
    private ProgressBar progressBar;
    private ImageView questionImage;
    private int currentQuestionIndex = 0;
    private int score = 0;
    private CountDownTimer countDownTimer;
    private String selectedLevel = "easy";
    private long timeLeftInMillis = 10000;
    private List<Question> questions;
    private List<String> userAnswers = new ArrayList<>();
    private QuizDatabaseHelper dbHelper;


    private static final String KEY_ENGLISH_QUIZ_SCORE = "EnglishQuizScore";
    private TextToSpeech textToSpeech;
    private Button speakerButton;


    private static final String PREFS_NAME = "QuizScores";
    private static final String ENGLISH_EASY_PASSED = "englishEasyPassed";
    private static final String ENGLISH_MEDIATE_PASSED = "englishMediatePassed";

    private boolean easyPassed = false;
    private boolean mediatePassed = false;
    private Button proceedButton;


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SpeechRecognizer speechRecognizer;
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usa);

        dbHelper = new QuizDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("English Quiz");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        questionText = findViewById(R.id.question_text);
        quizcard = findViewById(R.id.quiz_card);
        timerTextView = findViewById(R.id.timer_text);
        answerGroup = findViewById(R.id.answer_group);
        answerInput = findViewById(R.id.answer_input);
        submitButton = findViewById(R.id.submit_button);
        startQuizButton = findViewById(R.id.start_quiz_button);
        retakeButton = findViewById(R.id.retake_button);
        spaceButton = findViewById(R.id.space_button);
        progressBar = findViewById(R.id.progress_bar);
        questionImage = findViewById(R.id.question_image);
        micButton = findViewById(R.id.mic_button);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        easyPassed = prefs.getBoolean(ENGLISH_EASY_PASSED, false);
        mediatePassed = prefs.getBoolean(ENGLISH_MEDIATE_PASSED, false);

        RadioButton easyRadio = findViewById(R.id.easy_radio);
        RadioButton mediateRadio = findViewById(R.id.mediate_radio);
        RadioButton hardRadio = findViewById(R.id.hard_radio);

        easyRadio.setEnabled(true);


        mediateRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!easyPassed) {
                    // Show popup message
                    Toast.makeText(USAPage.this, "You need to pass the easy level to unlock this", Toast.LENGTH_SHORT).show();
                    // Uncheck the mediate radio button
                    mediateRadio.setChecked(false);
                    // Ensure easy radio button remains checked
                    easyRadio.setChecked(true);
                }
            }
        });

        // Similarly, add a click listener for the hard radio button
        hardRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mediatePassed) {
                    Toast.makeText(USAPage.this, "You need to pass the mediate level to unlock this", Toast.LENGTH_SHORT).show();
                    hardRadio.setChecked(false);
                    if (easyPassed) {
                        mediateRadio.setChecked(true);
                    } else {
                        easyRadio.setChecked(true);
                    }
                }
            }
        });


        proceedButton = findViewById(R.id.proceed_button);
        proceedButton.setVisibility(View.GONE);
        proceedButton.setOnClickListener(v -> proceedToNextLevel());


        speakerButton = findViewById(R.id.speaker_button);
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.ENGLISH);
                float speechRate = 0.8f; // Speech rate
                textToSpeech.setSpeechRate(speechRate);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "English language is not supported", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });

        speakerButton.setOnClickListener(v -> speakCorrectAnswer());

        RadioGroup difficultyGroup = findViewById(R.id.difficulty_group);
        startQuizButton.setOnClickListener(v -> {
            int selectedDifficultyId = difficultyGroup.getCheckedRadioButtonId();
            if (selectedDifficultyId == -1) {
                Toast.makeText(this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedDifficultyButton = findViewById(selectedDifficultyId);
            selectedLevel = selectedDifficultyButton.getText().toString().toLowerCase();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("selectedLevel", selectedLevel);
            editor.apply();

            generateQuestions(selectedLevel);
            startQuizButton.setVisibility(View.GONE);
            quizcard.setVisibility(View.VISIBLE);
            difficultyGroup.setVisibility(View.GONE);
            submitButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.VISIBLE);
            loadQuestion();
        });

        submitButton.setOnClickListener(v -> {
            checkAnswer();
            currentQuestionIndex++;
            if (currentQuestionIndex < questions.size()) {
                loadQuestion();
            } else {
                showScore();
            }
        });

        retakeButton.setOnClickListener(v -> {
            TextView text = findViewById(R.id.text);
            text.setVisibility(View.GONE);
            questionImage.setVisibility(View.GONE);
            proceedButton.setVisibility(View.GONE);
            resetQuiz();
            micButton.setVisibility(View.GONE);
        });

        difficultyGroup.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedDifficultyButton = findViewById(checkedId);
            String selectedDifficulty = selectedDifficultyButton.getText().toString();
            TextView difficultyTextView = findViewById(R.id.difficulty);
            difficultyTextView.setText("Choose level of Difficulty: " + selectedDifficulty);
        });
    }

    private void resetQuiz() {
        currentQuestionIndex = 0;
        score = 0;
        userAnswers.clear();
        submitButton.setVisibility(View.VISIBLE);
        retakeButton.setVisibility(View.GONE);
        answerGroup.setVisibility(View.VISIBLE);
        answerInput.setVisibility(View.GONE);
        spaceButton.setVisibility(View.GONE);
        findViewById(R.id.answer_1).setVisibility(View.VISIBLE);
        findViewById(R.id.answer_2).setVisibility(View.VISIBLE);
        findViewById(R.id.answer_3).setVisibility(View.VISIBLE);
        questionImage.setVisibility(View.GONE);
        questionText.setText("");
        timerTextView.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);
        progressBar.setVisibility(View.VISIBLE);

        questions.clear();
        generateQuestions(selectedLevel);
        loadQuestion();

        if (selectedLevel.equalsIgnoreCase("mediate")) {
            micButton.setVisibility(View.GONE);
        } else {
            micButton.setVisibility(View.VISIBLE);
        }
        if (selectedLevel.equalsIgnoreCase("hard")) {
            micButton.setVisibility(View.GONE);
        } else {
            micButton.setVisibility(View.VISIBLE);
        }


    }

    private void startTimer() {
        switch (selectedLevel.toLowerCase()) {
            case "easy":
                timeLeftInMillis = 20000;
                break;
            case "mediate":
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
                checkAnswer();
                currentQuestionIndex++;
                if (currentQuestionIndex < questions.size()) {
                    loadQuestion();
                } else {
                    showScore();
                }
            }
        }.start();
    }

    private void updateTimer() {
        int seconds = (int) (timeLeftInMillis / 1000);
        String timeFormatted = String.format(Locale.getDefault(), "%02d", seconds);
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
                Toast.makeText(USAPage.this, errorMessage, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String spokenText = matches.get(0);
                    answerInput.setText(spokenText);
                    answerInput.setVisibility(View.VISIBLE); // Make sure the input is visible
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now use the microphone
            } else {
                // Permission denied, handle accordingly (e.g., show a message to the user)
            }
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer in English");
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error starting speech recognition", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void generateQuestions(String level) {
        List<Question> allQuestions = new ArrayList<>();
        switch (level.toLowerCase()) {
            case "easy":
                allQuestions.add(new Question(R.drawable.plate, "¿Qué es esto en inglés?", "Plate", "Fork", "Glass", "Plate"));
                allQuestions.add(new Question(R.drawable.fork, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Fork", "Plate", "Glass", "Fork"));
                allQuestions.add(new Question(R.drawable.glass, "¿Cómo se dice esto en inglés?", "Glass", "Fork", "Plate", "Glass"));
                allQuestions.add(new Question(R.drawable.cup, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Cup", "Fork", "Plate", "Cup"));
                allQuestions.add(new Question(R.drawable.spoon, "¿Qué es esto en inglés?", "Spoon", "Plate", "Fork", "Spoon"));
                allQuestions.add(new Question(R.drawable.bottle, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Bottle", "Cup", "Plate", "Bottle"));
                allQuestions.add(new Question(R.drawable.book, "¿Qué es esto en inglés?", "Book", "Table", "Chair", "Book"));
                allQuestions.add(new Question(R.drawable.chair, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Chair", "Book", "Table", "Chair"));
                allQuestions.add(new Question(R.drawable.table, "¿Qué es esto en inglés?", "Table", "Chair", "Book", "Table"));
                allQuestions.add(new Question(R.drawable.phone, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Phone", "Mobile", "Book", "Mobile"));
                allQuestions.add(new Question(R.drawable.laptop, "¿Qué es esto en inglés?", "Laptop", "Table", "Phone", "Laptop"));
                allQuestions.add(new Question(R.drawable.desk, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Desk", "Chair", "Table", "Desk"));
                allQuestions.add(new Question(R.drawable.shirt, "¿Qué es esto en inglés?", "Shirt", "Pants", "Shoes", "Shirt"));
                allQuestions.add(new Question(R.drawable.pants, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Pants", "Shirt", "Shoes", "Pants"));
                allQuestions.add(new Question(R.drawable.shoes, "¿Qué es esto en inglés?", "Shoes", "Pants", "Hat", "Shoes"));
                allQuestions.add(new Question(R.drawable.jacket, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Jacket", "Pants", "Shirt", "Jacket"));
                allQuestions.add(new Question(R.drawable.glove, "¿Qué es esto en inglés?", "Glove", "Shoes", "Hat", "Glove"));
                allQuestions.add(new Question(R.drawable.scarf, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Scarf", "Hat", "Glove", "Scarf"));
                allQuestions.add(new Question(R.drawable.shot, "¿Qué es esto en inglés?", "Shorts", "Pants", "Shirt", "Shorts"));
                allQuestions.add(new Question(R.drawable.glasses, "這句話用英文怎麼說？ (Zhè jù huà yòng yīngwén zěnme shuō?)", "Glasses", "Hat", "Bag", "Glasses"));

                allQuestions.add(new Question(0, "¿Cómo dirías '¿Cómo te llamas?' en inglés?", "What is your name?", "Where are you from?", "How old are you?", "What is your name?"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿De dónde eres?' en inglés?", "Where are you from?", "Where do you live?", "How are you?", "Where are you from?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Tengo un hermano' en inglés?", "I have a brother", "I have a sister", "I have a cousin", "I have a brother"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Qué tal?' en inglés?", "How's it going?", "How are you?", "What time is it?", "How's it going?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Tengo miedo' en inglés?", "I am scared", "I am tired", "I am happy", "I am scared"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Puedes ayudarme?' en inglés?", "Can you help me?", "Can I help you?", "I need help", "Can you help me?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Estoy enfermo' en inglés?", "I am sick", "I am tired", "I am happy", "I am sick"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Dónde está mi teléfono?' en inglés?", "Where is my phone?", "Where is my book?", "Where is your phone?", "Where is my phone?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Este es mi amigo' en inglés?", "This is my friend", "This is my brother", "This is my cousin", "This is my friend"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Voy a la escuela' en inglés?", "I am going to school", "I am going to work", "I am going to the store", "I am going to school"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Tengo que estudiar' en inglés?", "I have to study", "I need to study", "I must study", "I have to study"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Hace calor' en inglés?", "It is hot", "It is cold", "It is warm", "It is hot"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Qué te gusta hacer?' en inglés?", "What do you like to do?", "What do you like?", "What are you doing?", "What do you like to do?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'No entiendo' en inglés?", "I don't understand", "I don't know", "I am confused", "I don't understand"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Me siento bien' en inglés?", "I feel good", "I feel fine", "I feel great", "I feel good"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Qué hiciste ayer?' en inglés?", "What did you do yesterday?", "What are you doing today?", "What will you do tomorrow?", "What did you do yesterday?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Estoy aprendiendo inglés' en inglés?", "I am learning English", "I am studying English", "I learn English", "I am learning English"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Qué quieres comer?' en inglés?", "What do you want to eat?", "What are you eating?", "What do you eat?", "What do you want to eat?"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Dónde está la estación de tren?' en inglés?", "Where is the train station?", "Where is the bus station?", "Where is the airport?", "Where is the train station?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'Hace frío' en inglés?", "It is cold", "It is hot", "It is chilly", "It is cold"));

                allQuestions.add(new Question(0, "what is 你叫什么名字？(Nǐ jiào shénme míngzì?) in english", "What is your name?", "Where are you from?", "How old are you?", "What is your name?"));
                allQuestions.add(new Question(0, "what is 你好吗？(Nǐ hǎo ma?) in english", "How are you?", "What is your name?", "How old are you?", "How are you?"));
                allQuestions.add(new Question(0, "what is 你从哪里来？(Nǐ cóng nǎlǐ lái?) in english", "Where are you from?", "What is your name?", "How old are you?", "Where are you from?"));
                allQuestions.add(new Question(0, "what is 今天星期几？(Jīntiān xīngqī jǐ?) in english", "What day is it today?", "How old are you?", "What time is it?", "What day is it today?"));
                allQuestions.add(new Question(0, "what is 你喜欢什么颜色？(Nǐ xǐhuān shénme yánsè?) in english", "What is your favorite color?", "How old are you?", "What is your name?", "What is your favorite color?"));
                allQuestions.add(new Question(0, "what is 你会说中文吗？(Nǐ huì shuō zhōngwén ma?) in english", "Can you speak Chinese?", "How old are you?", "Where are you?", "Can you speak Chinese?"));
                allQuestions.add(new Question(0, "what is 你想吃什么？(Nǐ xiǎng chī shénme?) in english", "What do you want to eat?", "What time is it?", "How old are you?", "What do you want to eat?"));
                allQuestions.add(new Question(0, "what is 你住在哪里？(Nǐ zhù zài nǎlǐ?) in english", "Where do you live?", "What time is it?", "How old are you?", "Where do you live?"));
                allQuestions.add(new Question(0, "what is 你喜欢什么音乐？(Nǐ xǐhuān shénme yīnyuè?) in english", "What music do you like?", "Where are you from?", "What is your name?", "What music do you like?"));
                allQuestions.add(new Question(0, "what is 你工作吗？(Nǐ gōngzuò ma?) in english", "Do you work?", "How old are you?", "What is your name?", "Do you work?"));
                allQuestions.add(new Question(0, "what is 你有兄弟姐妹吗？(Nǐ yǒu xiōngdì jiěmèi ma?) in english", "Do you have siblings?", "How old are you?", "Where are you?", "Do you have siblings?"));
                allQuestions.add(new Question(0, "what is 你喜欢哪种运动？(Nǐ xǐhuān nǎ zhǒng yùndòng?) in english", "What sport do you like?", "Where are you from?", "How old are you?", "What sport do you like?"));
                allQuestions.add(new Question(0, "what is 你喝咖啡吗？(Nǐ hē kāfēi ma?) in english", "Do you drink coffee?", "What time is it?", "How old are you?", "Do you drink coffee?"));
                allQuestions.add(new Question(0, "what is 今天几号？(Jīntiān jǐ hào?) in english", "What is today's date?", "Where are you from?", "What time is it?", "What is today's date?"));
                allQuestions.add(new Question(0, "what is 你喜欢去哪儿旅行？(Nǐ xǐhuān qù nǎr lǚxíng?) in english", "Where do you like to travel?", "What is your name?", "How old are you?", "Where do you like to travel?"));
                allQuestions.add(new Question(0, "what is 你会做饭吗？(Nǐ huì zuò fàn ma?) in english", "Can you cook?", "How old are you?", "Where do you live?", "Can you cook?"));
                allQuestions.add(new Question(0, "what is 你喜欢什么电影？(Nǐ xǐhuān shénme diànyǐng?) in english", "What movie do you like?", "Where are you from?", "What is your name?", "What movie do you like?"));
                allQuestions.add(new Question(0, "what is 你早上几点起床？(Nǐ zǎoshang jǐ diǎn qǐchuáng?) in english", "What time do you wake up in the morning?", "What time is it?", "Where are you from?", "What time do you wake up in the morning?"));
                allQuestions.add(new Question(0, "what is 你喜欢吃甜点吗？(Nǐ xǐhuān chī tiándiǎn ma?) in english", "Do you like dessert?", "Where do you live?", "How old are you?", "Do you like dessert?"));
                allQuestions.add(new Question(0, "what is 你常去哪里购物？(Nǐ cháng qù nǎlǐ gòuwù?) in english", "Where do you usually shop?", "What time is it?", "How old are you?", "Where do you usually shop?"));
                allQuestions.add(new Question(0, "what is 你喜欢猫还是狗？(Nǐ xǐhuān māo háishì gǒu?) in english", "Do you like cats or dogs?", "Where are you from?", "How old are you?", "Do you like cats or dogs?"));

                allQuestions.add(new Question(0, "¿Cómo dirías '¿Cuántos años tienes?' en inglés?", "How old are you?", "Where are you?", "What time is it?", "How old are you?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'estoy cansado' en inglés?", "I am tired", "I am happy", "I am sad", "I am tired"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'esto es un libro' en inglés?", "This is a book", "This is an apple", "This is a pen", "This is a book"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'tengo hambre' en inglés?", "I am hungry", "I am thirsty", "I am tired", "I am hungry"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'lo siento' en inglés?", "I am sorry", "Goodbye", "Hello", "I am sorry"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'buenos días' en inglés?", "Good morning", "Good afternoon", "Good night", "Good morning"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'gracias' en inglés?", "Thank you", "Sorry", "Please", "Thank you"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Dónde está el baño?' en inglés?", "Where is the bathroom?", "Where are you?", "How are you?", "Where is the bathroom?"));
                allQuestions.add(new Question(0, "¿Cómo dirías '¿Cuántos años tienes?' en inglés?", "How old are you?", "How are you?", "Where are you?", "How old are you?"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'estoy bien' en inglés?", "I am fine", "I am good", "I am happy", "I am fine"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'me gusta' en inglés?", "I like", "I don't like", "I love", "I like"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'necesito ayuda' en inglés?", "I need help", "I want help", "I am asking for help", "I need help"));
                allQuestions.add(new Question(0, "¿Cómo dirías 'gracias por tu ayuda' en inglés?", "Thank you for your help", "Thank you for help", "Thanks you for your help", "Thank you for your help"));
                Collections.shuffle(allQuestions);
                questions = new ArrayList<>(allQuestions.subList(0, 15));
                break;


            case "mediate":

                allQuestions.add(new Question(R.drawable.plate, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Plate"));
                allQuestions.add(new Question(R.drawable.fork, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Fork"));
                allQuestions.add(new Question(R.drawable.glass, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Glass"));
                allQuestions.add(new Question(R.drawable.phone, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Phone"));
                allQuestions.add(new Question(R.drawable.laptop, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Laptop"));
                allQuestions.add(new Question(R.drawable.desk, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Desk"));
                allQuestions.add(new Question(R.drawable.computer, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Computer"));

                allQuestions.add(new Question(R.drawable.toothpaste, "¿Cómo se llama esto en inglés?", null, null, null, "Toothpaste"));
                allQuestions.add(new Question(R.drawable.socks, "¿Cómo se llama esto en inglés?", null, null, null, "Socks"));
                allQuestions.add(new Question(R.drawable.blanket, "¿Cómo se llama esto en inglés?", null, null, null, "Blanket"));
                allQuestions.add(new Question(R.drawable.towel, "¿Cómo se llama esto en inglés?", null, null, null, "Towel"));
                allQuestions.add(new Question(R.drawable.pillow, "¿Cómo se llama esto en inglés?", null, null, null, "Pillow"));
                allQuestions.add(new Question(R.drawable.broom, "¿Cómo se llama esto en inglés?", null, null, null, "Broom"));

                allQuestions.add(new Question(0, "How do you say '¿Podrías ayudarme, por favor?' in English?", null, null, null, "Could you help me, please?"));
                allQuestions.add(new Question(0, "How do you say 'Hace mucho frío hoy' in English?", null, null, null, "It's very cold today"));
                allQuestions.add(new Question(0, "How do you say '¿Qué tal tu día?' in English?", null, null, null, "How was your day?"));
                allQuestions.add(new Question(0, "How do you say 'Te llamo más tarde' in English?", null, null, null, "I'll call you later"));
                allQuestions.add(new Question(0, "How do you say '¿Dónde está la estación de tren?' in English?", null, null, null, "Where is the train station?"));
                allQuestions.add(new Question(0, "How do you say 'Estoy aprendiendo inglés' in English?", null, null, null, "I am learning English"));
                allQuestions.add(new Question(0, "How do you say '¿Cuánto cuesta esto?' in English?", null, null, null, "How much does this cost?"));
                allQuestions.add(new Question(0, "How do you say 'Me encantaría ir a la fiesta' in English?", null, null, null, "I would love to go to the party"));
                allQuestions.add(new Question(0, "How do you say '¿Puedes repetir eso, por favor?' in English?", null, null, null, "Can you repeat that, please?"));
                allQuestions.add(new Question(0, "How do you say '¿A qué hora empieza la película?' in English?", null, null, null, "What time does the movie start?"));
                allQuestions.add(new Question(0, "How do you say 'Necesito un médico' in English?", null, null, null, "I need a doctor"));
                allQuestions.add(new Question(0, "How do you say '¿Cómo se llega al aeropuerto?' in English?", null, null, null, "How do you get to the airport?"));
                allQuestions.add(new Question(0, "How do you say 'Tengo hambre' in English?", null, null, null, "I am hungry"));
                allQuestions.add(new Question(0, "How do you say '¿Puedes darme un ejemplo?' in English?", null, null, null, "Can you give me an example?"));
                allQuestions.add(new Question(0, "How do you say '¿Cuándo es tu cumpleaños?' in English?", null, null, null, "When is your birthday?"));
                allQuestions.add(new Question(0, "How do you say 'El tren está retrasado' in English?", null, null, null, "The train is delayed"));
                allQuestions.add(new Question(0, "How do you say '¿Hay alguna tienda cerca de aquí?' in English?", null, null, null, "Is there a store near here?"));
                allQuestions.add(new Question(0, "How do you say 'Este lugar es muy bonito' in English?", null, null, null, "This place is very beautiful"));
                allQuestions.add(new Question(0, "How do you say '¿Puedo pagar con tarjeta de crédito?' in English?", null, null, null, "Can I pay with a credit card?"));
                allQuestions.add(new Question(0, "How do you say 'Hace buen tiempo hoy' in English?", null, null, null, "The weather is nice today"));


                allQuestions.add(new Question(0, "How do you say '你住在哪裡? (Nǐ zhù zài nǎlǐ?)' in English?", null, null, null, "Where do you live?"));
                allQuestions.add(new Question(0, "How do you say '你會說英語嗎? (Nǐ huì shuō yīngyǔ ma?)' in English?", null, null, null, "Do you speak English?"));
                allQuestions.add(new Question(0, "How do you say '今天是幾號? (Jīntiān shì jǐ hào?)' in English?", null, null, null, "What’s the date today?"));
                allQuestions.add(new Question(0, "How do you say '你從哪裡來? (Nǐ cóng nǎlǐ lái?)' in English?", null, null, null, "Where are you from?"));
                allQuestions.add(new Question(0, "How do you say '我想喝水 (Wǒ xiǎng hē shuǐ)' in English?", null, null, null, "I want to drink water"));
                allQuestions.add(new Question(0, "How do you say '你喜歡這裡嗎? (Nǐ xǐhuān zhèlǐ ma?)' in English?", null, null, null, "Do you like it here?"));
                allQuestions.add(new Question(0, "How do you say '我在學習中文 (Wǒ zài xuéxí zhōngwén)' in English?", null, null, null, "I am learning Chinese"));
                allQuestions.add(new Question(0, "How do you say '你要去哪裡? (Nǐ yào qù nǎlǐ?)' in English?", null, null, null, "Where are you going?"));
                allQuestions.add(new Question(0, "How do you say '今天很熱 (Jīntiān hěn rè)' in English?", null, null, null, "It’s hot today"));
                allQuestions.add(new Question(0, "How do you say '我不懂 (Wǒ bù dǒng)' in English?", null, null, null, "I don’t understand"));
                allQuestions.add(new Question(0, "How do you say '你吃過晚餐了嗎? (Nǐ chī guò wǎncān le ma?)' in English?", null, null, null, "Have you had dinner?"));
                allQuestions.add(new Question(0, "How do you say '這是我的朋友 (Zhè shì wǒ de péngyǒu)' in English?", null, null, null, "This is my friend"));
                allQuestions.add(new Question(0, "How do you say '我明天有空 (Wǒ míngtiān yǒu kòng)' in English?", null, null, null, "I’m free tomorrow"));
                allQuestions.add(new Question(0, "How do you say '你喜歡哪個顏色? (Nǐ xǐhuān nǎ ge yánsè?)' in English?", null, null, null, "What’s your favorite color?"));
                allQuestions.add(new Question(0, "How do you say '我很高興認識你 (Wǒ hěn gāoxìng rènshí nǐ)' in English?", null, null, null, "Nice to meet you"));
                allQuestions.add(new Question(0, "How do you say '那是我的家 (Nà shì wǒ de jiā)' in English?", null, null, null, "That’s my house"));
                allQuestions.add(new Question(0, "How do you say '今天我們去看電影 (Jīntiān wǒmen qù kàn diànyǐng)' in English?", null, null, null, "We are going to the movies today"));
                allQuestions.add(new Question(0, "How do you say '請問廁所在哪裡? (Qǐngwèn cè suǒ zài nǎlǐ?)' in English?", null, null, null, "Where is the bathroom?"));
                allQuestions.add(new Question(0, "How do you say '你要喝什麼? (Nǐ yào hē shénme?)' in English?", null, null, null, "What would you like to drink?"));
                allQuestions.add(new Question(0, "How do you say '明天見 (Míngtiān jiàn)' in English?", null, null, null, "See you tomorrow"));

                allQuestions.add(new Question(0, "How do you say '你住在哪裡? (Nǐ zhù zài nǎlǐ?)' in English?", null, null, null, "Where do you live?"));
                allQuestions.add(new Question(0, "How do you say '你會說英語嗎? (Nǐ huì shuō yīngyǔ ma?)' in English?", null, null, null, "Do you speak English?"));
                allQuestions.add(new Question(0, "How do you say '今天是幾號? (Jīntiān shì jǐ hào?)' in English?", null, null, null, "What’s the date today?"));
                allQuestions.add(new Question(0, "How do you say '你從哪裡來? (Nǐ cóng nǎlǐ lái?)' in English?", null, null, null, "Where are you from?"));
                allQuestions.add(new Question(0, "How do you say '我想喝水 (Wǒ xiǎng hē shuǐ)' in English?", null, null, null, "I want to drink water"));
                allQuestions.add(new Question(0, "How do you say '你喜歡這裡嗎? (Nǐ xǐhuān zhèlǐ ma?)' in English?", null, null, null, "Do you like it here?"));
                allQuestions.add(new Question(0, "How do you say '我在學習中文 (Wǒ zài xuéxí zhōngwén)' in English?", null, null, null, "I am learning Chinese"));
                allQuestions.add(new Question(0, "How do you say '你要去哪裡? (Nǐ yào qù nǎlǐ?)' in English?", null, null, null, "Where are you going?"));
                allQuestions.add(new Question(0, "How do you say '今天很熱 (Jīntiān hěn rè)' in English?", null, null, null, "It’s hot today"));
                allQuestions.add(new Question(0, "How do you say '我不懂 (Wǒ bù dǒng)' in English?", null, null, null, "I don’t understand"));
                allQuestions.add(new Question(0, "How do you say '你吃過晚餐了嗎? (Nǐ chī guò wǎncān le ma?)' in English?", null, null, null, "Have you had dinner?"));
                allQuestions.add(new Question(0, "How do you say '這是我的朋友 (Zhè shì wǒ de péngyǒu)' in English?", null, null, null, "This is my friend"));
                allQuestions.add(new Question(0, "How do you say '我明天有空 (Wǒ míngtiān yǒu kòng)' in English?", null, null, null, "I’m free tomorrow"));
                allQuestions.add(new Question(0, "How do you say '你喜歡哪個顏色? (Nǐ xǐhuān nǎ ge yánsè?)' in English?", null, null, null, "What’s your favorite color?"));
                allQuestions.add(new Question(0, "How do you say '我很高興認識你 (Wǒ hěn gāoxìng rènshí nǐ)' in English?", null, null, null, "Nice to meet you"));
                allQuestions.add(new Question(0, "How do you say '那是我的家 (Nà shì wǒ de jiā)' in English?", null, null, null, "That’s my house"));
                allQuestions.add(new Question(0, "How do you say '今天我們去看電影 (Jīntiān wǒmen qù kàn diànyǐng)' in English?", null, null, null, "We are going to the movies today"));
                allQuestions.add(new Question(0, "How do you say '請問廁所在哪裡? (Qǐngwèn cè suǒ zài nǎlǐ?)' in English?", null, null, null, "Where is the bathroom?"));
                allQuestions.add(new Question(0, "How do you say '你要喝什麼? (Nǐ yào hē shénme?)' in English?", null, null, null, "What would you like to drink?"));
                allQuestions.add(new Question(0, "How do you say '明天見 (Míngtiān jiàn)' in English?", null, null, null, "See you tomorrow"));


                allQuestions.add(new Question(0, "Traducir 'Me gustaría un café, por favor' al inglés:", null, null, null, "I would like a coffee, please"));
                allQuestions.add(new Question(0, "¿Cómo se dice 'Mañana' en español?", null, null, null, "Tomorrow"));
                allQuestions.add(new Question(0, "¿Cómo se traduce'Buenas noches'al español?", null, null, null, "Good night"));
                allQuestions.add(new Question(0, "¿Cómo se traduce '¿Cómo estás? ' al español?", null, null, null, "How are you?"));
                allQuestions.add(new Question(0, "¿Cuál es la palabra en inglés para 'familia'?", null, null, null, "Family"));
                allQuestions.add(new Question(0, "‘我不明白’用英語怎麼說？ (Wǒ bù míngbái yòng yīngyǔ zěnme shuō?)", null, null, null, "I don’t understand"));
                allQuestions.add(new Question(0, "‘這個多少錢？’的英文翻譯是什麼？ (Zhège duōshǎo qián? De yīngwén fānyì shì shénme?)", null, null, null, "How much does it cost?"));
                allQuestions.add(new Question(0, "‘書’的英文單字是什麼？ (Shū de yīngwén dānzì shì shénme?)", null, null, null, "Book"));
                allQuestions.add(new Question(0, "‘朋友’的英文單字是什麼？ (Péngyǒu de yīngwén dānzì shì shénme?)", null, null, null, "Friend"));
                allQuestions.add(new Question(0, "‘謝謝’用英語怎麼說？ (Xièxiè yòng yīngyǔ zěnme shuō?)", null, null, null, "Thank you"));
                allQuestions.add(new Question(0, "‘汽車’的英文單字是什麼？ (Qìchē de yīngwén dānzì shì shénme?)", null, null, null, "Car"));
                allQuestions.add(new Question(0, "‘早安’的英文翻譯是什麼？ (Zǎo'ān de yīngwén fānyì shì shénme?)", null, null, null, "Good morning"));
                allQuestions.add(new Question(0, "‘生日快樂’用英語怎麼說？ (Shēngrì kuàilè yòng yīngyǔ zěnme shuō?)", null, null, null, "Happy birthday"));

// Additional 48 questions:
                allQuestions.add(new Question(0, "Traducir '¿Cómo te llamas?' al inglés:", null, null, null, "What is your name?"));
                allQuestions.add(new Question(0, "Traducir 'Tengo hambre' al inglés:", null, null, null, "I am hungry"));
                allQuestions.add(new Question(0, "Traducir '¿Dónde está el baño?' al inglés:", null, null, null, "Where is the bathroom?"));
                allQuestions.add(new Question(0, "Traducir '¿Cuántos años tienes?' al inglés:", null, null, null, "How old are you?"));
                allQuestions.add(new Question(0, "Traducir 'Estoy cansado' al inglés:", null, null, null, "I am tired"));
                allQuestions.add(new Question(0, "Traducir 'Me gusta bailar' al inglés:", null, null, null, "I like to dance"));
                allQuestions.add(new Question(0, "Traducir 'Tengo un perro' al inglés:", null, null, null, "I have a dog"));
                allQuestions.add(new Question(0, "Traducir 'Voy a la tienda' al inglés:", null, null, null, "I am going to the store"));
                allQuestions.add(new Question(0, "Traducir '¿Qué hora es?' al inglés:", null, null, null, "What time is it?"));
                allQuestions.add(new Question(0, "Traducir '¿De dónde eres?' al inglés:", null, null, null, "Where are you from?"));
                allQuestions.add(new Question(0, "Traducir 'Mi casa está lejos' al inglés:", null, null, null, "My house is far"));
                allQuestions.add(new Question(0, "Traducir 'Tengo frío' al inglés:", null, null, null, "I am cold"));
                allQuestions.add(new Question(0, "Traducir 'Necesito ayuda' al inglés:", null, null, null, "I need help"));
                allQuestions.add(new Question(0, "Traducir 'No entiendo' al inglés:", null, null, null, "I don't understand"));
                allQuestions.add(new Question(0, "Traducir 'Estoy feliz' al inglés:", null, null, null, "I am happy"));
                allQuestions.add(new Question(0, "Traducir '¿Dónde vives?' al inglés:", null, null, null, "Where do you live?"));
                allQuestions.add(new Question(0, "Traducir 'Estoy aprendiendo inglés' al inglés:", null, null, null, "I am learning English"));
                allQuestions.add(new Question(0, "Traducir '¿Cuántos años tienes?' al inglés:", null, null, null, "How old are you?"));
                allQuestions.add(new Question(0, "Traducir '¿Qué te gusta hacer?' al inglés:", null, null, null, "What do you like to do?"));
                allQuestions.add(new Question(0, "Traducir '¿Dónde está el restaurante?' al inglés:", null, null, null, "Where is the restaurant?"));
                allQuestions.add(new Question(0, "Traducir '¿Qué día es hoy?' al inglés:", null, null, null, "What day is today?"));
                allQuestions.add(new Question(0, "Traducir 'Hace calor' al inglés:", null, null, null, "It is hot"));
                allQuestions.add(new Question(0, "Traducir 'Tengo sed' al inglés:", null, null, null, "I am thirsty"));
                allQuestions.add(new Question(0, "Traducir 'Me siento bien' al inglés:", null, null, null, "I feel good"));
                allQuestions.add(new Question(0, "Traducir '¿Cómo estás?' al inglés:", null, null, null, "How are you?"));
                allQuestions.add(new Question(0, "Traducir 'Estoy bien' al inglés:", null, null, null, "I am fine"));
                allQuestions.add(new Question(0, "Traducir '¿Qué haces?' al inglés:", null, null, null, "What are you doing?"));
                allQuestions.add(new Question(0, "Traducir 'Voy al cine' al inglés:", null, null, null, "I am going to the cinema"));
                allQuestions.add(new Question(0, "Traducir 'Soy estudiante' al inglés:", null, null, null, "I am a student"));
                allQuestions.add(new Question(0, "Traducir '¿Qué hora es?' al inglés:", null, null, null, "What time is it?"));
                allQuestions.add(new Question(0, "Traducir 'Esto es un libro' al inglés:", null, null, null, "This is a book"));
                allQuestions.add(new Question(0, "Traducir '¿Cuál es tu color favorito?' al inglés:", null, null, null, "What is your favorite color?"));
                allQuestions.add(new Question(0, "Traducir 'Tengo un poco de sueño' al inglés:", null, null, null, "I am a little sleepy"));
                allQuestions.add(new Question(0, "Traducir 'Me gustaría ir al parque' al inglés:", null, null, null, "I would like to go to the park"));
                allQuestions.add(new Question(0, "Traducir 'No me gusta el frío' al inglés:", null, null, null, "I don't like the cold"));
                allQuestions.add(new Question(0, "Traducir '¿Puedes ayudarme?' al inglés:", null, null, null, "Can you help me?"));
                allQuestions.add(new Question(0, "Traducir 'No sé' al inglés:", null, null, null, "I don't know"));
                allQuestions.add(new Question(0, "Traducir 'Tengo un coche' al inglés:", null, null, null, "I have a car"));
                allQuestions.add(new Question(0, "Traducir 'Me gusta viajar' al inglés:", null, null, null, "I like to travel"));
                allQuestions.add(new Question(0, "Traducir '¿Te gusta la comida italiana?' al inglés:", null, null, null, "Do you like Italian food?"));
                allQuestions.add(new Question(0, "Traducir '¿Puedes hablar más despacio?' al inglés:", null, null, null, "Can you speak slower?"));
                allQuestions.add(new Question(0, "Traducir 'Estoy perdido' al inglés:", null, null, null, "I am lost"));
                allQuestions.add(new Question(0, "Traducir 'Hace frío hoy' al inglés:", null, null, null, "It is cold today"));
                allQuestions.add(new Question(0, "Traducir 'Quiero ir a la playa' al inglés:", null, null, null, "I want to go to the beach"));
                allQuestions.add(new Question(0, "Traducir '¿A qué hora es la reunión?' al inglés:", null, null, null, "What time is the meeting?"));

                Collections.shuffle(allQuestions);
                questions = new ArrayList<>(allQuestions.subList(0, 20));
                break;


            case "hard":

                allQuestions.add(new Question(R.drawable.plate, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Plate"));
                allQuestions.add(new Question(R.drawable.fork, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Fork"));
                allQuestions.add(new Question(R.drawable.glass, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Glass"));
                allQuestions.add(new Question(R.drawable.phone, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Phone"));
                allQuestions.add(new Question(R.drawable.laptop, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Laptop"));
                allQuestions.add(new Question(R.drawable.desk, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Desk"));
                allQuestions.add(new Question(R.drawable.computer, "這個用英語怎麼說？ (Zhège yòng yīngyǔ zěnme shuō?)", null, null, null, "Computer"));

                allQuestions.add(new Question(R.drawable.toothpaste, "¿Cómo se llama esto en inglés?", null, null, null, "Toothpaste"));
                allQuestions.add(new Question(R.drawable.socks, "¿Cómo se llama esto en inglés?", null, null, null, "Socks"));
                allQuestions.add(new Question(R.drawable.blanket, "¿Cómo se llama esto en inglés?", null, null, null, "Blanket"));
                allQuestions.add(new Question(R.drawable.towel, "¿Cómo se llama esto en inglés?", null, null, null, "Towel"));
                allQuestions.add(new Question(R.drawable.pillow, "¿Cómo se llama esto en inglés?", null, null, null, "Pillow"));
                allQuestions.add(new Question(R.drawable.broom, "¿Cómo se llama esto en inglés?", null, null, null, "Broom"));

                allQuestions.add(new Question(0, "How do you say '¿Podrías ayudarme, por favor?' in English?", null, null, null, "Could you help me, please?"));
                allQuestions.add(new Question(0, "How do you say 'Hace mucho frío hoy' in English?", null, null, null, "It's very cold today"));
                allQuestions.add(new Question(0, "How do you say '¿Qué tal tu día?' in English?", null, null, null, "How was your day?"));
                allQuestions.add(new Question(0, "How do you say 'Te llamo más tarde' in English?", null, null, null, "I'll call you later"));
                allQuestions.add(new Question(0, "How do you say '¿Dónde está la estación de tren?' in English?", null, null, null, "Where is the train station?"));
                allQuestions.add(new Question(0, "How do you say 'Estoy aprendiendo inglés' in English?", null, null, null, "I am learning English"));
                allQuestions.add(new Question(0, "How do you say '¿Cuánto cuesta esto?' in English?", null, null, null, "How much does this cost?"));
                allQuestions.add(new Question(0, "How do you say 'Me encantaría ir a la fiesta' in English?", null, null, null, "I would love to go to the party"));
                allQuestions.add(new Question(0, "How do you say '¿Puedes repetir eso, por favor?' in English?", null, null, null, "Can you repeat that, please?"));
                allQuestions.add(new Question(0, "How do you say '¿A qué hora empieza la película?' in English?", null, null, null, "What time does the movie start?"));
                allQuestions.add(new Question(0, "How do you say 'Necesito un médico' in English?", null, null, null, "I need a doctor"));
                allQuestions.add(new Question(0, "How do you say '¿Cómo se llega al aeropuerto?' in English?", null, null, null, "How do you get to the airport?"));
                allQuestions.add(new Question(0, "How do you say 'Tengo hambre' in English?", null, null, null, "I am hungry"));
                allQuestions.add(new Question(0, "How do you say '¿Puedes darme un ejemplo?' in English?", null, null, null, "Can you give me an example?"));
                allQuestions.add(new Question(0, "How do you say '¿Cuándo es tu cumpleaños?' in English?", null, null, null, "When is your birthday?"));
                allQuestions.add(new Question(0, "How do you say 'El tren está retrasado' in English?", null, null, null, "The train is delayed"));
                allQuestions.add(new Question(0, "How do you say '¿Hay alguna tienda cerca de aquí?' in English?", null, null, null, "Is there a store near here?"));
                allQuestions.add(new Question(0, "How do you say 'Este lugar es muy bonito' in English?", null, null, null, "This place is very beautiful"));
                allQuestions.add(new Question(0, "How do you say '¿Puedo pagar con tarjeta de crédito?' in English?", null, null, null, "Can I pay with a credit card?"));
                allQuestions.add(new Question(0, "How do you say 'Hace buen tiempo hoy' in English?", null, null, null, "The weather is nice today"));

                allQuestions.add(new Question(0, "How do you say '你覺得這個方案如何? (Nǐ juéde zhège fāng'àn rúhé?)' in English?", null, null, null, "What do you think of this plan?"));
                allQuestions.add(new Question(0, "How do you say '你最近忙嗎? (Nǐ zuìjìn máng ma?)' in English?", null, null, null, "Have you been busy recently?"));
                allQuestions.add(new Question(0, "How do you say '我需要更多的資訊 (Wǒ xūyào gèng duō de zīxùn)' in English?", null, null, null, "I need more information"));
                allQuestions.add(new Question(0, "How do you say '如果有問題請隨時聯絡我 (Rúguǒ yǒu wèntí qǐng suíshí liánluò wǒ)' in English?", null, null, null, "Please feel free to contact me if you have any questions"));
                allQuestions.add(new Question(0, "How do you say '我還沒準備好 (Wǒ hái méi zhǔnbèi hǎo)' in English?", null, null, null, "I’m not ready yet"));
                allQuestions.add(new Question(0, "How do you say '這樣做會有風險 (Zhèyàng zuò huì yǒu fēngxiǎn)' in English?", null, null, null, "This approach has risks"));
                allQuestions.add(new Question(0, "How do you say '我對這個問題很感興趣 (Wǒ duì zhège wèntí hěn gǎn xìngqù)' in English?", null, null, null, "I’m very interested in this issue"));
                allQuestions.add(new Question(0, "How do you say '他在會議中提出了一個很有價值的觀點 (Tā zài huìyì zhōng tíchūle yīgè hěn yǒu jiàzhí de guāndiǎn)' in English?", null, null, null, "He raised a very valuable point during the meeting"));
                allQuestions.add(new Question(0, "How do you say '我相信這將是一個成功的合作 (Wǒ xiāngxìn zhè jiāng shì yīgè chénggōng de hézuò)' in English?", null, null, null, "I believe this will be a successful collaboration"));
                allQuestions.add(new Question(0, "How do you say '我們的目標是達成長期的合作關係 (Wǒmen de mùbiāo shì dáchéng chángqī de hézuò guānxì)' in English?", null, null, null, "Our goal is to establish a long-term partnership"));
                allQuestions.add(new Question(0, "How do you say '你能給我更多的建議嗎? (Nǐ néng gěi wǒ gèng duō de jiànyì ma?)' in English?", null, null, null, "Could you give me more suggestions?"));
                allQuestions.add(new Question(0, "How do you say '這項計畫需要更多的資金支持 (Zhè xiàng jìhuà xūyào gèng duō de zījīn zhīchí)' in English?", null, null, null, "This project requires more financial support"));
                allQuestions.add(new Question(0, "How do you say '這個問題對我們的未來發展至關重要 (Zhège wèntí duì wǒmen de wèilái fāzhǎn zhìguān zhòngyào)' in English?", null, null, null, "This issue is crucial to our future development"));
                allQuestions.add(new Question(0, "How do you say '在這方面，我們還有很多可以改進的地方 (Zài zhè fāngmiàn, wǒmen hái yǒu hěn duō kěyǐ gǎijìn de dìfāng)' in English?", null, null, null, "In this regard, we still have a lot to improve"));
                allQuestions.add(new Question(0, "How do you say '我們必須克服這些挑戰才能取得成功 (Wǒmen bìxū kèfú zhèxiē tiǎozhàn cáinéng qǔdé chénggōng)' in English?", null, null, null, "We must overcome these challenges to succeed"));
                allQuestions.add(new Question(0, "How do you say '我希望我們能達成共識 (Wǒ xīwàng wǒmen néng dáchéng gòngshí)' in English?", null, null, null, "I hope we can reach a consensus"));
                allQuestions.add(new Question(0, "How do you say '這份報告需要進一步的修改 (Zhè fèn bàogào xūyào jìnyībù de xiūgǎi)' in English?", null, null, null, "This report needs further revisions"));
                allQuestions.add(new Question(0, "How do you say '我們將在下週的會議上討論這個問題 (Wǒmen jiāng zài xià zhōu de huìyì shàng tǎolùn zhège wèntí)' in English?", null, null, null, "We will discuss this issue at next week's meeting"));
                allQuestions.add(new Question(0, "How do you say '我對這項工作充滿熱情 (Wǒ duì zhè xiàng gōngzuò chōngmǎn rèqíng)' in English?", null, null, null, "I am passionate about this job"));
                allQuestions.add(new Question(0, "How do you say '你能解釋一下這個概念嗎? (Nǐ néng jiěshì yīxià zhège gàiniàn ma?)' in English?", null, null, null, "Can you explain this concept?"));


                allQuestions.add(new Question(0, "翻譯‘您能否為我提供有關該產品的更多詳細信息？’翻譯成英文： (Fānyì ‘Nín néngfǒu wèi wǒ tígōng yǒuguān gāi chǎnpǐn de gèng duō xiángxì xìnxī?’ fānyì chéng yīngwén:)", null, null, null, "Could you kindly provide me with more detailed information about the product?"));
                allQuestions.add(new Question(0, "翻譯‘我對下週開始的新項目感到非常興奮’翻譯成英文： (Fānyì ‘Wǒ duì xià zhōu kāishǐ de xīn xiàngmù gǎndào fēicháng xīngfèn’ fānyì chéng yīngwén:)", null, null, null, "I’m really excited about the new project that we are starting next week"));
                allQuestions.add(new Question(0, "翻譯‘我認為這將是一次非常具有挑戰性但有回報的經歷’翻譯成英文： (Fānyì ‘Wǒ rènwéi zhè jiāng shì yī cì fēicháng jùyǒu tiǎozhànxìng dàn yǒu huíbào de jīnglì’ fānyì chéng yīngwén:)", null, null, null, "I think it’s going to be a very challenging, but rewarding experience"));
                allQuestions.add(new Question(0, "翻譯‘我明年將出國學習，以提高我的語言技能’翻譯成英文： (Fānyì ‘Wǒ míngnián jiāng chūguó xuéxí, yǐ tígāo wǒ de yǔyán jìnéng’ fānyì chéng yīngwén:)", null, null, null, "I’m going to study abroad next year to improve my language skills"));
                allQuestions.add(new Question(0, "翻譯‘你能幫我完成這項任務嗎？我遇到了一些困難’翻譯成英文： (Fānyì ‘Nǐ néng bāng wǒ wánchéng zhè xiàng rènwù ma? Wǒ yùdào le yīxiē kùnnán’ fānyì chéng yīngwén:)", null, null, null, "Can you help me with this task? I’m having some difficulties"));
                allQuestions.add(new Question(0, "翻譯‘我已經在這個城市住了幾年了，我很喜歡這裡’翻譯成英文： (Fānyì ‘Wǒ yǐjīng zài zhège chéngshì zhùle jǐ nián le, wǒ hěn xǐhuān zhèlǐ’ fānyì chéng yīngwén:)", null, null, null, "I’ve been living in this city for a few years now, and I love it"));
                allQuestions.add(new Question(0, "翻譯‘到目前為止這是一個具有挑戰性但充實的經歷’翻譯成英文： (Fānyì ‘Dào mùqián wéizhǐ zhè shì yīgè jùyǒu tiǎozhànxìng dàn chōngshí de jīnglì’ fānyì chéng yīngwén:)", null, null, null, "It’s been a challenging yet fulfilling experience so far"));
                allQuestions.add(new Question(0, "翻譯‘我們需要為即將到來的項目提前計劃’翻譯成英文： (Fānyì ‘Wǒmen xūyào wèi jíjiāng dàolái de xiàngmù tíqián jìhuà’ fānyì chéng yīngwén:)", null, null, null, "We need to plan ahead for the upcoming project"));
                allQuestions.add(new Question(0, "翻譯‘我相信這個新系統將使我們的工作更加輕鬆’翻譯成英文： (Fānyì ‘Wǒ xiāngxìn zhège xīn xìtǒng jiāng shǐ wǒmen de gōngzuò gèng jiā qīngsōng’ fānyì chéng yīngwén:)", null, null, null, "I believe this new system will make our work much easier"));
                allQuestions.add(new Question(0, "翻譯‘我期待著親自見到每一個人’翻譯成英文： (Fānyì ‘Wǒ qídàizhe qīnzì jiàn dào měi yī gè rén’ fānyì chéng yīngwén:)", null, null, null, "I’m looking forward to meeting everyone in person"));
                allQuestions.add(new Question(0, "翻譯‘感謝您的理解和耐心’翻譯成英文： (Fānyì ‘Gǎnxiè nín de lǐjiě hé nàixīn’ fānyì chéng yīngwén:)", null, null, null, "Thank you for your understanding and patience"));
                allQuestions.add(new Question(0, "翻譯‘我們必須一起合作以實現我們的目標’翻譯成英文： (Fānyì ‘Wǒmen bìxū yīqǐ hézuò yǐ shíxiàn wǒmen de mùbiāo’ fānyì chéng yīngwén:)", null, null, null, "We must work together to achieve our goals"));

                allQuestions.add(new Question(0, "¿Qué opinas sobre la inteligencia artificial?", null, null, null, "What do you think about artificial intelligence?"));
                allQuestions.add(new Question(0, "Si pudieras aprender cualquier habilidad instantáneamente, ¿cuál escogerías?", null, null, null, "If you could instantly learn any skill, which one would you choose?"));
                allQuestions.add(new Question(0, "¿Crees que es más importante el éxito o la felicidad?", null, null, null, "Do you think success or happiness is more important?"));
                allQuestions.add(new Question(0, "¿Qué harías si tuvieras un millón de dólares?", null, null, null, "What would you do if you had a million dollars?"));
                allQuestions.add(new Question(0, "¿Qué te gustaría cambiar del mundo si pudieras?", null, null, null, "What would you like to change about the world if you could?"));
                allQuestions.add(new Question(0, "¿Cuál es tu mayor miedo?", null, null, null, "What is your biggest fear?"));
                allQuestions.add(new Question(0, "Si pudieras vivir en cualquier época, ¿cuál escogerías?", null, null, null, "If you could live in any era, which one would you choose?"));
                allQuestions.add(new Question(0, "¿Cómo definirías el éxito personal?", null, null, null, "How would you define personal success?"));
                allQuestions.add(new Question(0, "¿Qué harías si te quedaras sin internet por un mes?", null, null, null, "What would you do if you were without the internet for a month?"));
                allQuestions.add(new Question(0, "¿Cuál es la decisión más difícil que has tenido que tomar?", null, null, null, "What is the hardest decision you’ve ever had to make?"));
                allQuestions.add(new Question(0, "¿Crees que la tecnología nos acerca o nos aleja de las personas?", null, null, null, "Do you think technology brings us closer or distances us from people?"));
                allQuestions.add(new Question(0, "¿Qué harías si supieras que el mundo se acaba en un mes?", null, null, null, "What would you do if you knew the world was ending in a month?"));
                allQuestions.add(new Question(0, "Si pudieras tener una conversación con tu yo del futuro, ¿qué le preguntarías?", null, null, null, "If you could have a conversation with your future self, what would you ask?"));
                allQuestions.add(new Question(0, "¿Qué importancia le das al equilibrio entre la vida personal y profesional?", null, null, null, "How important is work-life balance to you?"));
                allQuestions.add(new Question(0, "¿Cuál es la lección más valiosa que has aprendido hasta ahora?", null, null, null, "What’s the most valuable lesson you’ve learned so far?"));
                allQuestions.add(new Question(0, "Si pudieras cambiar algo de ti mismo, ¿qué cambiarías?", null, null, null, "If you could change something about yourself, what would you change?"));
                allQuestions.add(new Question(0, "¿Qué significa para ti la palabra 'libertad'?", null, null, null, "What does the word 'freedom' mean to you?"));
                allQuestions.add(new Question(0, "¿Cuál consideras que es el mayor reto de las nuevas generaciones?", null, null, null, "What do you consider to be the biggest challenge for new generations?"));
                allQuestions.add(new Question(0, "¿Si pudieras vivir sin trabajar, cómo pasarías tu tiempo?", null, null, null, "If you could live without working, how would you spend your time?"));
                allQuestions.add(new Question(0, "¿Qué opinas sobre el cambio climático y qué crees que se debería hacer?", null, null, null, "What do you think about climate change and what do you think should be done?"));
                Collections.shuffle(allQuestions);
                questions = new ArrayList<>(allQuestions.subList(0, 30));
                break;
            default:
                throw new IllegalArgumentException("Invalid difficulty level: " + level);
        }
        Collections.shuffle(allQuestions);

    }



    private void loadQuestion() {
        answerGroup.clearCheck();
        Question currentQuestion = questions.get(currentQuestionIndex);
        Button micButton = findViewById(R.id.mic_button);
        micButton.setOnClickListener(v -> startSpeechRecognition());

        initializeSpeechRecognizer();

        RadioButton answer1 = findViewById(R.id.answer_1);
        RadioButton answer2 = findViewById(R.id.answer_2);
        RadioButton answer3 = findViewById(R.id.answer_3);
        Button speakerButton = findViewById(R.id.speaker_button);

        // Hide speaker button by default
        speakerButton.setVisibility(View.GONE);

        if (currentQuestion.getImageResourceId() != 0) {
            questionImage.setImageResource(currentQuestion.getImageResourceId());
            questionImage.setVisibility(View.VISIBLE);
            questionText.setVisibility(View.GONE);
        } else {
            questionImage.setVisibility(View.GONE);
            questionText.setVisibility(View.VISIBLE);
            questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));

            // Only show speaker button for audio questions in hard mode
            if (selectedLevel.equalsIgnoreCase("hard") && currentQuestion.getQuestionText().startsWith("🔊")) {
                speakerButton.setVisibility(View.VISIBLE);
            }
        }

        if (selectedLevel.equalsIgnoreCase("easy")) {
            questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));
            questionText.setVisibility(View.VISIBLE);

            List<String> answers = new ArrayList<>();
            answers.add(currentQuestion.getOption1());
            answers.add(currentQuestion.getOption2());
            answers.add(currentQuestion.getOption3());
            Collections.shuffle(answers);

            answer1.setText(answers.get(0));
            answer2.setText(answers.get(1));
            answer3.setText(answers.get(2));

            answer1.setVisibility(View.VISIBLE);
            answer2.setVisibility(View.VISIBLE);
            answer3.setVisibility(View.VISIBLE);
            answerInput.setVisibility(View.GONE);
            answerGroup.setVisibility(View.VISIBLE);

        } else if (selectedLevel.equalsIgnoreCase("mediate")) {
            questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));
            questionText.setVisibility(View.VISIBLE);
            micButton.setVisibility(View.GONE);

            answer1.setVisibility(View.GONE);
            answer2.setVisibility(View.GONE);
            answer3.setVisibility(View.GONE);
            answerGroup.setVisibility(View.VISIBLE);
            answerInput.setVisibility(View.VISIBLE);
            answerInput.setText("");

            if (currentQuestion.getQuestionText().startsWith("How do you say")) {
                micButton.setVisibility(View.VISIBLE);
                answerInput.setHint("Press the mic to speak");

            } else {
                micButton.setVisibility(View.GONE);
                answerInput.setHint("Enter your answer here");
            }

        } else if (selectedLevel.equalsIgnoreCase("hard")) {

            questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));
            questionText.setVisibility(View.VISIBLE);
            micButton.setVisibility(View.GONE);

            if (currentQuestion.getQuestionText().startsWith("🔊")) {
                questionText.setVisibility(View.GONE);
                speakerButton.setVisibility(View.VISIBLE);
            } else {
                questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));
                questionText.setVisibility(View.VISIBLE);
            }

            if (currentQuestion.getQuestionText().startsWith("How do you say")) {
                micButton.setVisibility(View.VISIBLE);
                answerInput.setHint("Press the mic to speak");
            } else {
                micButton.setVisibility(View.GONE);
                answerInput.setHint("Enter your answer here");
            }
            questionText.setVisibility(View.VISIBLE);
            answer1.setVisibility(View.GONE);
            answer2.setVisibility(View.GONE);
            answer3.setVisibility(View.GONE);
            answerGroup.setVisibility(View.VISIBLE);
            answerInput.setVisibility(View.VISIBLE);
            answerInput.setText("");
            answerInput.setHint("Enter your answer here");
            answerInput.setFocusableInTouchMode(true);
            answerInput.setClickable(true);
            answerInput.setLongClickable(true);
        }

        timerTextView.setVisibility(View.VISIBLE);
        startTimer();
        updateProgressBar();
    }

    private void speakCorrectAnswer() {
        if (textToSpeech != null && !textToSpeech.isSpeaking()) {
            Question currentQuestion = questions.get(currentQuestionIndex);
            String textToSpeak = currentQuestion.getCorrectAnswer();
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    private void updateProgressBar() {
        int progress = (currentQuestionIndex * 100) / questions.size();
        progressBar.setProgress(progress);
    }

    private void saveScore(int score) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_ENGLISH_QUIZ_SCORE, score);
        editor.apply();
    }

    private void checkAnswer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        String userAnswer;
        if (selectedLevel.equalsIgnoreCase("easy")) {
            RadioButton selectedAnswer = findViewById(answerGroup.getCheckedRadioButtonId());
            userAnswer = selectedAnswer != null ? selectedAnswer.getText().toString() : "";
        } else {
            userAnswer = answerInput.getText().toString().trim();
        }

        userAnswers.add(userAnswer);
        if (userAnswer.equalsIgnoreCase(questions.get(currentQuestionIndex).getCorrectAnswer())) {
            score++;
        }
    }


    private void proceedToNextLevel() {
        if (easyPassed && selectedLevel.equalsIgnoreCase("easy")) {
            selectedLevel = "mediate";
            RadioButton mediateRadio = findViewById(R.id.mediate_radio);
            mediateRadio.setEnabled(true);
            mediateRadio.setChecked(true);
        } else if (mediatePassed && selectedLevel.equalsIgnoreCase("mediate")) {
            selectedLevel = "hard";
            RadioButton hardRadio = findViewById(R.id.hard_radio);
            hardRadio.setEnabled(true);
            hardRadio.setChecked(true);
        }

        // Ensure all passed levels remain enabled
        RadioButton easyRadio = findViewById(R.id.easy_radio);
        RadioButton mediateRadio = findViewById(R.id.mediate_radio);
        easyRadio.setEnabled(true);
        mediateRadio.setEnabled(easyPassed);

        resetQuiz();
        proceedButton.setVisibility(View.GONE);
    }



    private void showScore() {

        int totalQuestions = questions.size();
        double scorePercentage = (double) score / totalQuestions * 100;

        String resultMessage;
        boolean levelPassed = false;

        if (selectedLevel.equalsIgnoreCase("easy")) {
            if (scorePercentage >= 53.33) {
                easyPassed = true;
                levelPassed = true;
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(ENGLISH_EASY_PASSED, true);
                editor.apply();
                resultMessage = "<br><font color='#4CAF50'> Congratulations! You can proceed to the next level.</font><br>";
            } else {
                resultMessage = "<br><font color='#F44336'> You need to improve your score to proceed to the next level.</font><br>";
            }
        } else if (selectedLevel.equalsIgnoreCase("mediate")) {
                if (scorePercentage >= 60) {
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(ENGLISH_MEDIATE_PASSED, true);
                    editor.apply();
                mediatePassed = true;
                levelPassed = true;
                resultMessage = "<br><font color='#4CAF50'>Passed - Congratulations! You can proceed to the next level.</font><br>";
            } else {
                resultMessage = "<br><font color='#F44336'> You need to improve your score to proceed to the next level.</font><br>";
                // Ensure easy level remains unlocked even if mediate is failed
                easyPassed = true;
            }
        } else if (selectedLevel.equalsIgnoreCase("hard")) {
            if (score >= 50) { // 15/30
                levelPassed = true;
                resultMessage = "<br><font color='#4CAF50'> Congratulations! You have passed.</font><br>";
            } else {
                resultMessage = "<br><font color='#F44336'> Please review the lesson and try again.</font><br>";
            }
            easyPassed = true;
            mediatePassed = true;
        } else {
            resultMessage = "Quiz completed!";
        }

        if (levelPassed) {
            proceedButton.setVisibility(View.VISIBLE);
            if (selectedLevel.equalsIgnoreCase("easy")) {
                proceedButton.setText("Proceed");
                easyPassed = true;
                playSound(true); // Play passed sound
                Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();
            } else if (selectedLevel.equalsIgnoreCase("mediate")) {
                proceedButton.setText("Proceed");
                easyPassed = true; // Ensure ang easy remains passed parin
                mediatePassed = true;
                playSound(true); // Play passed sound
                Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();
            }
        } else {
            proceedButton.setVisibility(View.GONE);
            if (selectedLevel.equalsIgnoreCase("easy")) {
                playSound(false); // Play fail sound
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
            } else if (selectedLevel.equalsIgnoreCase("mediate")) {
                playSound(false); // Play fail sound
                Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();;
            }
        }

        if (selectedLevel.equalsIgnoreCase("mediate")) {
            micButton.setVisibility(View.GONE);
        }
        saveScore(score);


        StringBuilder result = new StringBuilder();
        result.append("<br>Your Score: ").append(score).append("/").append(totalQuestions)
                .append(" - ").append(resultMessage).append("<br><br>");

        submitButton.setVisibility(View.GONE);
        retakeButton.setVisibility(View.VISIBLE);
        spaceButton.setVisibility(View.VISIBLE);
        TextView text = findViewById(R.id.text);
        text.setVisibility(View.VISIBLE);
        timerTextView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        questionImage.setVisibility(View.GONE);
        speakerButton.setVisibility(View.GONE);

        saveScore(score);

        String radioButtonDefault = "<font size='30'>〇</font>";
        String radioButtonCheckedGreen = " 🟢";
        String radioButtonCheckedRed = "\uD83D\uDD34";

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            result.append("----------------------------------------------------<br>");
            result.append(question.getQuestionText()).append("<br><br>");

            if (question.getImageResourceId() != 0) {
                result.append("<img src='").append(question.getImageResourceId()).append("'><br>");
            }

            String userAnswer = userAnswers.get(i);

            if (selectedLevel.equalsIgnoreCase("easy")) {
                result.append(
                        (question.getOption1().equals(userAnswer)
                                ? (userAnswer.equals(question.getCorrectAnswer())
                                ? radioButtonCheckedGreen
                                : radioButtonCheckedRed)
                                : radioButtonDefault)).append(" ").append(question.getOption1()).append("<br>");

                result.append(
                        (question.getOption2().equals(userAnswer)
                                ? (userAnswer.equals(question.getCorrectAnswer())
                                ? radioButtonCheckedGreen
                                : radioButtonCheckedRed)
                                : radioButtonDefault)).append(" ").append(question.getOption2()).append("<br>");

                result.append(
                        (question.getOption3().equals(userAnswer)
                                ? (userAnswer.equals(question.getCorrectAnswer())
                                ? radioButtonCheckedGreen
                                : radioButtonCheckedRed)
                                : radioButtonDefault)).append(" ").append(question.getOption3()).append("<br>");


            } else {
                if (userAnswer.equalsIgnoreCase(question.getCorrectAnswer())) {
                    result.append("<font color='#4CAF50'> Your answer: ").append(userAnswer).append("</font> <br>");
                } else {
                    result.append("<font color='#F44336'> Your answer: ").append(userAnswer).append("</font> <br>");
                }
            }

            if (userAnswer.equalsIgnoreCase(question.getCorrectAnswer())) {
                result.append("<font color='#4CAF50'>Correct Answer: ").append(question.getCorrectAnswer()).append("</font><br><br>");
            } else {
                result.append("<font color='#F44336'>Correct Answer: ").append(question.getCorrectAnswer()).append("</font><br><br>");
            }

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            questionText.setText(Html.fromHtml(result.toString(), Html.FROM_HTML_MODE_COMPACT, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(String source) {
                    int id = Integer.parseInt(source);
                    Drawable drawable = getResources().getDrawable(id, null);
                    int width = questionText.getWidth();
                    if (width <= 0) {
                        width = questionText.getResources().getDisplayMetrics().widthPixels / 2; // Use half of screen width
                    }
                    int height = width * drawable.getIntrinsicHeight() / drawable.getIntrinsicWidth();
                    drawable.setBounds(180, 0, 455, 235);
                    return drawable;
                }
            }, null));
        }
        if (selectedLevel.equalsIgnoreCase("hard")) {
            for (int i = 0; i < questions.size(); i++) {
                Question question = questions.get(i);
                result.append("----------------------------------------------------<br>");
                if (question.getQuestionText().startsWith("🔊")) {
                    result.append("Audio Question: ").append(question.getQuestionText().substring(2)).append("<br>");
                } else {
                    result.append(question.getQuestionText()).append("<br>");
                }

                if (question.getImageResourceId() != 0) {
                    result.append("<img src='").append(question.getImageResourceId()).append("'><br>");
                }

                String userAnswer = userAnswers.get(i);
                String correctAnswer = question.getCorrectAnswer();

                if (userAnswer.equalsIgnoreCase(correctAnswer)) {
                    result.append("<font color='#4CAF50'>Your answer: ").append(userAnswer).append("</font><br>");
                    result.append("<font color='#4CAF50'>Correct Answer: ").append(correctAnswer).append("</font><br><br>");
                } else {
                    result.append("<font color='#F44336'>Your answer: ").append(userAnswer).append("</font><br>");
                    result.append("<font color='#F44336'>Correct Answer: ").append(correctAnswer).append("</font><br><br>");
                }
            }
        }

        findViewById(R.id.answer_1).setVisibility(View.GONE);
        findViewById(R.id.answer_2).setVisibility(View.GONE);
        findViewById(R.id.answer_3).setVisibility(View.GONE);
        answerInput.setVisibility(View.GONE);


        dbHelper.insertQuizResult( score, questions.size(), selectedLevel, selectedLevel, "English");


    }


    public static class Question {
        private int imageResourceId;
        private String questionText;
        private String option1, option2, option3;
        private String correctAnswer;

        public Question(int imageResourceId, String questionText, String option1, String option2, String option3, String correctAnswer) {
            this.imageResourceId = imageResourceId;
            this.questionText = questionText;
            this.option1 = option1;
            this.option2 = option2;
            this.option3 = option3;
            this.correctAnswer = correctAnswer;
        }

        public int getImageResourceId() { return imageResourceId; }
        public String getQuestionText() { return questionText; }
        public String getOption1() { return option1; }
        public String getOption2() { return option2; }
        public String getOption3() { return option3; }
        public String getCorrectAnswer() { return correctAnswer; }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle the back button click
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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


}

