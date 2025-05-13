    package com.IT4A.langhub;

    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.content.pm.PackageManager;
    import android.graphics.drawable.Drawable;
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
    import android.media.MediaPlayer;

    public class SpainPage extends AppCompatActivity {

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
        private String selectedLevel = "   easy";
        private long timeLeftInMillis = 10000;
        private List<Question> questions;
        private List<String> userAnswers = new ArrayList<>();
        private QuizDatabaseHelper dbHelper;

        private static final String KEY_SPAIN_QUIZ_SCORE = "SpainQuizScore";
        private TextToSpeech textToSpeech;
        private Button speakerButton;


        private static final String PREFS_NAME = "QuizScores";
        private static final String SPAIN_EASY_PASSED = "spainEasyPassed";
        private static final String SPAIN_MEDIATE_PASSED = "spainMediatePassed";


        private boolean easyPassed = false;
        private boolean mediatePassed = false;
        private Button proceedButton;


        private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
        private SpeechRecognizer speechRecognizer;
        private MediaPlayer mediaPlayer;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_spain);

            dbHelper = new QuizDatabaseHelper(this);

            Toolbar toolbar = findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("Spanish Quiz");
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
            easyPassed = prefs.getBoolean(SPAIN_EASY_PASSED, false);
            mediatePassed = prefs.getBoolean(SPAIN_MEDIATE_PASSED, false);

            RadioButton easyRadio = findViewById(R.id.easy_radio);
            RadioButton mediateRadio = findViewById(R.id.mediate_radio);
            RadioButton hardRadio = findViewById(R.id.hard_radio);

            easyRadio.setEnabled(true);


            mediateRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!easyPassed) {
                        // Show popup message
                        Toast.makeText(SpainPage.this, "You need to pass the easy level to unlock this", Toast.LENGTH_SHORT).show();
                        mediateRadio.setChecked(false);
                        // Ensure ang easy radio button remains checked
                        easyRadio.setChecked(true);
                    }
                }
            });

            hardRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mediatePassed) {
                        Toast.makeText(SpainPage.this, "You need to pass the mediate level to unlock this", Toast.LENGTH_SHORT).show();
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
                    int result = textToSpeech.setLanguage(new Locale("es", "ES"));
                    float speechRate = 0.8f; //BILIS NG TTS
                    textToSpeech.setSpeechRate(speechRate);

                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(this, "Spanish language is not supported", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(SpainPage.this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
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
            loadQuestion();
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
                    Toast.makeText(SpainPage.this, errorMessage, Toast.LENGTH_SHORT).show();
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
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer in Spanish");
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
                    allQuestions.add(new Question(R.drawable.plate, "How do you say this in Spanish?", "Plato", "Tenedor", "Vaso", "Plato"));
                    allQuestions.add(new Question(R.drawable.fork, "What’s the Spanish word for this?", "Tenedor", "Plato", "Vaso", "Tenedor"));
                    allQuestions.add(new Question(R.drawable.glass, "What do you call this in Spanish?", "Vaso", "Tenedor", "Plato", "Vaso"));
                    allQuestions.add(new Question(R.drawable.cup, "What’s this in Spanish?", "Taza", "Tenedor", "Plato", "Taza"));
                    allQuestions.add(new Question(R.drawable.spoon, "How would you say this in Spanish?", "Cuchara", "Plato", "Tenedor", "Cuchara"));
                    allQuestions.add(new Question(R.drawable.bottle, "How is this called in Spanish?", "Botella", "Taza", "Plato", "Botella"));
                    allQuestions.add(new Question(R.drawable.book, "What’s the Spanish translation for this?", "Libro", "Mesa", "Silla", "Libro"));
                    allQuestions.add(new Question(R.drawable.chair, "What’s the Spanish term for this?", "Silla", "Libro", "Mesa", "Silla"));
                    allQuestions.add(new Question(R.drawable.table, "What do you call this in Spanish?", "Mesa", "Silla", "Libro", "Mesa"));
                    allQuestions.add(new Question(R.drawable.phone, "How do you say this in Spanish?", "Teléfono", "Móvil", "Libro", "Móvil"));
                    allQuestions.add(new Question(R.drawable.laptop, "How would you refer to this in Spanish?", "Portátil", "Mesa", "Teléfono", "Portátil"));
                    allQuestions.add(new Question(R.drawable.desk, "What’s the name for this in Spanish?", "Escritorio", "Silla", "Mesa", "Escritorio"));
                    allQuestions.add(new Question(R.drawable.computer, "How is this called in Spanish?", "Computadora", "Libro", "Silla", "Computadora"));
                    allQuestions.add(new Question(R.drawable.window, "What’s this in Spanish?", "Ventana", "Puerta", "Pared", "Ventana"));
                    allQuestions.add(new Question(R.drawable.door, "What do you call this in Spanish?", "Puerta", "Ventana", "Pared", "Puerta"));
                    allQuestions.add(new Question(R.drawable.tv, "How would you say this in Spanish?", "Televisión", "Teléfono", "Computadora", "Televisión"));
                    allQuestions.add(new Question(R.drawable.car, "What is this called in Spanish?", "Coche", "Bicicleta", "Motocicleta", "Coche"));
                    allQuestions.add(new Question(R.drawable.bike, "How do you say this in Spanish?", "Bicicleta", "Motocicleta", "Coche", "Bicicleta"));
                    allQuestions.add(new Question(R.drawable.clock, "What’s this in Spanish?", "Reloj", "Brazalete", "Computadora", "Reloj"));
                    allQuestions.add(new Question(R.drawable.watch, "What would you call this in Spanish?", "Reloj de pulsera", "Reloj", "Brazalete", "Reloj de pulsera"));
                    allQuestions.add(new Question(R.drawable.bag, "How would you say this in Spanish?", "Bolsa", "Zapatos", "Sombrero", "Bolsa"));
                    allQuestions.add(new Question(R.drawable.hat, "What’s the Spanish word for this?", "Sombrero", "Bolsa", "Zapatos", "Sombrero"));
                    allQuestions.add(new Question(R.drawable.shirt, "What’s the Spanish term for this?", "Camiseta", "Pantalones", "Zapatos", "Camiseta"));
                    allQuestions.add(new Question(R.drawable.pants, "How do you say this in Spanish?", "Pantalones", "Camiseta", "Zapatos", "Pantalones"));
                    allQuestions.add(new Question(R.drawable.shoes, "What do you call these in Spanish?", "Zapatos", "Pantalones", "Sombrero", "Zapatos"));
                    allQuestions.add(new Question(R.drawable.jacket, "How is this called in Spanish?", "Chaqueta", "Pantalones", "Camiseta", "Chaqueta"));
                    allQuestions.add(new Question(R.drawable.glove, "What’s the Spanish word for this?", "Guante", "Zapatos", "Sombrero", "Guante"));
                    allQuestions.add(new Question(R.drawable.scarf, "How would you say this in Spanish?", "Bufanda", "Sombrero", "Guante", "Bufanda"));
                    allQuestions.add(new Question(R.drawable.shot, "How do you call this in Spanish?", "Pantalones cortos", "Pantalones", "Camiseta", "Pantalones cortos"));
                    allQuestions.add(new Question(R.drawable.glasses, "What’s the Spanish word for this?", "Gafas", "Sombrero", "Bolsa", "Gafas"));
                    allQuestions.add(new Question(R.drawable.key, "How do you say this in Spanish?", "Llave", "Puerta", "Ventana", "Llave"));
                    allQuestions.add(new Question(R.drawable.comb, "What’s the Spanish term for this?", "Peina", "Espejo", "Toalla", "Peina"));
                    allQuestions.add(new Question(R.drawable.toothbrush, "What do you call this in Spanish?", "Cepillo de dientes", "Pasta de dientes", "Peina", "Cepillo de dientes"));
                    allQuestions.add(new Question(R.drawable.toothpaste, "What is this in Spanish?", "Pasta de dientes", "Cepillo de dientes", "Toalla", "Pasta de dientes"));
                    allQuestions.add(new Question(R.drawable.socks, "What’s the Spanish word for these?", "Calcetines", "Zapatos", "Pantalones", "Calcetines"));
                    allQuestions.add(new Question(R.drawable.blanket, "How would you say this in Spanish?", "Manta", "Almohada", "Cama", "Manta"));
                    allQuestions.add(new Question(R.drawable.towel, "What is this called in Spanish?", "Toalla", "Almohada", "Manta", "Toalla"));
                    allQuestions.add(new Question(R.drawable.pillow, "How do you say this in Spanish?", "Almohada", "Cama", "Manta", "Almohada"));
                    allQuestions.add(new Question(R.drawable.broom, "What’s this called in Spanish?", "Escoba", "Mopa", "Cubo", "Escoba"));

                    allQuestions.add(new Question(0, "Translate 'To work' into Spanish:", "Para trabajar", "A la ciudad", "Al baño", "Para trabajar"));
                    allQuestions.add(new Question(0, "Translate 'No' into Spanish:", "No", "Sí", "Tal vez", "No"));
                    allQuestions.add(new Question(0, "Translate 'Do you understand?' into Spanish:", "¿Entiendes?", "¿Hablas?", "¿Dónde?", "¿Entiendes?"));
                    allQuestions.add(new Question(0, "Translate 'I don't understand' into Spanish:", "No entiendo", "Entiendo", "Hablas inglés", "No entiendo"));
                    allQuestions.add(new Question(0, "Translate 'Where?' into Spanish:", "¿Dónde?", "¿Cuándo?", "¿Qué?", "¿Dónde?"));
                    allQuestions.add(new Question(0, "Translate 'What?' into Spanish:", "¿Qué?", "¿Cómo?", "¿Dónde?", "¿Qué?"));
                    allQuestions.add(new Question(0, "Translate 'How?' into Spanish:", "¿Cómo?", "¿Cuándo?", "¿Por qué?", "¿Cómo?"));
                    allQuestions.add(new Question(0, "Translate 'How much?' into Spanish:", "¿Cuánto cuesta?", "¿Dónde?", "¿Por qué?", "¿Cuánto cuesta?"));
                    allQuestions.add(new Question(0, "Translate 'When?' into Spanish:", "¿Cuándo?", "¿Cómo?", "¿Dónde?", "¿Cuándo?"));
                    allQuestions.add(new Question(0, "Translate 'Who?' into Spanish:", "¿Quién?", "¿Qué?", "¿Cómo?", "¿Quién?"));
                    allQuestions.add(new Question(0, "Translate 'Why?' into Spanish:", "¿Por qué?", "¿Cómo?", "¿Dónde?", "¿Por qué?"));
                    allQuestions.add(new Question(0, "Translate 'Thank you' into Spanish:", "Gracias", "Por favor", "Lo siento", "Gracias"));
                    allQuestions.add(new Question(0, "Translate 'I'm sorry' into Spanish:", "Lo siento", "Gracias", "Por favor", "Lo siento"));
                    allQuestions.add(new Question(0, "Translate 'Congratulations!' into Spanish:", "¡Felicidades!", "¡Bien hecho!", "¡Bravo!", "¡Felicidades!"));
                    allQuestions.add(new Question(0, "Translate 'It's okay' into Spanish:", "Está bien", "No importa", "Perdón", "Está bien"));
                    allQuestions.add(new Question(0, "Translate 'I don't know' into Spanish:", "No sé", "Sí sé", "No entiendo", "No sé"));
                    allQuestions.add(new Question(0, "Translate 'I don't like it' into Spanish:", "No me gusta", "Me gusta", "Lo quiero", "No me gusta"));
                    allQuestions.add(new Question(0, "Translate 'I like it' into Spanish:", "Me gusta", "No me gusta", "Me encanta", "Me gusta"));
                    allQuestions.add(new Question(0, "Translate 'You're welcome' into Spanish:", "De nada", "Por favor", "No hay problema", "De nada"));
                    allQuestions.add(new Question(0, "Translate 'I think that...' into Spanish:", "Pienso que", "Creo que", "Opino que", "Pienso que"));
                    allQuestions.add(new Question(0, "Translate 'No, thank you' into Spanish:", "No, gracias", "Sí, gracias", "No, por favor", "No, gracias"));
                    allQuestions.add(new Question(0, "Translate 'Excuse me' into Spanish:", "Disculpe", "Perdón", "Lo siento", "Disculpe"));
                    allQuestions.add(new Question(0, "Translate 'Take care' into Spanish:", "Cuídate", "Vete", "Cuidado", "Cuídate"));
                    allQuestions.add(new Question(0, "Translate 'Don't forget' into Spanish:", "No olvides", "Recuerda", "Olvídalo", "No olvides"));
                    allQuestions.add(new Question(0, "Translate 'How do you pronounce this?' into Spanish:", "¿Cómo se pronuncia esto?", "¿Qué significa esto?", "¿Dónde está esto?", "¿Cómo se pronuncia esto?"));
                    allQuestions.add(new Question(0, "Translate 'Before' into Spanish:", "Antes", "Después", "Durante", "Antes"));
                    allQuestions.add(new Question(0, "Translate 'After' into Spanish:", "Después", "Antes", "Durante", "Después"));
                    allQuestions.add(new Question(0, "Translate 'Wrong' into Spanish:", "Incorrecto", "Correcto", "Claro", "Incorrecto"));
                    allQuestions.add(new Question(0, "Translate 'Right' into Spanish:", "Correcto", "Incorrecto", "Claro", "Correcto"));
                    allQuestions.add(new Question(0, "Translate 'Until' into Spanish:", "Hasta", "Después", "Antes", "Hasta"));
                    allQuestions.add(new Question(0, "Translate 'Where is the toilet?' into Spanish:", "¿Dónde está el baño?", "¿Dónde está el restaurante?", "¿Dónde está la tienda?", "¿Dónde está el baño?"));
                    allQuestions.add(new Question(0, "Translate 'Do you live here?' into Spanish:", "¿Vives aquí?", "¿Vas aquí?", "¿Trabajas aquí?", "¿Vives aquí?"));
                    allQuestions.add(new Question(0, "Translate 'Do you like it?' into Spanish:", "¿Te gusta?", "¿Te encanta?", "¿Lo prefieres?", "¿Te gusta?"));
                    allQuestions.add(new Question(0, "Translate 'I love it' into Spanish:", "Me encanta", "Me gusta mucho", "Lo quiero", "Me encanta"));
                    allQuestions.add(new Question(0, "Translate 'On business' into Spanish:", "En negocios", "De vacaciones", "De viaje", "En negocios"));
                    allQuestions.add(new Question(0, "Translate 'To work' into Spanish:", "Para trabajar", "Para estudiar", "Para descansar", "Para trabajar"));
                    allQuestions.add(new Question(0, "Translate 'What happened?' into Spanish:", "¿Qué pasó?", "¿Qué hiciste?", "¿Dónde fuiste?", "¿Qué pasó?"));
                    allQuestions.add(new Question(0, "Translate 'Do you need help?' into Spanish:", "¿Necesitas ayuda?", "¿Puedo ayudarte?", "¿Tienes ayuda?", "¿Necesitas ayuda?"));
                    allQuestions.add(new Question(0, "Translate 'I'm lost' into Spanish:", "Estoy perdido", "Estoy aquí", "Estoy bien", "Estoy perdido"));
                    allQuestions.add(new Question(0, "Translate 'What time is it?' into Spanish:", "¿Qué hora es?", "¿Qué día es?", "¿A qué hora?", "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "Translate 'I want to go there' into Spanish:", "Quiero ir allí", "Voy allá", "Me voy", "Quiero ir allí"));
                    allQuestions.add(new Question(0, "Translate 'How far is it?' into Spanish:", "¿Qué tan lejos está?", "¿Está cerca?", "¿Está lejos?", "¿Qué tan lejos está?"));
                    allQuestions.add(new Question(0, "Translate 'Can I help you?' into Spanish:", "¿Puedo ayudarte?", "¿Te ayudo?", "¿Quieres ayuda?", "¿Puedo ayudarte?"));
                    allQuestions.add(new Question(0, "Translate 'Do you speak English?' into Spanish:", "¿Hablas inglés?", "¿Hablas español?", "¿Sabes inglés?", "¿Hablas inglés?"));
                    allQuestions.add(new Question(0, "Translate 'I need a doctor' into Spanish:", "Necesito un médico", "Quiero un médico", "No me siento bien", "Necesito un médico"));
                    allQuestions.add(new Question(0, "Translate 'Call the police' into Spanish:", "Llama a la policía", "Llama a la ambulancia", "Llama a un taxi", "Llama a la policía"));

                    allQuestions.add(new Question(0, "What is 'Red' in Spanish?", "Rojo", "Azul", "Verde", "Rojo"));
                    allQuestions.add(new Question(0, "What is 'Blue' in Spanish?", "Azul", "Rojo", "Verde", "Azul"));
                    allQuestions.add(new Question(0, "What is 'Green' in Spanish?", "Verde", "Rojo", "Azul", "Verde"));
                    allQuestions.add(new Question(0, "What is 'Yellow' in Spanish?", "Amarillo", "Rojo", "Azul", "Amarillo"));
                    allQuestions.add(new Question(0, "What is 'Black' in Spanish?", "Negro", "Blanco", "Azul", "Negro"));
                    allQuestions.add(new Question(0, "What is 'White' in Spanish?", "Blanco", "Negro", "Azul", "Blanco"));
                    allQuestions.add(new Question(0, "What is 'Pink' in Spanish?", "Rosa", "Rojo", "Amarillo", "Rosa"));
                    allQuestions.add(new Question(0, "What is 'Purple' in Spanish?", "Morado", "Azul", "Rojo", "Morado"));
                    allQuestions.add(new Question(0, "What is 'Orange' in Spanish?", "Naranja", "Rojo", "Amarillo", "Naranja"));
                    allQuestions.add(new Question(0, "What is 'Brown' in Spanish?", "Marrón", "Negro", "Azul", "Marrón"));
                    allQuestions.add(new Question(0, "What is 'Gray' in Spanish?", "Gris", "Negro", "Blanco", "Gris"));
                    allQuestions.add(new Question(0, "What is 'Beige' in Spanish?", "Beige", "Gris", "Marrón", "Beige"));
                    allQuestions.add(new Question(0, "What is 'Turquoise' in Spanish?", "Turquesa", "Verde", "Azul", "Turquesa"));
                    allQuestions.add(new Question(0, "What is 'Gold' in Spanish?", "Oro", "Plata", "Bronce", "Oro"));
                    allQuestions.add(new Question(0, "What is 'Silver' in Spanish?", "Plata", "Oro", "Bronce", "Plata"));
                    allQuestions.add(new Question(0, "What is 'Copper' in Spanish?", "Cobre", "Oro", "Plata", "Cobre"));
                    allQuestions.add(new Question(0, "What is 'Bronze' in Spanish?", "Bronce", "Oro", "Plata", "Bronce"));
                    allQuestions.add(new Question(0, "What is 'Magenta' in Spanish?", "Magenta", "Violeta", "Rosado", "Magenta"));
                    allQuestions.add(new Question(0, "What is 'Violet' in Spanish?", "Violeta", "Azul", "Rojo", "Violeta"));
                    allQuestions.add(new Question(0, "What is 'Indigo' in Spanish?", "Índigo", "Azul", "Verde", "Índigo"));
                    allQuestions.add(new Question(0, "What is 'Lavender' in Spanish?", "Lavanda", "Morado", "Rosa", "Lavanda"));
                    allQuestions.add(new Question(0, "What is 'Peach' in Spanish?", "Durazno", "Naranja", "Amarillo", "Durazno"));
                    allQuestions.add(new Question(0, "What is 'Ivory' in Spanish?", "Marfil", "Blanco", "Gris", "Marfil"));
                    allQuestions.add(new Question(0, "What is 'Cream' in Spanish?", "Crema", "Blanco", "Amarillo", "Crema"));
                    allQuestions.add(new Question(0, "What is 'Charcoal' in Spanish?", "Carbón", "Gris", "Negro", "Carbón"));
                    allQuestions.add(new Question(0, "What is 'Emerald' in Spanish?", "Esmeralda", "Verde", "Amarillo", "Esmeralda"));
                    allQuestions.add(new Question(0, "What is 'Cyan' in Spanish?", "Cian", "Azul", "Verde", "Cian"));
                    allQuestions.add(new Question(0, "What is 'Mint' in Spanish?", "Menta", "Verde", "Amarillo", "Menta"));
                    allQuestions.add(new Question(0, "What is 'Lime' in Spanish?", "Lima", "Verde", "Amarillo", "Lima"));
                    allQuestions.add(new Question(0, "What is 'Plum' in Spanish?", "Ciruela", "Rojo", "Morado", "Ciruela"));
                    allQuestions.add(new Question(0, "What is 'Coral' in Spanish?", "Coral", "Rosa", "Naranja", "Coral"));
                    allQuestions.add(new Question(0, "What is 'Tan' in Spanish?", "Canela", "Beige", "Marrón", "Canela"));
                    allQuestions.add(new Question(0, "What is 'Rust' in Spanish?", "Óxido", "Marrón", "Naranja", "Óxido"));
                    allQuestions.add(new Question(0, "What is 'Sapphire' in Spanish?", "Zafiro", "Azul", "Morado", "Zafiro"));
                    allQuestions.add(new Question(0, "What is 'Scarlet' in Spanish?", "Escarlata", "Rojo", "Amarillo", "Escarlata"));
                    allQuestions.add(new Question(0, "What is 'Burgundy' in Spanish?", "Burdeos", "Rojo", "Marrón", "Burdeos"));
                    allQuestions.add(new Question(0, "What is 'Azure' in Spanish?", "Azul celeste", "Azul", "Verde", "Azul celeste"));
                    allQuestions.add(new Question(0, "What is 'Seafoam' in Spanish?", "Espuma de mar", "Verde", "Azul", "Espuma de mar"));
                    allQuestions.add(new Question(0, "What is 'Sunset' in Spanish?", "Puesta de sol", "Naranja", "Rojo", "Puesta de sol"));
                    allQuestions.add(new Question(0, "What is 'Fuchsia' in Spanish?", "Fucsia", "Rosa", "Morado", "Fucsia"));
                    allQuestions.add(new Question(0, "What is 'Rose' in Spanish?", "Rosa", "Rojo", "Blanco", "Rosa"));
                    allQuestions.add(new Question(0, "What is 'Jade' in Spanish?", "Jade", "Verde", "Amarillo", "Jade"));
                    allQuestions.add(new Question(0, "What is 'Onyx' in Spanish?", "Ónice", "Negro", "Gris", "Ónice"));
                    allQuestions.add(new Question(0, "What is 'Chartreuse' in Spanish?", "Chartreuse", "Verde", "Amarillo", "Chartreuse"));
                    allQuestions.add(new Question(0, "What is 'Wisteria' in Spanish?", "Glicinia", "Violeta", "Azul", "Glicinia"));
                    allQuestions.add(new Question(0, "What is 'Auburn' in Spanish?", "Castaño rojizo", "Rojo", "Marrón", "Castaño rojizo"));
                    allQuestions.add(new Question(0, "What is 'Khaki' in Spanish?", "Caqui", "Verde", "Beige", "Caqui"));
                    allQuestions.add(new Question(0, "What is 'Chocolate' in Spanish?", "Chocolate", "Marrón", "Negro", "Chocolate"));
                    allQuestions.add(new Question(0, "What is 'Honey' in Spanish?", "Miel", "Amarillo", "Dorado", "Miel"));
                    allQuestions.add(new Question(0, "What is 'Amber' in Spanish?", "Ámbar", "Naranja", "Amarillo", "Ámbar"));
                    allQuestions.add(new Question(0, "What is 'Tangerine' in Spanish?", "Mandarina", "Naranja", "Amarillo", "Mandarina"));
                    allQuestions.add(new Question(0, "What is 'Periwinkle' in Spanish?", "Periwinkle", "Azul", "Violeta", "Periwinkle"));
                    allQuestions.add(new Question(0, "What is 'Lilac' in Spanish?", "Lila", "Rosa", "Violeta", "Lila"));
                    allQuestions.add(new Question(0, "What is 'Pewter' in Spanish?", "Peltre", "Gris", "Plata", "Peltre"));
                    allQuestions.add(new Question(0, "What is 'Pine' in Spanish?", "Pino", "Verde", "Marrón", "Pino"));
                    allQuestions.add(new Question(0, "What is 'Slate' in Spanish?", "Pizarra", "Gris", "Azul", "Pizarra"));
                    allQuestions.add(new Question(0, "What is 'Canary' in Spanish?", "Canario", "Amarillo", "Rojo", "Canario"));
                    allQuestions.add(new Question(0, "What is 'Slate gray' in Spanish?", "Gris pizarra", "Gris", "Negro", "Gris pizarra"));
                    allQuestions.add(new Question(0, "What is 'Steel blue' in Spanish?", "Azul acero", "Azul", "Gris", "Azul acero"));


                    allQuestions.add(new Question(0, "What is 'Where is the nearest hospital?' in Spanish?", "¿Dónde está el hospital más cercano?", "¿Dónde está la estación de tren?", "¿Cómo llego al aeropuerto?", "¿Dónde está el hospital más cercano?"));
                    allQuestions.add(new Question(0, "What is 'How do I get to the train station?' in Spanish?", "¿Cómo llego a la estación de tren?", "¿Dónde está el hospital más cercano?", "¿Dónde está la estación de autobuses?", "¿Cómo llego a la estación de tren?"));
                    allQuestions.add(new Question(0, "What is 'Is this the way to the airport?' in Spanish?", "¿Es este el camino al aeropuerto?", "¿Cómo llego a la estación de tren?", "¿Dónde está el hospital más cercano?", "¿Es este el camino al aeropuerto?"));
                    allQuestions.add(new Question(0, "What is 'How much does it cost to go to the airport?' in Spanish?", "¿Cuánto cuesta ir al aeropuerto?", "¿Cuánto cuesta el tren?", "¿Dónde está la estación de tren?", "¿Cuánto cuesta ir al aeropuerto?"));
                    allQuestions.add(new Question(0, "What is 'Where is the nearest bus stop?' in Spanish?", "¿Dónde está la parada de autobús más cercana?", "¿Dónde está la estación de tren?", "¿Cómo llego al restaurante?", "¿Dónde está la parada de autobús más cercana?"));
                    allQuestions.add(new Question(0, "What is 'Can you show me the way to the restaurant?' in Spanish?", "¿Puedes mostrarme el camino al restaurante?", "¿Dónde está el hospital más cercano?", "¿Cómo llego a la estación de tren?", "¿Puedes mostrarme el camino al restaurante?"));
                    allQuestions.add(new Question(0, "What is 'Is there a pharmacy nearby?' in Spanish?", "¿Hay una farmacia cerca?", "¿Dónde está la parada de autobús más cercana?", "¿Dónde está el aeropuerto?", "¿Hay una farmacia cerca?"));
                    allQuestions.add(new Question(0, "What is 'How do I get to the nearest subway station?' in Spanish?", "¿Cómo llego a la estación de metro más cercana?", "¿Dónde está la estación de tren?", "¿Dónde está el hospital más cercano?", "¿Cómo llego a la estación de metro más cercana?"));
                    allQuestions.add(new Question(0, "What is 'Where can I buy tickets for the bus?' in Spanish?", "¿Dónde puedo comprar boletos para el autobús?", "¿Dónde está la estación de tren?", "¿Dónde está el aeropuerto?", "¿Dónde puedo comprar boletos para el autobús?"));


                    allQuestions.add(new Question(0, "What is 'Restaurant' in Spanish?", "Restaurante", "Menú", "Mesa para dos", "Restaurante"));
                    allQuestions.add(new Question(0, "What is 'Menu' in Spanish?", "Menú", "Restaurante", "Cuenta, por favor", "Menú"));
                    allQuestions.add(new Question(0, "Translate this: 'Table for two' into Spanish:", "Mesa para dos", "¿Qué me recomienda?", "Agua", "Mesa para dos"));
                    allQuestions.add(new Question(0, "Translate this: 'Bill, please' into Spanish:", "Cuenta, por favor", "¿Puedo tener el menú?", "Soy vegetariano", "Cuenta, por favor"));
                    allQuestions.add(new Question(0, "What is 'What do you recommend?' in Spanish?", "¿Qué me recomienda?", "¿Tienes Wi-Fi?", "¿Podría tener la cuenta?", "¿Qué me recomienda?"));
                    allQuestions.add(new Question(0, "Translate this: 'Water' into Spanish:", "Agua", "Soy vegetariano", "¿Podría tener el menú?", "Agua"));
                    allQuestions.add(new Question(0, "What is 'Can I have the menu?' in Spanish?", "¿Puedo tener el menú?", "¿Tienes Wi-Fi?", "Soy vegetariano", "¿Puedo tener el menú?"));
                    allQuestions.add(new Question(0, "What is 'I’m vegetarian' in Spanish?", "Soy vegetariano", "¿Tienes Wi-Fi?", "¿Podría tener la cuenta?", "Soy vegetariano"));
                    allQuestions.add(new Question(0, "Translate this: 'Do you have Wi-Fi?' into Spanish:", "¿Tienes Wi-Fi?", "¿Podría tener la cuenta?", "¿Puedo tener el menú?", "¿Tienes Wi-Fi?"));
                    allQuestions.add(new Question(0, "What is 'Could I have the check?' in Spanish?", "¿Podría tener la cuenta?", "Soy alérgico a los frutos secos", "¿Tienes una opción vegetariana?", "¿Podría tener la cuenta?"));
                    allQuestions.add(new Question(0, "What is 'I’m allergic to nuts' in Spanish?", "Soy alérgico a los frutos secos", "¿Podemos dividir la cuenta?", "¿Tienes una opción vegetariana?", "Soy alérgico a los frutos secos"));
                    allQuestions.add(new Question(0, "Translate this: 'Can we get the bill separately?' into Spanish:", "¿Podemos dividir la cuenta?", "¿Tienes una opción vegetariana?", "Me gustaría pedir", "¿Podemos dividir la cuenta?"));
                    allQuestions.add(new Question(0, "What is 'Do you have a vegetarian option?' in Spanish?", "¿Tienes una opción vegetariana?", "Me gustaría pedir", "¿Qué lleva este plato?", "¿Tienes una opción vegetariana?"));
                    allQuestions.add(new Question(0, "What is 'I’d like to order' in Spanish?", "Me gustaría pedir", "¿Este plato es picante?", "Tomaré lo mismo", "Me gustaría pedir"));
                    allQuestions.add(new Question(0, "What is 'What’s in this dish?' in Spanish?", "¿Qué lleva este plato?", "¿Es picante?", "Tomaré lo mismo", "¿Qué lleva este plato?"));
                    allQuestions.add(new Question(0, "What is 'Is this spicy?' in Spanish?", "¿Es picante?", "Tomaré lo mismo", "¿Tienes postre?", "¿Es picante?"));
                    allQuestions.add(new Question(0, "Translate this: 'I’ll have the same' into Spanish:", "Tomaré lo mismo", "¿Tienes postre?", "¿Puedo llevarlo para llevar?", "Tomaré lo mismo"));
                    allQuestions.add(new Question(0, "What is 'Do you have dessert?' in Spanish?", "¿Tienes postre?", "¿Puedo llevarlo para llevar?", "¿Cuál es el especial de hoy?", "¿Tienes postre?"));
                    allQuestions.add(new Question(0, "Translate this: 'Can I get it to go?' into Spanish:", "¿Puedo llevarlo para llevar?", "¿Cuál es el especial de hoy?", "¿Cuánto cuesta esto?", "¿Puedo llevarlo para llevar?"));
                    allQuestions.add(new Question(0, "What is 'What’s today’s special?' in Spanish?", "¿Cuál es el especial de hoy?", "¿Cuánto cuesta esto?", "Necesito una mesa para cuatro", "¿Cuál es el especial de hoy?"));
                    allQuestions.add(new Question(0, "What is 'How much is this?' in Spanish?", "¿Cuánto cuesta esto?", "Necesito una mesa para cuatro", "¿Puedo tener una copa de vino?", "¿Cuánto cuesta esto?"));
                    allQuestions.add(new Question(0, "What is 'I need a table for four' in Spanish?", "Necesito una mesa para cuatro", "¿Puedo tener una copa de vino?", "¿Qué tienen en la llave?", "Necesito una mesa para cuatro"));
                    allQuestions.add(new Question(0, "What is 'Can I have a glass of wine?' in Spanish?", "¿Puedo tener una copa de vino?", "¿Qué tienen en la llave?", "Solo estoy mirando", "¿Puedo tener una copa de vino?"));
                    allQuestions.add(new Question(0, "Translate this: 'What do you have on tap?' into Spanish:", "¿Qué tienen en la llave?", "Solo estoy mirando", "¿Puedo tenerlo sin cebollas?", "¿Qué tienen en la llave?"));
                    allQuestions.add(new Question(0, "What is 'I’m just looking' in Spanish?", "Solo estoy mirando", "¿Puedo tenerlo sin cebollas?", "Tomaré la sopa", "Solo estoy mirando"));
                    allQuestions.add(new Question(0, "What is 'Can I have it without onions?' in Spanish?", "¿Puedo tenerlo sin cebollas?", "Tomaré la sopa", "¿Es sin gluten?", "¿Puedo tenerlo sin cebollas?"));
                    allQuestions.add(new Question(0, "What is 'I’ll have the soup' in Spanish?", "Tomaré la sopa", "¿Es sin gluten?", "Me gustaría mi carne bien cocida", "Tomaré la sopa"));
                    allQuestions.add(new Question(0, "What is 'Is it gluten-free?' in Spanish?", "¿Es sin gluten?", "Me gustaría mi carne bien cocida", "¿Puedo tener algunas servilletas extra?", "¿Es sin gluten?"));
                    allQuestions.add(new Question(0, "What is 'I’d like my steak well done' in Spanish?", "Me gustaría mi carne bien cocida", "¿Puedo tener algunas servilletas extra?", "¿Puedo tenerlo sin cebollas?", "Me gustaría mi carne bien cocida"));
                    allQuestions.add(new Question(0, "Translate this: 'Can I have some extra napkins?' into Spanish:", "¿Puedo tener algunas servilletas extra?", "¿Puedo tenerlo sin cebollas?", "¿Es sin gluten?", "¿Puedo tener algunas servilletas extra?"));

                    allQuestions.add(new Question(0, "What is 'Hello/Hi' in Spanish?", "Hola", "Buenas tardes", "Buenas noches", "Hola"));
                    allQuestions.add(new Question(0, "What is 'Good Afternoon' in Spanish?", "Buenas tardes", "Buenas noches", "¿Cómo estás?", "Buenas tardes"));
                    allQuestions.add(new Question(0, "What is 'Good Evening' in Spanish?", "Buenas noches", "¿Cómo estás?", "Me siento bien", "Buenas noches"));
                    allQuestions.add(new Question(0, "What is 'Good Night' in Spanish?", "Buenas noches", "¿Cómo estás?", "Hasta luego", "Buenas noches"));
                    allQuestions.add(new Question(0, "What is 'How are you?' in Spanish?", "¿Cómo estás?", "Me siento bien", "¿Qué hora es?", "¿Cómo estás?"));
                    allQuestions.add(new Question(0, "What is 'I'm fine' in Spanish?", "Estoy bien", "No estoy bien", "Bien", "Estoy bien"));
                    allQuestions.add(new Question(0, "What is 'I'm not well' in Spanish?", "No estoy bien", "Estoy bien", "Así así", "No estoy bien"));
                    allQuestions.add(new Question(0, "What is 'Good' in Spanish?", "Bien", "Así así", "Mal", "Bien"));
                    allQuestions.add(new Question(0, "What is 'So so' in Spanish?", "Así así", "Mal", "Genial", "Así así"));
                    allQuestions.add(new Question(0, "What is 'Bad' in Spanish?", "Mal", "Genial", "Estoy bien", "Mal"));
                    allQuestions.add(new Question(0, "What is 'Great!' in Spanish?", "¡Genial!", "No sé", "Gracias", "¡Genial!"));
                    allQuestions.add(new Question(0, "What is 'What's your name?' in Spanish?", "¿Cómo te llamas?", "Me llamo ...", "¿De dónde eres?", "¿Cómo te llamas?"));
                    allQuestions.add(new Question(0, "What is 'My name is ...' in Spanish?", "Me llamo ...", "¿Cómo te llamas?", "Tengo ... años", "Me llamo ..."));
                    allQuestions.add(new Question(0, "What is 'Take care' in Spanish?", "Cuídate", "Buena suerte", "Nos vemos luego", "Cuídate"));
                    allQuestions.add(new Question(0, "What is 'Good luck' in Spanish?", "Buena suerte", "Cuídate", "Hasta mañana", "Buena suerte"));
                    allQuestions.add(new Question(0, "What is 'See you later' in Spanish?", "Nos vemos luego", "Hasta mañana", "Adiós", "Nos vemos luego"));
                    allQuestions.add(new Question(0, "What is 'See you tomorrow' in Spanish?", "Hasta mañana", "Nos vemos luego", "¿Qué hora es?", "Hasta mañana"));
                    allQuestions.add(new Question(0, "What is 'What about you?' in Spanish?", "¿Y tú?", "¿De dónde eres?", "¿Cómo estás?", "¿Y tú?"));
                    allQuestions.add(new Question(0, "What is 'Goodbye' in Spanish?", "Adiós", "Hasta luego", "Nos vemos mañana", "Adiós"));
                    allQuestions.add(new Question(0, "What is 'How old are you?' in Spanish?", "¿Cuántos años tienes?", "Tengo ... años", "¿De dónde eres?", "¿Cuántos años tienes?"));
                    allQuestions.add(new Question(0, "What is 'I'm ... years old' in Spanish?", "Tengo ... años", "¿Cuántos años tienes?", "Tengo hambre", "Tengo ... años"));
                    allQuestions.add(new Question(0, "What is 'Where are you from?' in Spanish?", "¿De dónde eres?", "Soy de ...", "¿Qué hora es?", "¿De dónde eres?"));
                    allQuestions.add(new Question(0, "What is 'I am from ...' in Spanish?", "Soy de ...", "¿De dónde eres?", "¿Cuántos años tienes?", "Soy de ..."));
                    allQuestions.add(new Question(0, "What is 'Can you help me?' in Spanish?", "¿Me puedes ayudar?", "No entiendo", "¿Puedes ayudarme?", "¿Me puedes ayudar?"));
                    allQuestions.add(new Question(0, "What is 'I don't understand' in Spanish?", "No entiendo", "Por favor", "Gracias", "No entiendo"));
                    allQuestions.add(new Question(0, "What is 'Please' in Spanish?", "Por favor", "Gracias", "Disculpa", "Por favor"));
                    allQuestions.add(new Question(0, "What is 'Thank you' in Spanish?", "Gracias", "De nada", "Perdón", "Gracias"));
                    allQuestions.add(new Question(0, "What is 'You're welcome' in Spanish?", "De nada", "Gracias", "Perdón", "De nada"));
                    allQuestions.add(new Question(0, "What is 'Excuse me' in Spanish?", "Perdón", "Disculpa", "Lo siento", "Perdón"));
                    allQuestions.add(new Question(0, "What is 'Sorry' in Spanish?", "Lo siento", "Perdón", "Gracias", "Lo siento"));
                    allQuestions.add(new Question(0, "What is 'What time is it?' in Spanish?", "¿Qué hora es?", "Es ... en punto", "Tengo hambre", "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "What is 'It's ... o'clock' in Spanish?", "Es ... en punto", "¿Qué hora es?", "Tengo hambre", "Es ... en punto"));
                    allQuestions.add(new Question(0, "What is 'I'm hungry' in Spanish?", "Tengo hambre", "Tengo sed", "¿Dónde está el baño?", "Tengo hambre"));
                    allQuestions.add(new Question(0, "What is 'I'm thirsty' in Spanish?", "Tengo sed", "Tengo hambre", "¿Dónde está el baño?", "Tengo sed"));
                    allQuestions.add(new Question(0, "What is 'Where is the bathroom?' in Spanish?", "¿Dónde está el baño?", "¿Dónde está el restaurante?", "¿Dónde está la estación de tren?", "¿Dónde está el baño?"));
                    allQuestions.add(new Question(0, "What is 'How much is this?' in Spanish?", "¿Cuánto cuesta esto?", "¿Dónde está el baño?", "¿Dónde está la estación de tren?", "¿Cuánto cuesta esto?"));
                    allQuestions.add(new Question(0, "What is 'I like it' in Spanish?", "Me gusta", "No me gusta", "Lo entiendo", "Me gusta"));
                    allQuestions.add(new Question(0, "What is 'I don't like it' in Spanish?", "No me gusta", "Me gusta", "No sé", "No me gusta"));
                    allQuestions.add(new Question(0, "What is 'I understand' in Spanish?", "Lo entiendo", "No sé", "No entiendo", "Lo entiendo"));
                    allQuestions.add(new Question(0, "What is 'I don't know' in Spanish?", "No sé", "Lo entiendo", "No me gusta", "No sé"));
                    allQuestions.add(new Question(0, "What is 'What is this?' in Spanish?", "¿Qué es esto?", "¿Qué hora es?", "¿Cómo estás?", "¿Qué es esto?"));

                    allQuestions.add(new Question(0, "How do you say 'One' in Spanish?", "Uno", "Dos", "Tres", "Uno"));
                    allQuestions.add(new Question(0, "How do you say 'Two' in Spanish?", "Dos", "Tres", "Cuatro", "Dos"));
                    allQuestions.add(new Question(0, "How do you say 'Three' in Spanish?", "Tres", "Cuatro", "Cinco", "Tres"));
                    allQuestions.add(new Question(0, "How do you say 'Four' in Spanish?", "Cuatro", "Cinco", "Seis", "Cuatro"));
                    allQuestions.add(new Question(0, "How do you say 'Five' in Spanish?", "Cinco", "Seis", "Siete", "Cinco"));
                    allQuestions.add(new Question(0, "How do you say 'Six' in Spanish?", "Seis", "Siete", "Ocho", "Seis"));
                    allQuestions.add(new Question(0, "How do you say 'Seven' in Spanish?", "Siete", "Ocho", "Nueve", "Siete"));
                    allQuestions.add(new Question(0, "How do you say 'Eight' in Spanish?", "Ocho", "Nueve", "Diez", "Ocho"));
                    allQuestions.add(new Question(0, "How do you say 'Nine' in Spanish?", "Nueve", "Diez", "Veinte", "Nueve"));
                    allQuestions.add(new Question(0, "How do you say 'Ten' in Spanish?", "Diez", "Veinte", "Treinta", "Diez"));
                    allQuestions.add(new Question(0, "How do you say 'Twenty' in Spanish?", "Veinte", "Treinta", "Cuarenta", "Veinte"));
                    allQuestions.add(new Question(0, "How do you say 'Thirty' in Spanish?", "Treinta", "Cuarenta", "Cincuenta", "Treinta"));
                    allQuestions.add(new Question(0, "How do you say 'Forty' in Spanish?", "Cuarenta", "Cincuenta", "Sesenta", "Cuarenta"));
                    allQuestions.add(new Question(0, "How do you say 'Fifty' in Spanish?", "Cincuenta", "Sesenta", "Setenta", "Cincuenta"));
                    allQuestions.add(new Question(0, "How do you say 'Sixty' in Spanish?", "Sesenta", "Setenta", "Ochenta", "Sesenta"));
                    allQuestions.add(new Question(0, "How do you say 'Seventy' in Spanish?", "Setenta", "Ochenta", "Noventa", "Setenta"));
                    allQuestions.add(new Question(0, "How do you say 'Eighty' in Spanish?", "Ochenta", "Noventa", "Cien", "Ochenta"));
                    allQuestions.add(new Question(0, "How do you say 'Ninety' in Spanish?", "Noventa", "Cien", "Doscientos", "Noventa"));
                    allQuestions.add(new Question(0, "How do you say 'Hundred' in Spanish?", "Cien", "Doscientos", "Trescientos", "Cien"));
                    allQuestions.add(new Question(0, "How do you say 'Two Hundred' in Spanish?", "Doscientos", "Trescientos", "Cuatrocientos", "Doscientos"));
                    allQuestions.add(new Question(0, "How do you say 'Three Hundred' in Spanish?", "Trescientos", "Cuatrocientos", "Quinientos", "Trescientos"));
                    allQuestions.add(new Question(0, "How do you say 'Four Hundred' in Spanish?", "Cuatrocientos", "Quinientos", "Seiscientos", "Cuatrocientos"));
                    allQuestions.add(new Question(0, "How do you say 'Five Hundred' in Spanish?", "Quinientos", "Seiscientos", "Setecientos", "Quinientos"));
                    allQuestions.add(new Question(0, "How do you say 'Six Hundred' in Spanish?", "Seiscientos", "Setecientos", "Ochocientos", "Seiscientos"));
                    allQuestions.add(new Question(0, "How do you say 'Seven Hundred' in Spanish?", "Setecientos", "Ochocientos", "Novecientos", "Setecientos"));
                    allQuestions.add(new Question(0, "How do you say 'Eight Hundred' in Spanish?", "Ochocientos", "Novecientos", "Mil", "Ochocientos"));
                    allQuestions.add(new Question(0, "How do you say 'Nine Hundred' in Spanish?", "Novecientos", "Mil", "Dos mil", "Novecientos"));
                    allQuestions.add(new Question(0, "How do you say 'Thousand' in Spanish?", "Mil", "Dos mil", "Tres mil", "Mil"));

                    allQuestions.add(new Question(0, "What time is it?", "¿Qué hora es?", "¿Qué fecha es?", "¿Qué día es?", "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "What is the date today?", "¿Cuál es la fecha de hoy?", "¿Qué día es hoy?", "¿Qué hora es?", "¿Qué fecha es hoy?"));
                    allQuestions.add(new Question(0, "Tomorrow's date?", "¿Cuál es la fecha de mañana?", "¿Qué día es mañana?", "¿Qué hora es?", "¿Qué fecha es mañana?"));
                    allQuestions.add(new Question(0, "What time do we meet?", "¿A qué hora nos encontramos?", "¿A qué hora nos vemos?", "¿Cuándo nos encontramos?", "¿A qué hora nos reunimos?"));
                    allQuestions.add(new Question(0, "What day is it?", "¿Qué día es hoy?", "¿Qué día es?", "¿Qué fecha es?", "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "See you at 3 PM", "Nos vemos a las 3 PM", "Nos vemos a las 3 de la tarde", "Te veo a las 3 PM", "Nos vemos a las 3"));
                    allQuestions.add(new Question(0, "It's midnight", "Es medianoche", "Es la medianoche", "Es la 12 de la noche", "Es la medianoche"));
                    allQuestions.add(new Question(0, "The meeting is at 10 AM", "La reunión es a las 10 AM", "La reunión es a las 10 de la mañana", "La junta es a las 10 AM", "La reunión es a las 10"));
                    allQuestions.add(new Question(0, "It's 12 o'clock", "Es mediodía", "Son las 12 en punto", "Son las 12", "Es la 1 del mediodía"));
                    allQuestions.add(new Question(0, "What time does the train leave?", "¿A qué hora sale el tren?", "¿A qué hora parte el tren?", "¿A qué hora se va el tren?", "¿Qué hora parte el tren?"));
                    allQuestions.add(new Question(0, "I’ll arrive at 6 PM", "Llegaré a las 6 PM", "Estaré ahí a las 6 PM", "Llego a las 6 de la tarde", "Llegaré a las 6"));
                    allQuestions.add(new Question(0, "It's already late", "Ya es tarde", "Ya está tarde", "Es tarde ya", "Ya se hizo tarde"));
                    allQuestions.add(new Question(0, "It's early", "Es temprano", "Es muy temprano", "Está temprano", "Es muy pronto"));
                    allQuestions.add(new Question(0, "The event starts at 7 PM", "El evento comienza a las 7 PM", "El evento empieza a las 7 de la tarde", "La actividad empieza a las 7 PM", "El evento comienza a las 7"));
                    allQuestions.add(new Question(0, "See you in the morning", "Nos vemos en la mañana", "Nos vemos por la mañana", "Nos vemos mañana", "Te veo por la mañana"));
                    allQuestions.add(new Question(0, "It’s past noon", "Ya pasó el mediodía", "Es después del mediodía", "Es ya tarde", "Ya es tarde para el mediodía"));
                    allQuestions.add(new Question(0, "The flight departs at 2 PM", "El vuelo sale a las 2 PM", "El vuelo parte a las 2 de la tarde", "El vuelo sale a las 2", "El avión sale a las 2"));
                    allQuestions.add(new Question(0, "I woke up at 8 AM", "Me desperté a las 8 AM", "Desperté a las 8 de la mañana", "Me levanté a las 8 AM", "Desperté a las 8"));
                    allQuestions.add(new Question(0, "It's the 1st of May", "Es el 1 de mayo", "Hoy es 1 de mayo", "Es el primer día de mayo", "Es el 1ro de mayo"));
                    allQuestions.add(new Question(0, "Next Friday", "El próximo viernes", "El viernes que viene", "El viernes próximo", "El viernes que sigue"));
                    allQuestions.add(new Question(0, "It's 3 minutes past 5", "Son 3 minutos pasados de las 5", "Son las 5 con 3 minutos", "Son las 5 y 3 minutos", "Pasaron 3 minutos de las 5"));
                    allQuestions.add(new Question(0, "What time does the store close?", "¿A qué hora cierra la tienda?", "¿A qué hora cierra el comercio?", "¿Qué hora cierra la tienda?", "¿A qué hora cierran las tiendas?"));



                    Collections.shuffle(allQuestions);
                    questions = new ArrayList<>(allQuestions.subList(0, 15));
                    break;

                case "mediate":
                    allQuestions.add(new Question(R.drawable.plate, "What is this called in Spanish?", null, null, null, "Plato"));
                    allQuestions.add(new Question(R.drawable.fork, "What is this called in Spanish?", null, null, null, "Tenedor"));
                    allQuestions.add(new Question(R.drawable.glass, "What is this called in Spanish?", null, null, null, "Vaso"));
                    allQuestions.add(new Question(R.drawable.cup, "What is this called in Spanish?", null, null, null, "Taza"));
                    allQuestions.add(new Question(R.drawable.spoon, "What is this called in Spanish?", null, null, null, "Cuchara"));
                    allQuestions.add(new Question(R.drawable.bottle, "What is this called in Spanish?", null, null, null, "Botella"));
                    allQuestions.add(new Question(R.drawable.book, "What is this called in Spanish?", null, null, null, "Libro"));
                    allQuestions.add(new Question(R.drawable.chair, "What is this called in Spanish?", null, null, null, "Silla"));
                    allQuestions.add(new Question(R.drawable.table, "What is this called in Spanish?", null, null, null, "Mesa"));
                    allQuestions.add(new Question(R.drawable.phone, "What is this called in Spanish?", null, null, null, "Móvil"));
                    allQuestions.add(new Question(R.drawable.laptop, "What is this called in Spanish?", null, null, null, "Portátil"));
                    allQuestions.add(new Question(R.drawable.desk, "What is this called in Spanish?", null, null, null, "Escritorio"));
                    allQuestions.add(new Question(R.drawable.computer, "What is this called in Spanish?", null, null, null, "Computadora"));
                    allQuestions.add(new Question(R.drawable.window, "What is this called in Spanish?", null, null, null, "Ventana"));
                    allQuestions.add(new Question(R.drawable.door, "What is this called in Spanish?", null, null, null, "Puerta"));
                    allQuestions.add(new Question(R.drawable.tv, "What is this called in Spanish?", null, null, null, "Televisión"));
                    allQuestions.add(new Question(R.drawable.car, "What is this called in Spanish?", null, null, null, "Coche"));
                    allQuestions.add(new Question(R.drawable.bike, "What is this called in Spanish?", null, null, null, "Bicicleta"));
                    allQuestions.add(new Question(R.drawable.clock, "What is this called in Spanish?", null, null, null, "Reloj"));
                    allQuestions.add(new Question(R.drawable.watch, "What is this called in Spanish?", null, null, null, "Reloj de pulsera"));
                    allQuestions.add(new Question(R.drawable.bag, "What is this called in Spanish?", null, null, null, "Bolsa"));
                    allQuestions.add(new Question(R.drawable.hat, "What is this called in Spanish?", null, null, null, "Sombrero"));
                    allQuestions.add(new Question(R.drawable.shirt, "What is this called in Spanish?", null, null, null, "Camiseta"));
                    allQuestions.add(new Question(R.drawable.pants, "What is this called in Spanish?", null, null, null, "Pantalones"));
                    allQuestions.add(new Question(R.drawable.shoes, "What is this called in Spanish?", null, null, null, "Zapatos"));
                    allQuestions.add(new Question(R.drawable.jacket, "What is this called in Spanish?", null, null, null, "Chaqueta"));
                    allQuestions.add(new Question(R.drawable.glove, "What is this called in Spanish?", null, null, null, "Guante"));
                    allQuestions.add(new Question(R.drawable.scarf, "What is this called in Spanish?", null, null, null, "Bufanda"));
                    allQuestions.add(new Question(R.drawable.shot, "What is this called in Spanish?", null, null, null, "Pantalones cortos"));
                    allQuestions.add(new Question(R.drawable.glasses, "What is this called in Spanish?", null, null, null, "Gafas"));
                    allQuestions.add(new Question(R.drawable.key, "What is this called in Spanish?", null, null, null, "Llave"));
                    allQuestions.add(new Question(R.drawable.comb, "What is this called in Spanish?", null, null, null, "Peina"));
                    allQuestions.add(new Question(R.drawable.toothbrush, "What is this called in Spanish?", null, null, null, "Cepillo de dientes"));
                    allQuestions.add(new Question(R.drawable.toothpaste, "What is this called in Spanish?", null, null, null, "Pasta de dientes"));
                    allQuestions.add(new Question(R.drawable.socks, "What is this called in Spanish?", null, null, null, "Calcetines"));
                    allQuestions.add(new Question(R.drawable.blanket, "What is this called in Spanish?", null, null, null, "Manta"));
                    allQuestions.add(new Question(R.drawable.towel, "What is this called in Spanish?", null, null, null, "Toalla"));
                    allQuestions.add(new Question(R.drawable.pillow, "What is this called in Spanish?", null, null, null, "Almohada"));
                    allQuestions.add(new Question(R.drawable.broom, "What is this called in Spanish?", null, null, null, "Escoba"));

                    allQuestions.add(new Question(0, "Translate 'To work' into Spanish:", null, null, null, "Para trabajar"));
                    allQuestions.add(new Question(0, "Translate 'No' into Spanish:", null, null, null, "No"));
                    allQuestions.add(new Question(0, "Translate 'Do you understand?' into Spanish:", null, null, null, "¿Entiendes?"));
                    allQuestions.add(new Question(0, "Translate 'I don't understand' into Spanish:", null, null, null, "No entiendo"));
                    allQuestions.add(new Question(0, "Translate 'Where?' into Spanish:", null, null, null, "¿Dónde?"));
                    allQuestions.add(new Question(0, "Translate 'What?' into Spanish:", null, null, null, "¿Qué?"));
                    allQuestions.add(new Question(0, "Translate 'How?' into Spanish:", null, null, null, "¿Cómo?"));
                    allQuestions.add(new Question(0, "Translate 'How much?' into Spanish:", null, null, null, "¿Cuánto cuesta?"));
                    allQuestions.add(new Question(0, "Translate 'When?' into Spanish:", null, null, null, "¿Cuándo?"));
                    allQuestions.add(new Question(0, "Translate 'Who?' into Spanish:", null, null, null, "¿Quién?"));
                    allQuestions.add(new Question(0, "Translate 'Why?' into Spanish:", null, null, null, "¿Por qué?"));
                    allQuestions.add(new Question(0, "Translate 'Thank you' into Spanish:", null, null, null, "Gracias"));
                    allQuestions.add(new Question(0, "Translate 'I'm sorry' into Spanish:", null, null, null, "Lo siento"));
                    allQuestions.add(new Question(0, "Translate 'Congratulations!' into Spanish:", null, null, null, "¡Felicidades!"));
                    allQuestions.add(new Question(0, "Translate 'It's okay' into Spanish:", null, null, null, "Está bien"));
                    allQuestions.add(new Question(0, "Translate 'I don't know' into Spanish:", null, null, null, "No sé"));
                    allQuestions.add(new Question(0, "Translate 'I don't like it' into Spanish:", null, null, null, "No me gusta"));
                    allQuestions.add(new Question(0, "Translate 'I like it' into Spanish:", null, null, null, "Me gusta"));
                    allQuestions.add(new Question(0, "Translate 'You're welcome' into Spanish:", null, null, null, "De nada"));
                    allQuestions.add(new Question(0, "Translate 'I think that...' into Spanish:", null, null, null, "Pienso que"));
                    allQuestions.add(new Question(0, "Translate 'No, thank you' into Spanish:", null, null, null, "No, gracias"));
                    allQuestions.add(new Question(0, "Translate 'Excuse me' into Spanish:", null, null, null, "Disculpe"));
                    allQuestions.add(new Question(0, "Translate 'Take care' into Spanish:", null, null, null, "Cuídate"));
                    allQuestions.add(new Question(0, "Translate 'Don't forget' into Spanish:", null, null, null, "No olvides"));
                    allQuestions.add(new Question(0, "Translate 'How do you pronounce this?' into Spanish:", null, null, null, "¿Cómo se pronuncia esto?"));
                    allQuestions.add(new Question(0, "Translate 'Before' into Spanish:", null, null, null, "Antes"));
                    allQuestions.add(new Question(0, "Translate 'After' into Spanish:", null, null, null, "Después"));
                    allQuestions.add(new Question(0, "Translate 'Wrong' into Spanish:", null, null, null, "Incorrecto"));
                    allQuestions.add(new Question(0, "Translate 'Right' into Spanish:", null, null, null, "Correcto"));
                    allQuestions.add(new Question(0, "Translate 'Until' into Spanish:", null, null, null, "Hasta"));
                    allQuestions.add(new Question(0, "Translate 'Where is the toilet?' into Spanish:", null, null, null, "¿Dónde está el baño?"));
                    allQuestions.add(new Question(0, "Translate 'Do you live here?' into Spanish:", null, null, null, "¿Vives aquí?"));
                    allQuestions.add(new Question(0, "Translate 'Do you like it?' into Spanish:", null, null, null, "¿Te gusta?"));
                    allQuestions.add(new Question(0, "Translate 'I love it' into Spanish:", null, null, null, "Me encanta"));
                    allQuestions.add(new Question(0, "Translate 'On business' into Spanish:", null, null, null, "En negocios"));
                    allQuestions.add(new Question(0, "Translate 'To work' into Spanish:", null, null, null, "Para trabajar"));
                    allQuestions.add(new Question(0, "Translate 'What happened?' into Spanish:", null, null, null, "¿Qué pasó?"));
                    allQuestions.add(new Question(0, "Translate 'Do you need help?' into Spanish:", null, null, null, "¿Necesitas ayuda?"));
                    allQuestions.add(new Question(0, "Translate 'I'm lost' into Spanish:", null, null, null, "Estoy perdido"));
                    allQuestions.add(new Question(0, "Translate 'What time is it?' into Spanish:", null, null, null, "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "Translate 'I want to go there' into Spanish:", null, null, null, "Quiero ir allí"));
                    allQuestions.add(new Question(0, "Translate 'How far is it?' into Spanish:", null, null, null, "¿Qué tan lejos está?"));
                    allQuestions.add(new Question(0, "Translate 'Can I help you?' into Spanish:", null, null, null, "¿Puedo ayudarte?"));
                    allQuestions.add(new Question(0, "Translate 'Do you speak English?' into Spanish:", null, null, null, "¿Hablas inglés?"));
                    allQuestions.add(new Question(0, "Translate 'I need a doctor' into Spanish:", null, null, null, "Necesito un médico"));
                    allQuestions.add(new Question(0, "Translate 'Call the police' into Spanish:", null, null, null, "Llama a la policía"));


                    allQuestions.add(new Question(0, "What is 'Red' in Spanish?", null, null, null, "Rojo"));
                    allQuestions.add(new Question(0, "What is 'Blue' in Spanish?", null, null, null, "Azul"));
                    allQuestions.add(new Question(0, "What is 'Green' in Spanish?", null, null, null, "Verde"));
                    allQuestions.add(new Question(0, "What is 'Yellow' in Spanish?", null, null, null, "Amarillo"));
                    allQuestions.add(new Question(0, "What is 'Black' in Spanish?", null, null, null, "Negro"));
                    allQuestions.add(new Question(0, "What is 'White' in Spanish?", null, null, null, "Blanco"));
                    allQuestions.add(new Question(0, "What is 'Pink' in Spanish?", null, null, null, "Rosa"));
                    allQuestions.add(new Question(0, "What is 'Purple' in Spanish?", null, null, null, "Morado"));
                    allQuestions.add(new Question(0, "What is 'Orange' in Spanish?", null, null, null, "Naranja"));
                    allQuestions.add(new Question(0, "What is 'Brown' in Spanish?", null, null, null, "Marrón"));
                    allQuestions.add(new Question(0, "What is 'Gray' in Spanish?", null, null, null, "Gris"));
                    allQuestions.add(new Question(0, "What is 'Beige' in Spanish?", null, null, null, "Beige"));
                    allQuestions.add(new Question(0, "What is 'Turquoise' in Spanish?", null, null, null, "Turquesa"));
                    allQuestions.add(new Question(0, "What is 'Gold' in Spanish?", null, null, null, "Oro"));
                    allQuestions.add(new Question(0, "What is 'Silver' in Spanish?", null, null, null, "Plata"));
                    allQuestions.add(new Question(0, "What is 'Copper' in Spanish?", null, null, null, "Cobre"));
                    allQuestions.add(new Question(0, "What is 'Bronze' in Spanish?", null, null, null, "Bronce"));
                    allQuestions.add(new Question(0, "What is 'Magenta' in Spanish?", null, null, null, "Magenta"));
                    allQuestions.add(new Question(0, "What is 'Violet' in Spanish?", null, null, null, "Violeta"));
                    allQuestions.add(new Question(0, "What is 'Indigo' in Spanish?", null, null, null, "Índigo"));
                    allQuestions.add(new Question(0, "What is 'Lavender' in Spanish?", null, null, null, "Lavanda"));
                    allQuestions.add(new Question(0, "What is 'Peach' in Spanish?", null, null, null, "Durazno"));
                    allQuestions.add(new Question(0, "What is 'Ivory' in Spanish?", null, null, null, "Marfil"));
                    allQuestions.add(new Question(0, "What is 'Cream' in Spanish?", null, null, null, "Crema"));
                    allQuestions.add(new Question(0, "What is 'Charcoal' in Spanish?", null, null, null, "Carbón"));
                    allQuestions.add(new Question(0, "What is 'Emerald' in Spanish?", null, null, null, "Esmeralda"));
                    allQuestions.add(new Question(0, "What is 'Cyan' in Spanish?", null, null, null, "Cian"));
                    allQuestions.add(new Question(0, "What is 'Mint' in Spanish?", null, null, null, "Menta"));
                    allQuestions.add(new Question(0, "What is 'Lime' in Spanish?", null, null, null, "Lima"));
                    allQuestions.add(new Question(0, "What is 'Plum' in Spanish?", null, null, null, "Ciruela"));
                    allQuestions.add(new Question(0, "What is 'Coral' in Spanish?", null, null, null, "Coral"));
                    allQuestions.add(new Question(0, "What is 'Tan' in Spanish?", null, null, null, "Canela"));
                    allQuestions.add(new Question(0, "What is 'Rust' in Spanish?", null, null, null, "Óxido"));
                    allQuestions.add(new Question(0, "What is 'Sapphire' in Spanish?", null, null, null, "Zafiro"));
                    allQuestions.add(new Question(0, "What is 'Scarlet' in Spanish?", null, null, null, "Escarlata"));
                    allQuestions.add(new Question(0, "What is 'Burgundy' in Spanish?", null, null, null, "Burdeos"));
                    allQuestions.add(new Question(0, "What is 'Azure' in Spanish?", null, null, null, "Azul celeste"));
                    allQuestions.add(new Question(0, "What is 'Seafoam' in Spanish?", null, null, null, "Espuma de mar"));
                    allQuestions.add(new Question(0, "What is 'Sunset' in Spanish?", null, null, null, "Puesta de sol"));
                    allQuestions.add(new Question(0, "What is 'Fuchsia' in Spanish?", null, null, null, "Fucsia"));
                    allQuestions.add(new Question(0, "What is 'Rose' in Spanish?", null, null, null, "Rosa"));
                    allQuestions.add(new Question(0, "What is 'Jade' in Spanish?", null, null, null, "Jade"));
                    allQuestions.add(new Question(0, "What is 'Onyx' in Spanish?", null, null, null, "Ónice"));
                    allQuestions.add(new Question(0, "What is 'Chartreuse' in Spanish?", null, null, null, "Chartreuse"));
                    allQuestions.add(new Question(0, "What is 'Wisteria' in Spanish?", null, null, null, "Glicinia"));
                    allQuestions.add(new Question(0, "What is 'Auburn' in Spanish?", null, null, null, "Castaño rojizo"));
                    allQuestions.add(new Question(0, "What is 'Khaki' in Spanish?", null, null, null, "Caqui"));
                    allQuestions.add(new Question(0, "What is 'Chocolate' in Spanish?", null, null, null, "Chocolate"));
                    allQuestions.add(new Question(0, "What is 'Honey' in Spanish?", null, null, null, "Miel"));
                    allQuestions.add(new Question(0, "What is 'Amber' in Spanish?", null, null, null, "Ámbar"));
                    allQuestions.add(new Question(0, "What is 'Tangerine' in Spanish?", null, null, null, "Mandarina"));
                    allQuestions.add(new Question(0, "What is 'Periwinkle' in Spanish?", null, null, null, "Periwinkle"));
                    allQuestions.add(new Question(0, "What is 'Lilac' in Spanish?", null, null, null, "Lila"));
                    allQuestions.add(new Question(0, "What is 'Pewter' in Spanish?", null, null, null, "Peltre"));
                    allQuestions.add(new Question(0, "What is 'Pine' in Spanish?", null, null, null, "Pino"));
                    allQuestions.add(new Question(0, "What is 'Slate' in Spanish?", null, null, null, "Pizarra"));
                    allQuestions.add(new Question(0, "What is 'Canary' in Spanish?", null, null, null, "Canario"));
                    allQuestions.add(new Question(0, "What is 'Slate gray' in Spanish?", null, null, null, "Gris pizarra"));
                    allQuestions.add(new Question(0, "What is 'Steel blue' in Spanish?", null, null, null, "Azul acero"));


                    allQuestions.add(new Question(0, "What is 'Where is the nearest hospital?' in Spanish?", null, null, null, "¿Dónde está el hospital más cercano?"));
                    allQuestions.add(new Question(0, "What is 'How do I get to the train station?' in Spanish?", null, null, null, "¿Cómo llego a la estación de tren?"));
                    allQuestions.add(new Question(0, "What is 'Is this the way to the airport?' in Spanish?", null, null, null, "¿Es este el camino al aeropuerto?"));
                    allQuestions.add(new Question(0, "What is 'How much does it cost to go to the airport?' in Spanish?", null, null, null, "¿Cuánto cuesta ir al aeropuerto?"));
                    allQuestions.add(new Question(0, "What is 'Where is the nearest bus stop?' in Spanish?", null, null, null, "¿Dónde está la parada de autobús más cercana?"));
                    allQuestions.add(new Question(0, "What is 'Can you show me the way to the restaurant?' in Spanish?", null, null, null, "¿Puedes mostrarme el camino al restaurante?"));
                    allQuestions.add(new Question(0, "What is 'Is there a pharmacy nearby?' in Spanish?", null, null, null, "¿Hay una farmacia cerca?"));
                    allQuestions.add(new Question(0, "What is 'How do I get to the nearest subway station?' in Spanish?", null, null, null, "¿Cómo llego a la estación de metro más cercana?"));
                    allQuestions.add(new Question(0, "What is 'Where can I buy tickets for the bus?' in Spanish?", null, null, null, "¿Dónde puedo comprar boletos para el autobús?"));

                    allQuestions.add(new Question(0, "What is 'Restaurant' in Spanish?", null, null, null, "Restaurante"));
                    allQuestions.add(new Question(0, "What is 'Menu' in Spanish?", null, null, null, "Menú"));
                    allQuestions.add(new Question(0, "Translate this: 'Table for two' into Spanish:", null, null, null, "Mesa para dos"));
                    allQuestions.add(new Question(0, "Translate this: 'Bill, please' into Spanish:", null, null, null, "Cuenta, por favor"));
                    allQuestions.add(new Question(0, "What is 'What do you recommend?' in Spanish?", null, null, null, "¿Qué me recomienda?"));
                    allQuestions.add(new Question(0, "Translate this: 'Water' into Spanish:", null, null, null, "Agua"));
                    allQuestions.add(new Question(0, "What is 'Can I have the menu?' in Spanish?", null, null, null, "¿Puedo tener el menú?"));
                    allQuestions.add(new Question(0, "What is 'I’m vegetarian' in Spanish?", null, null, null, "Soy vegetariano"));
                    allQuestions.add(new Question(0, "Translate this: 'Do you have Wi-Fi?' into Spanish:", null, null, null, "¿Tienes Wi-Fi?"));
                    allQuestions.add(new Question(0, "What is 'Could I have the check?' in Spanish?", null, null, null, "¿Podría tener la cuenta?"));
                    allQuestions.add(new Question(0, "What is 'I’m allergic to nuts' in Spanish?", null, null, null, "Soy alérgico a los frutos secos"));
                    allQuestions.add(new Question(0, "Translate this: 'Can we get the bill separately?' into Spanish:", null, null, null, "¿Podemos dividir la cuenta?"));
                    allQuestions.add(new Question(0, "What is 'Do you have a vegetarian option?' in Spanish?", null, null, null, "¿Tienes una opción vegetariana?"));
                    allQuestions.add(new Question(0, "What is 'I’d like to order' in Spanish?", null, null, null, "Me gustaría pedir"));
                    allQuestions.add(new Question(0, "What is 'What’s in this dish?' in Spanish?", null, null, null, "¿Qué lleva este plato?"));
                    allQuestions.add(new Question(0, "What is 'Is this spicy?' in Spanish?", null, null, null, "¿Es picante?"));
                    allQuestions.add(new Question(0, "Translate this: 'I’ll have the same' into Spanish:", null, null, null, "Tomaré lo mismo"));
                    allQuestions.add(new Question(0, "What is 'Do you have dessert?' in Spanish?", null, null, null, "¿Tienes postre?"));
                    allQuestions.add(new Question(0, "Translate this: 'Can I get it to go?' into Spanish:", null, null, null, "¿Puedo llevarlo para llevar?"));
                    allQuestions.add(new Question(0, "What is 'What’s today’s special?' in Spanish?", null, null, null, "¿Cuál es el especial de hoy?"));
                    allQuestions.add(new Question(0, "What is 'How much is this?' in Spanish?", null, null, null, "¿Cuánto cuesta esto?"));
                    allQuestions.add(new Question(0, "What is 'I need a table for four' in Spanish?", null, null, null, "Necesito una mesa para cuatro"));
                    allQuestions.add(new Question(0, "What is 'Can I have a glass of wine?' in Spanish?", null, null, null, "¿Puedo tener una copa de vino?"));
                    allQuestions.add(new Question(0, "Translate this: 'What do you have on tap?' into Spanish:", null, null, null, "¿Qué tienen en la llave?"));
                    allQuestions.add(new Question(0, "What is 'I’m just looking' in Spanish?", null, null, null, "Solo estoy mirando"));
                    allQuestions.add(new Question(0, "What is 'Can I have it without onions?' in Spanish?", null, null, null, "¿Puedo tenerlo sin cebollas?"));
                    allQuestions.add(new Question(0, "What is 'I’ll have the soup' in Spanish?", null, null, null, "Tomaré la sopa"));
                    allQuestions.add(new Question(0, "What is 'Is it gluten-free?' in Spanish?", null, null, null, "¿Es sin gluten?"));
                    allQuestions.add(new Question(0, "What is 'I’d like my steak well done' in Spanish?", null, null, null, "Me gustaría mi carne bien cocida"));
                    allQuestions.add(new Question(0, "Translate this: 'Can I have some extra napkins?' into Spanish:", null, null, null, "¿Puedo tener algunas servilletas extra?"));


                    allQuestions.add(new Question(0, "How do you say 'One' in Spanish?", null, null, null, "Uno"));
                    allQuestions.add(new Question(0, "How do you say 'Two' in Spanish?", null, null, null, "Dos"));
                    allQuestions.add(new Question(0, "How do you say 'Three' in Spanish?", null, null, null, "Tres"));
                    allQuestions.add(new Question(0, "How do you say 'Four' in Spanish?", null, null, null, "Cuatro"));
                    allQuestions.add(new Question(0, "How do you say 'Five' in Spanish?", null, null, null, "Cinco"));
                    allQuestions.add(new Question(0, "How do you say 'Six' in Spanish?", null, null, null, "Seis"));
                    allQuestions.add(new Question(0, "How do you say 'Seven' in Spanish?", null, null, null, "Siete"));
                    allQuestions.add(new Question(0, "How do you say 'Eight' in Spanish?", null, null, null, "Ocho"));
                    allQuestions.add(new Question(0, "How do you say 'Nine' in Spanish?", null, null, null, "Nueve"));
                    allQuestions.add(new Question(0, "How do you say 'Ten' in Spanish?", null, null, null, "Diez"));
                    allQuestions.add(new Question(0, "How do you say 'Twenty' in Spanish?", null, null, null, "Veinte"));
                    allQuestions.add(new Question(0, "How do you say 'Thirty' in Spanish?", null, null, null, "Treinta"));
                    allQuestions.add(new Question(0, "How do you say 'Forty' in Spanish?", null, null, null, "Cuarenta"));
                    allQuestions.add(new Question(0, "How do you say 'Fifty' in Spanish?", null, null, null, "Cincuenta"));
                    allQuestions.add(new Question(0, "How do you say 'Sixty' in Spanish?", null, null, null, "Sesenta"));
                    allQuestions.add(new Question(0, "How do you say 'Seventy' in Spanish?", null, null, null, "Setenta"));
                    allQuestions.add(new Question(0, "How do you say 'Eighty' in Spanish?", null, null, null, "Ochenta"));
                    allQuestions.add(new Question(0, "How do you say 'Ninety' in Spanish?", null, null, null, "Noventa"));
                    allQuestions.add(new Question(0, "How do you say 'Hundred' in Spanish?", null, null, null, "Cien"));
                    allQuestions.add(new Question(0, "How do you say 'Two Hundred' in Spanish?", null, null, null, "Doscientos"));
                    allQuestions.add(new Question(0, "How do you say 'Three Hundred' in Spanish?", null, null, null, "Trescientos"));
                    allQuestions.add(new Question(0, "How do you say 'Four Hundred' in Spanish?", null, null, null, "Cuatrocientos"));
                    allQuestions.add(new Question(0, "How do you say 'Five Hundred' in Spanish?", null, null, null, "Quinientos"));
                    allQuestions.add(new Question(0, "How do you say 'Six Hundred' in Spanish?", null, null, null, "Seiscientos"));
                    allQuestions.add(new Question(0, "How do you say 'Seven Hundred' in Spanish?", null, null, null, "Setecientos"));
                    allQuestions.add(new Question(0, "How do you say 'Eight Hundred' in Spanish?", null, null, null, "Ochocientos"));
                    allQuestions.add(new Question(0, "How do you say 'Nine Hundred' in Spanish?", null, null, null, "Novecientos"));
                    allQuestions.add(new Question(0, "How do you say 'Thousand' in Spanish?", null, null, null, "Mil"));


                    allQuestions.add(new Question(0, "What is 'Do you have any organic products?' in Spanish?", null, null, null, "¿Tienes productos orgánicos?"));
                    allQuestions.add(new Question(0, "What is 'Can I pay with a credit card?' in Spanish?", null, null, null, "¿Puedo pagar con tarjeta de crédito?"));
                    allQuestions.add(new Question(0, "What is 'Where are the bathroom products?' in Spanish?", null, null, null, "¿Dónde están los productos de baño?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any vegan options?' in Spanish?", null, null, null, "¿Tienes opciones veganas?"));
                    allQuestions.add(new Question(0, "What is 'How much is this item?' in Spanish?", null, null, null, "¿Cuánto cuesta este artículo?"));
                    allQuestions.add(new Question(0, "What is 'Where can I find the milk?' in Spanish?", null, null, null, "¿Dónde puedo encontrar la leche?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any gluten-free snacks?' in Spanish?", null, null, null, "¿Tienes bocadillos sin gluten?"));
                    allQuestions.add(new Question(0, "What is 'Can I try this on?' in Spanish?", null, null, null, "¿Puedo probarme esto?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any discounted items?' in Spanish?", null, null, null, "¿Tienes artículos con descuento?"));
                    allQuestions.add(new Question(0, "What is 'Are these items on sale?' in Spanish?", null, null, null, "¿Están estos artículos en oferta?"));
                    allQuestions.add(new Question(0, "What is 'Where can I find the shoes?' in Spanish?", null, null, null, "¿Dónde puedo encontrar los zapatos?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any sugar-free options?' in Spanish?", null, null, null, "¿Tienes opciones sin azúcar?"));
                    allQuestions.add(new Question(0, "What is 'Is this item available in other colors?' in Spanish?", null, null, null, "¿Este artículo está disponible en otros colores?"));
                    allQuestions.add(new Question(0, "What is 'Can I get a refund?' in Spanish?", null, null, null, "¿Puedo obtener un reembolso?"));
                    allQuestions.add(new Question(0, "What is 'Where is the customer service desk?' in Spanish?", null, null, null, "¿Dónde está el mostrador de servicio al cliente?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any seasonal items?' in Spanish?", null, null, null, "¿Tienes artículos de temporada?"));
                    allQuestions.add(new Question(0, "What is 'Is this item in stock?' in Spanish?", null, null, null, "¿Este artículo está en stock?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any clothing for kids?' in Spanish?", null, null, null, "¿Tienes ropa para niños?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any large sizes?' in Spanish?", null, null, null, "¿Tienes tallas grandes?"));
                    allQuestions.add(new Question(0, "What is 'Is this a new product?' in Spanish?", null, null, null, "¿Este es un producto nuevo?"));
                    allQuestions.add(new Question(0, "What is 'Do you offer free shipping?' in Spanish?", null, null, null, "¿Ofrecen envío gratuito?"));
                    allQuestions.add(new Question(0, "What is 'Where is the checkout?' in Spanish?", null, null, null, "¿Dónde está la caja?"));
                    allQuestions.add(new Question(0, "What is 'Can I get this delivered?' in Spanish?", null, null, null, "¿Puedo recibir esto a domicilio?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any promotional offers?' in Spanish?", null, null, null, "¿Tienes ofertas promocionales?"));
                    allQuestions.add(new Question(0, "What is 'Can I use a discount code?' in Spanish?", null, null, null, "¿Puedo usar un código de descuento?"));
                    allQuestions.add(new Question(0, "What is 'Is there a warranty for this item?' in Spanish?", null, null, null, "¿Hay garantía para este artículo?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any gift cards?' in Spanish?", null, null, null, "¿Tienes tarjetas de regalo?"));
                    allQuestions.add(new Question(0, "What is 'Do you offer home delivery?' in Spanish?", null, null, null, "¿Ofrecen entrega a domicilio?"));
                    allQuestions.add(new Question(0, "What is 'Can I pay with cash?' in Spanish?", null, null, null, "¿Puedo pagar en efectivo?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any electronic gadgets?' in Spanish?", null, null, null, "¿Tienes gadgets electrónicos?"));
                    allQuestions.add(new Question(0, "What is 'Can I get a gift receipt?' in Spanish?", null, null, null, "¿Puedo obtener un recibo de regalo?"));
                    allQuestions.add(new Question(0, "What is 'Where can I find the beauty products?' in Spanish?", null, null, null, "¿Dónde puedo encontrar los productos de belleza?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any environmentally-friendly products?' in Spanish?", null, null, null, "¿Tienes productos ecológicos?"));
                    allQuestions.add(new Question(0, "What is 'Can I return this item?' in Spanish?", null, null, null, "¿Puedo devolver este artículo?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any clearance items?' in Spanish?", null, null, null, "¿Tienes artículos en liquidación?"));
                    allQuestions.add(new Question(0, "What is 'Where are the kitchen appliances?' in Spanish?", null, null, null, "¿Dónde están los electrodomésticos?"));
                    allQuestions.add(new Question(0, "What is 'Can I get a discount for bulk orders?' in Spanish?", null, null, null, "¿Puedo obtener un descuento por pedidos al por mayor?"));
                    allQuestions.add(new Question(0, "What is 'Is there a size guide?' in Spanish?", null, null, null, "¿Hay una guía de tallas?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any items on preorder?' in Spanish?", null, null, null, "¿Tienes artículos en preventa?"));
                    allQuestions.add(new Question(0, "What is 'Where are the books?' in Spanish?", null, null, null, "¿Dónde están los libros?"));
                    allQuestions.add(new Question(0, "What is 'Can I get this item gift-wrapped?' in Spanish?", null, null, null, "¿Puedo envolver este artículo para regalo?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any home decor?' in Spanish?", null, null, null, "¿Tienes decoración para el hogar?"));
                    allQuestions.add(new Question(0, "What is 'Where are the electronic accessories?' in Spanish?", null, null, null, "¿Dónde están los accesorios electrónicos?"));
                    allQuestions.add(new Question(0, "What is 'Can I get this in a different size?' in Spanish?", null, null, null, "¿Puedo conseguir esto en otro tamaño?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any food items?' in Spanish?", null, null, null, "¿Tienes artículos alimenticios?"));
                    allQuestions.add(new Question(0, "What is 'Where are the perfumes?' in Spanish?", null, null, null, "¿Dónde están los perfumes?"));
                    allQuestions.add(new Question(0, "What is 'Is this the final price?' in Spanish?", null, null, null, "¿Este es el precio final?"));
                    allQuestions.add(new Question(0, "What is 'Do you offer gift wrapping?' in Spanish?", null, null, null, "¿Ofrecen envoltorio para regalo?"));
                    allQuestions.add(new Question(0, "What is 'Is this item on backorder?' in Spanish?", null, null, null, "¿Este artículo está en pedido pendiente?"));
                    allQuestions.add(new Question(0, "What is 'Can I pay in installments?' in Spanish?", null, null, null, "¿Puedo pagar en cuotas?"));
                    allQuestions.add(new Question(0, "What is 'Do you have any pet supplies?' in Spanish?", null, null, null, "¿Tienes suministros para mascotas?"));

                    allQuestions.add(new Question(0, "How do you say 'I am hungry' in Spanish?", null, null, null, "Tengo hambre"));
                    allQuestions.add(new Question(0, "How do you say 'I need a doctor' in Spanish?", null, null, null, "Necesito un doctor"));
                    allQuestions.add(new Question(0, "How do you say 'I have a question' in Spanish?", null, null, null, "Tengo una pregunta"));
                    allQuestions.add(new Question(0, "How do you say 'I am tired' in Spanish?", null, null, null, "Estoy cansado"));
                    allQuestions.add(new Question(0, "How do you say 'I am learning Spanish' in Spanish?", null, null, null, "Estoy aprendiendo español"));
                    allQuestions.add(new Question(0, "How do you say 'I am a tourist' in Spanish?", null, null, null, "Soy un turista"));
                    allQuestions.add(new Question(0, "How do you say 'I am studying' in Spanish?", null, null, null, "Estoy estudiando"));
                    allQuestions.add(new Question(0, "How do you say 'I am waiting' in Spanish?", null, null, null, "Estoy esperando"));
                    allQuestions.add(new Question(0, "How do you say 'It’s cold' in Spanish?", null, null, null, "Hace frío"));
                    allQuestions.add(new Question(0, "How do you say 'It’s hot' in Spanish?", null, null, null, "Hace calor"));
                    allQuestions.add(new Question(0, "How do you say 'I am lost' in Spanish?", null, null, null, "Estoy perdido"));
                    allQuestions.add(new Question(0, "How do you say 'I am happy' in Spanish?", null, null, null, "Estoy feliz"));
                    allQuestions.add(new Question(0, "How do you say 'I’m sorry' in Spanish?", null, null, null, "Lo siento"));
                    allQuestions.add(new Question(0, "How do you say 'I am sick' in Spanish?", null, null, null, "Estoy enfermo"));
                    allQuestions.add(new Question(0, "How do you say 'Where is the hospital?' in Spanish?", null, null, null, "¿Dónde está el hospital?"));
                    allQuestions.add(new Question(0, "How do you say 'Where is the train station?' in Spanish?", null, null, null, "Dónde está la estación de tren"));
                    allQuestions.add(new Question(0, "How do you say 'Where is the restaurant?' in Spanish?", null, null, null, "Dónde está el restaurante"));
                    allQuestions.add(new Question(0, "How do you say 'I need a taxi' in Spanish?", null, null, null, "Necesito un taxi"));
                    allQuestions.add(new Question(0, "How do you say 'I like Spanish music' in Spanish?", null, null, null, "Me gusta la música española"));
                    allQuestions.add(new Question(0, "How do you say 'I need help' in Spanish?", null, null, null, "Necesito ayuda"));

                    allQuestions.add(new Question(0, "Translate 'I would like a coffee, please' to Spanish:", null, null, null, "Me gustaría un café, por favor"));
                    allQuestions.add(new Question(0, "What's the Spanish word for 'tomorrow'?", null, null, null, "Mañana"));
                    allQuestions.add(new Question(0, "How do you say 'Good night' in Spanish?", null, null, null, "Buenas noches"));
                    allQuestions.add(new Question(0, "Translate 'How are you?' to Spanish:", null, null, null, "¿Cómo estás?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'family'?", null, null, null, "Familia"));
                    allQuestions.add(new Question(0, "How do you say 'I love you' in Spanish?", null, null, null, "Te quiero"));
                    allQuestions.add(new Question(0, "Translate 'See you later' to Spanish:", null, null, null, "Hasta luego"));
                    allQuestions.add(new Question(0, "What's the Spanish word for 'friend'?", null, null, null, "Amigo"));
                    allQuestions.add(new Question(0, "How do you say 'Good morning' in Spanish?", null, null, null, "Buenos días"));
                    allQuestions.add(new Question(0, "Translate 'Can you help me?' to Spanish:", null, null, null, "¿Puedes ayudarme?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'water'?", null, null, null, "Agua"));
                    allQuestions.add(new Question(0, "How do you say 'Excuse me' in Spanish?", null, null, null, "Perdón"));
                    allQuestions.add(new Question(0, "Translate 'Where is the bathroom?' to Spanish:", null, null, null, "¿Dónde está el baño?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'apple'?", null, null, null, "Manzana"));
                    allQuestions.add(new Question(0, "How do you say 'What time is it?' in Spanish?", null, null, null, "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "Translate 'Do you speak English?' to Spanish:", null, null, null, "¿Hablas inglés?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'school'?", null, null, null, "Escuela"));
                    allQuestions.add(new Question(0, "How do you say 'I don’t understand' in Spanish?", null, null, null, "No entiendo"));
                    allQuestions.add(new Question(0, "Translate 'How much does it cost?' to Spanish:", null, null, null, "¿Cuánto cuesta?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'book'?", null, null, null, "Libro"));

                    allQuestions.add(new Question(0, "Translate 'How much is this?' into Spanish:", null, null, null, "¿Cuánto cuesta esto?"));
                    allQuestions.add(new Question(0, "Translate 'I love you' into Spanish:", null, null, null, "Te quiero"));
                    allQuestions.add(new Question(0, "Translate 'I don’t understand' into Spanish:", null, null, null, "No entiendo"));
                    allQuestions.add(new Question(0, "Translate 'What time is it?' into Spanish:", null, null, null, "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "Translate 'See you later' into Spanish:", null, null, null, "Hasta luego"));
                    allQuestions.add(new Question(0, "Translate 'What’s your name?' into Spanish:", null, null, null, "¿Cómo te llamas?"));
                    allQuestions.add(new Question(0, "Translate 'I am lost' into Spanish:", null, null, null, "Estoy perdido"));
                    allQuestions.add(new Question(0, "Translate 'Can I help you?' into Spanish:", null, null, null, "¿Puedo ayudarte?"));
                    allQuestions.add(new Question(0, "Translate 'I need water' into Spanish:", null, null, null, "Necesito agua"));
                    allQuestions.add(new Question(0, "Translate 'I don’t know' into Spanish:", null, null, null, "No sé"));
                    allQuestions.add(new Question(0, "Translate 'I’m thirsty' into Spanish:", null, null, null, "Tengo sed"));
                    allQuestions.add(new Question(0, "Translate 'What is your favorite color?' into Spanish:", null, null, null, "¿Cuál es tu color favorito?"));
                    allQuestions.add(new Question(0, "Translate 'I like this' into Spanish:", null, null, null, "Me gusta esto"));
                    allQuestions.add(new Question(0, "Translate 'Where are you from?' into Spanish:", null, null, null, "¿De dónde eres?"));
                    allQuestions.add(new Question(0, "Translate 'I’m happy' into Spanish:", null, null, null, "Estoy feliz"));
                    allQuestions.add(new Question(0, "Translate 'Have a nice day' into Spanish:", null, null, null, "Que tengas un buen día"));
                    allQuestions.add(new Question(0, "Translate 'I am sorry' into Spanish:", null, null, null, "Lo siento"));
                    allQuestions.add(new Question(0, "Translate 'Can you repeat that?' into Spanish:", null, null, null, "¿Puedes repetir eso?"));
                    allQuestions.add(new Question(0, "Translate 'I’m learning Spanish' into Spanish:", null, null, null, "Estoy aprendiendo español"));
                    allQuestions.add(new Question(0, "Translate 'Is it far?' into Spanish:", null, null, null, "¿Está lejos?"));


                    allQuestions.add(new Question(0, "Translate 'Good afternoon' to Spanish:", null, null, null, "Buenas tardes"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'dog'?", null, null, null, "Perro"));
                    allQuestions.add(new Question(0, "How do you say 'Thank you' in Spanish?", null, null, null, "Gracias"));
                    allQuestions.add(new Question(0, "Translate 'I am hungry' to Spanish:", null, null, null, "Tengo hambre"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'house'?", null, null, null, "Casa"));
                    allQuestions.add(new Question(0, "How do you say 'I am tired' in Spanish?", null, null, null, "Estoy cansado"));
                    allQuestions.add(new Question(0, "Translate 'What is your name?' to Spanish:", null, null, null, "¿Cuál es tu nombre?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'cat'?", null, null, null, "Gato"));
                    allQuestions.add(new Question(0, "How do you say 'Where are you from?' in Spanish?", null, null, null, "¿De dónde eres?"));
                    allQuestions.add(new Question(0, "Translate 'I am lost' to Spanish:", null, null, null, "Estoy perdido"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'bread'?", null, null, null, "Pan"));
                    allQuestions.add(new Question(0, "How do you say 'I am sick' in Spanish?", null, null, null, "Estoy enfermo"));
                    allQuestions.add(new Question(0, "Translate 'I don’t know' to Spanish:", null, null, null, "No sé"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'car'?", null, null, null, "Coche"));
                    allQuestions.add(new Question(0, "How do you say 'I am happy' in Spanish?", null, null, null, "Estoy feliz"));
                    allQuestions.add(new Question(0, "Translate 'Can I have the bill?' to Spanish:", null, null, null, "¿Puedo tener la cuenta?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'street'?", null, null, null, "Calle"));
                    allQuestions.add(new Question(0, "How do you say 'I need help' in Spanish?", null, null, null, "Necesito ayuda"));
                    allQuestions.add(new Question(0, "Translate 'I like it' to Spanish:", null, null, null, "Me gusta"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'store'?", null, null, null, "Tienda"));
                    allQuestions.add(new Question(0, "How do you say 'Where is the train station?' in Spanish?", null, null, null, "¿Dónde está la estación de tren?"));
                    allQuestions.add(new Question(0, "Translate 'I am going to the beach' to Spanish:", null, null, null, "Voy a la playa"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'night'?", null, null, null, "Noche"));
                    allQuestions.add(new Question(0, "How do you say 'I’m sorry' in Spanish?", null, null, null, "Lo siento"));
                    allQuestions.add(new Question(0, "Translate 'What’s your favorite color?' to Spanish:", null, null, null, "¿Cuál es tu color favorito?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'shoe'?", null, null, null, "Zapato"));
                    allQuestions.add(new Question(0, "How do you say 'I have a question' in Spanish?", null, null, null, "Tengo una pregunta"));
                    allQuestions.add(new Question(0, "Translate 'Let’s go!' to Spanish:", null, null, null, "¡Vamos!"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'computer'?", null, null, null, "Computadora"));
                    allQuestions.add(new Question(0, "How do you say 'I am learning Spanish' in Spanish?", null, null, null, "Estoy aprendiendo español"));
                    allQuestions.add(new Question(0, "Translate 'Please speak slowly' to Spanish:", null, null, null, "Por favor, habla despacio"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'city'?", null, null, null, "Ciudad"));
                    allQuestions.add(new Question(0, "How do you say 'I am thirsty' in Spanish?", null, null, null, "Tengo sed"));
                    allQuestions.add(new Question(0, "Translate 'It’s raining' to Spanish:", null, null, null, "Está lloviendo"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'sun'?", null, null, null, "Sol"));
                    allQuestions.add(new Question(0, "How do you say 'It’s cold' in Spanish?", null, null, null, "Hace frío"));
                    allQuestions.add(new Question(0, "Translate 'What day is today?' to Spanish:", null, null, null, "¿Qué día es hoy?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'restaurant'?", null, null, null, "Restaurante"));
                    allQuestions.add(new Question(0, "How do you say 'I need a taxi' in Spanish?", null, null, null, "Necesito un taxi"));
                    allQuestions.add(new Question(0, "Translate 'I’m fine, thank you' to Spanish:", null, null, null, "Estoy bien, gracias"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'coffee'?", null, null, null, "Café"));
                    allQuestions.add(new Question(0, "How do you say 'I am a tourist' in Spanish?", null, null, null, "Soy un turista"));
                    allQuestions.add(new Question(0, "Translate 'Can you repeat that?' to Spanish:", null, null, null, "¿Puedes repetir eso?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'food'?", null, null, null, "Comida"));
                    allQuestions.add(new Question(0, "How do you say 'I’m waiting' in Spanish?", null, null, null, "Estoy esperando"));
                    allQuestions.add(new Question(0, "Translate 'I like Spanish music' to Spanish:", null, null, null, "Me gusta la música española"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'phone'?", null, null, null, "Teléfono"));
                    allQuestions.add(new Question(0, "How do you say 'I am studying' in Spanish?", null, null, null, "Estoy estudiando"));
                    allQuestions.add(new Question(0, "Translate 'What time does the train leave?' to Spanish:", null, null, null, "¿A qué hora sale el tren?"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'hotel'?", null, null, null, "Hotel"));
                    allQuestions.add(new Question(0, "How do you say 'I’m lost' in Spanish?", null, null, null, "Estoy perdido"));
                    allQuestions.add(new Question(0, "Translate 'I am on vacation' to Spanish:", null, null, null, "Estoy de vacaciones"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'wine'?", null, null, null, "Vino"));
                    allQuestions.add(new Question(0, "How do you say 'Where is the hospital?' in Spanish?", null, null, null, "¿Dónde está el hospital?"));
                    allQuestions.add(new Question(0, "Translate 'I need a doctor' to Spanish:", null, null, null, "Necesito un doctor"));
                    allQuestions.add(new Question(0, "What is the Spanish word for 'money'?", null, null, null, "Dinero"));
                    allQuestions.add(new Question(0, "How do you say 'I like to travel' in Spanish?", null, null, null, "Me gusta viajar"));

                    Collections.shuffle(allQuestions);

                    questions = new ArrayList<>(allQuestions.subList(0, 20));
                    break;
                case "hard":

                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Dónde estás?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cómo te llamas?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cuántos años tienes?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿De dónde eres?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué haces?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Dónde vives?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué hora es?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cómo estás?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Hablas inglés?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Puedes ayudarme?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué día es hoy?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué te gusta hacer?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Dónde está el baño?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cuánto cuesta?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Te gusta viajar?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué tal?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Puedes repetirlo?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Dónde está la estación de tren?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿A qué hora es la reunión?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Tienes hambre?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Tienes hermanos o hermanas?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cómo se llama tu madre?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿De qué color es tu coche?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Tienes tiempo libre?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué deporte practicas?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cuál es tu comida favorita?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Te gusta leer?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cuándo es tu cumpleaños?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué te gustaría hacer hoy?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Dónde compraste eso?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Puedo ayudarte?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Hace buen tiempo hoy?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Qué tipo de música te gusta?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Vas al cine este fin de semana?"));
                    allQuestions.add(new Question(0, "🔊", null, null, null, "¿Cuántos idiomas hablas?"));


                    allQuestions.add(new Question(R.drawable.hat, "What is this called in Spanish?", null, null, null, "Sombrero"));
                    allQuestions.add(new Question(R.drawable.shirt, "What is this called in Spanish?", null, null, null, "Camiseta"));
                    allQuestions.add(new Question(R.drawable.pants, "What is this called in Spanish?", null, null, null, "Pantalones"));
                    allQuestions.add(new Question(R.drawable.shoes, "What is this called in Spanish?", null, null, null, "Zapatos"));
                    allQuestions.add(new Question(R.drawable.jacket, "What is this called in Spanish?", null, null, null, "Chaqueta"));
                    allQuestions.add(new Question(R.drawable.glove, "What is this called in Spanish?", null, null, null, "Guante"));
                    allQuestions.add(new Question(R.drawable.scarf, "What is this called in Spanish?", null, null, null, "Bufanda"));
                    allQuestions.add(new Question(R.drawable.shot, "What is this called in Spanish?", null, null, null, "Pantalones cortos"));
                    allQuestions.add(new Question(R.drawable.glasses, "What is this called in Spanish?", null, null, null, "Gafas"));
                    allQuestions.add(new Question(R.drawable.key, "What is this called in Spanish?", null, null, null, "Llave"));
                    allQuestions.add(new Question(R.drawable.comb, "What is this called in Spanish?", null, null, null, "Peina"));
                    allQuestions.add(new Question(R.drawable.toothbrush, "What is this called in Spanish?", null, null, null, "Cepillo de dientes"));

                    allQuestions.add(new Question(0, "How do you say 'I would like a double shot of espresso with a dash of cinnamon, please' in Spanish?", null, null, null, "Me gustaría un espresso doble con una pizca de canela, por favor"));
                    allQuestions.add(new Question(0, "How do you say 'the dilemma' in Spanish?", null, null, null, "El dilema"));
                    allQuestions.add(new Question(0, "How do you say 'I have to wake up early for an important meeting tomorrow morning' in Spanish?", null, null, null, "Tengo que despertar temprano para una reunión importante mañana por la mañana"));
                    allQuestions.add(new Question(0, "How do you say 'Can you explain the process in more detail, please?' in Spanish?", null, null, null, "¿Puedes explicar el proceso con más detalle, por favor?"));
                    allQuestions.add(new Question(0, "How do you say 'I’m planning to visit a friend in Barcelona next summer' in Spanish?", null, null, null, "Estoy planeando visitar a un amigo en Barcelona el próximo verano"));
                    allQuestions.add(new Question(0, "How do you say 'I have a reservation under the name of Smith for 7 p.m.' in Spanish?", null, null, null, "Tengo una reserva a nombre de Smith para las 7 p.m."));
                    allQuestions.add(new Question(0, "How do you say 'It’s been a while since I last saw you' in Spanish?", null, null, null, "Hace tiempo que no te veía"));
                    allQuestions.add(new Question(0, "How do you say 'I would prefer not to talk about that topic right now' in Spanish?", null, null, null, "Preferiría no hablar de ese tema en este momento"));
                    allQuestions.add(new Question(0, "How do you say 'I am currently working on an urgent project that requires my immediate attention' in Spanish?", null, null, null, "Actualmente estoy trabajando en un proyecto urgente que requiere mi atención inmediata"));
                    allQuestions.add(new Question(0, "How do you say 'She has been learning Spanish for over five years, but she still makes a lot of mistakes' in Spanish?", null, null, null, "Ella ha estado aprendiendo español durante más de cinco años, pero todavía comete muchos errores"));
                    allQuestions.add(new Question(0, "How do you say 'Could you kindly assist me with this complex issue?' in Spanish?", null, null, null, "¿Podrías asistirme amablemente con este problema complejo?"));
                    allQuestions.add(new Question(0, "How do you say 'I’m really looking forward to the concert this weekend' in Spanish?", null, null, null, "Estoy realmente deseando que llegue el concierto de este fin de semana"));
                    allQuestions.add(new Question(0, "How do you say 'I would like to express my gratitude for all your help over the past few weeks' in Spanish?", null, null, null, "Me gustaría expresar mi gratitud por toda tu ayuda durante las últimas semanas"));
                    allQuestions.add(new Question(0, "How do you say 'The weather forecast predicts rain all week' in Spanish?", null, null, null, "El pronóstico del tiempo predice lluvia toda la semana"));
                    allQuestions.add(new Question(0, "How do you say 'I have a strong interest in learning more about Spanish culture and history' in Spanish?", null, null, null, "Tengo un gran interés en aprender más sobre la cultura e historia españolas"));
                    allQuestions.add(new Question(0, "How do you say 'I would appreciate it if you could send me the report by tomorrow morning' in Spanish?", null, null, null, "Agradecería si pudieras enviarme el informe para mañana por la mañana"));
                    allQuestions.add(new Question(0, "How do you say 'I’ve been meaning to contact you for quite some time, but I’ve been busy' in Spanish?", null, null, null, "He tenido la intención de contactarte durante bastante tiempo, pero he estado ocupado"));
                    allQuestions.add(new Question(0, "How do you say 'I need to take a few days off to attend a family emergency' in Spanish?", null, null, null, "Necesito tomar unos días libres para atender una emergencia familiar"));
                    allQuestions.add(new Question(0, "How do you say 'I was wondering if you could recommend a good place to stay in Madrid' in Spanish?", null, null, null, "Me preguntaba si podrías recomendarme un buen lugar para hospedarse en Madrid"));
                    allQuestions.add(new Question(0, "How do you say 'We have been working on this project for several months and are almost finished' in Spanish?", null, null, null, "Hemos estado trabajando en este proyecto durante varios meses y casi hemos terminado"));
                    allQuestions.add(new Question(0, "How do you say 'I am not sure if I can make it to the meeting tomorrow, but I will try' in Spanish?", null, null, null, "No estoy seguro de si podré asistir a la reunión mañana, pero lo intentaré"));
                    allQuestions.add(new Question(0, "How do you say 'Could you kindly provide me with more detailed information about the product?' in Spanish?", null, null, null, "¿Podrías proporcionarme más información detallada sobre el producto, por favor?"));
                    allQuestions.add(new Question(0, "How do you say 'I’m really excited about the new project that we are starting next week' in Spanish?", null, null, null, "Estoy realmente emocionado por el nuevo proyecto que comenzamos la próxima semana"));
                    allQuestions.add(new Question(0, "How do you say 'I think it’s going to be a very challenging, but rewarding experience' in Spanish?", null, null, null, "Creo que será una experiencia muy desafiante, pero gratificante"));
                    allQuestions.add(new Question(0, "How do you say 'I hope everything is going well with your new job' in Spanish?", null, null, null, "Espero que todo esté yendo bien con tu nuevo trabajo"));
                    allQuestions.add(new Question(0, "How do you say 'I’m sorry, I didn’t catch what you said' in Spanish?", null, null, null, "Lo siento, no entendí lo que dijiste"));
                    allQuestions.add(new Question(0, "How do you say 'It’s important to stay positive and focused during difficult times' in Spanish?", null, null, null, "Es importante mantener una actitud positiva y enfocada durante los momentos difíciles"));
                    allQuestions.add(new Question(0, "How do you say 'I’m going to study abroad next year to improve my language skills' in Spanish?", null, null, null, "Voy a estudiar en el extranjero el próximo año para mejorar mis habilidades lingüísticas"));
                    allQuestions.add(new Question(0, "How do you say 'Can you help me with this task? I’m having some difficulties' in Spanish?", null, null, null, "¿Puedes ayudarme con esta tarea? Estoy teniendo algunas dificultades"));
                    allQuestions.add(new Question(0, "How do you say 'I’ve been living in this city for a few years now, and I love it' in Spanish?", null, null, null, "He estado viviendo en esta ciudad durante algunos años y me encanta"));

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
                micButton.setVisibility(View.GONE);
                questionText.setText(Html.fromHtml(currentQuestion.getQuestionText()));
                questionText.setVisibility(View.VISIBLE);


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
            editor.putInt(KEY_SPAIN_QUIZ_SCORE, score);
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
                    editor.putBoolean(SPAIN_EASY_PASSED, true);
                    editor.apply();
                    resultMessage = "<br><font color='#4CAF50'> Congratulations! You can proceed to the next level.</font><br>";
                } else {
                    resultMessage = "<br><font color='#F44336'> You need to improve your score to proceed to the next level.</font><br>";
                }
            } else if (selectedLevel.equalsIgnoreCase("mediate")) {
                if (scorePercentage >= 60) {
                    SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                    editor.putBoolean(SPAIN_MEDIATE_PASSED, true);
                    editor.apply();
                    mediatePassed = true;
                    levelPassed = true;
                    resultMessage = "<br><font color='#4CAF50'>Passed - Congratulations! You can proceed to the next level.</font><br>";
                } else {
                    resultMessage = "<br><font color='#F44336'> You need to improve your score to proceed to the next level.</font><br>";
                    // Ensure ang easy level remains unlocked kahit ang mediate is failed
                    easyPassed = true;
                }
            } else if (selectedLevel.equalsIgnoreCase("hard")) {
                if (score >= 50) {
                    levelPassed = true;
                    resultMessage = "<br><font color='#4CAF50'> Congratulations! You have passed.</font><br>";
                } else {
                    resultMessage = "<br><font color='#F44336'> Please review the lesson and try again.</font><br>";
                }
                // Ensure ang easy and mediate levels remain unlocked parin even if hard is failed
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
                    easyPassed = true; //
                    mediatePassed = true;
                    playSound(true);
                    Toast.makeText(this, "Passed", Toast.LENGTH_LONG).show();
                }
            } else {
                proceedButton.setVisibility(View.GONE);
                if (selectedLevel.equalsIgnoreCase("easy")) {
                    playSound(false); // Play fail sound
                    Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                } else if (selectedLevel.equalsIgnoreCase("mediate")) {
                    playSound(false); // Play fail sound
                    Toast.makeText(this, "Failed", Toast.LENGTH_LONG).show();
                }
            }

            if (selectedLevel.equalsIgnoreCase("mediate")) {
                micButton.setVisibility(View.GONE);
            }

            if (selectedLevel.equalsIgnoreCase("hard")) {
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
                            width = questionText.getResources().getDisplayMetrics().widthPixels / 2;
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


            dbHelper.insertQuizResult( score, questions.size(), selectedLevel, selectedLevel, "Spanish");




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

    }

