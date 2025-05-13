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


public class ChinaPage extends AppCompatActivity {

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

    private static final String KEY_CHINESE_QUIZ_SCORE = "ChineseQuizScore";
    private TextToSpeech textToSpeech;
    private Button speakerButton;


    private static final String PREFS_NAME = "QuizScores";
    private static final String CHINA_EASY_PASSED = "chinaEasyPassed";
    private static final String CHINA_MEDIATE_PASSED = "chinaMediatePassed";

    private boolean easyPassed = false;
    private boolean mediatePassed = false;
    private Button proceedButton;


    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private SpeechRecognizer speechRecognizer;
    private MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_china);

        dbHelper = new QuizDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chinese Quiz");
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
        easyPassed = prefs.getBoolean(CHINA_EASY_PASSED, false);
        mediatePassed = prefs.getBoolean(CHINA_MEDIATE_PASSED, false);

        RadioButton easyRadio = findViewById(R.id.easy_radio);
        RadioButton mediateRadio = findViewById(R.id.mediate_radio);
        RadioButton hardRadio = findViewById(R.id.hard_radio);

        easyRadio.setEnabled(true);


        mediateRadio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!easyPassed) {
                    // Show popup message
                    Toast.makeText(ChinaPage.this, "You need to pass the easy level to unlock this", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ChinaPage.this, "You need to pass the mediate level to unlock this", Toast.LENGTH_SHORT).show();
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
                int result = textToSpeech.setLanguage(Locale.CHINESE);
                float speechRate = 0.8f; //BILIS NG TTS
                textToSpeech.setSpeechRate(speechRate);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Chinese language is not supported", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(ChinaPage.this, "Please select a difficulty level.", Toast.LENGTH_SHORT).show();
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

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {

                String errorMessage = "Unstable internet, kindly type you answer " + error;
                Toast.makeText(ChinaPage.this, errorMessage, Toast.LENGTH_SHORT).show();
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

            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
            }
        }
    }

    private void startSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your answer in Chinese");
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
                allQuestions.add(new Question(R.drawable.plate, "What is this called in Chinese?", "盘子 (Pánzǐ)", "叉子 (Chāzǐ)", "玻璃杯 (Bōlí bēi)", "盘子 (Pánzǐ)"));
                allQuestions.add(new Question(R.drawable.fork, "What is this called in Chinese?", "叉子 (Chāzǐ)", "盘子 (Pánzǐ)", "玻璃杯 (Bōlí bēi)", "叉子 (Chāzǐ)"));
                allQuestions.add(new Question(R.drawable.glass, "What is this called in Chinese?", "玻璃杯 (Bōlí bēi)", "叉子 (Chāzǐ)", "盘子 (Pánzǐ)", "玻璃杯 (Bōlí bēi)"));
                allQuestions.add(new Question(R.drawable.cup, "What is this called in Chinese?", "杯子 (Bēizi)", "叉子 (Chāzǐ)", "盘子 (Pánzǐ)", "杯子 (Bēizi)"));
                allQuestions.add(new Question(R.drawable.spoon, "What is this called in Chinese?", "勺子 (Sháozi)", "盘子 (Pánzǐ)", "叉子 (Chāzǐ)", "勺子 (Sháozi)"));
                allQuestions.add(new Question(R.drawable.bottle, "What is this called in Chinese?", "瓶子 (Píngzi)", "杯子 (Bēizi)", "盘子 (Pánzǐ)", "瓶子 (Píngzi)"));
                allQuestions.add(new Question(R.drawable.book, "What is this called in Chinese?", "书 (Shū)", "桌子 (Zhuōzi)", "椅子 (Yǐzi)", "书 (Shū)"));
                allQuestions.add(new Question(R.drawable.chair, "What is this called in Chinese?", "椅子 (Yǐzi)", "书 (Shū)", "桌子 (Zhuōzi)", "椅子 (Yǐzi)"));
                allQuestions.add(new Question(R.drawable.table, "What is this called in Chinese?", "桌子 (Zhuōzi)", "椅子 (Yǐzi)", "书 (Shū)", "桌子 (Zhuōzi)"));
                allQuestions.add(new Question(R.drawable.phone, "What is this called in Chinese?", "电话 (Diànhuà)", "手机 (Shǒujī)", "书 (Shū)", "手机 (Shǒujī)"));
                allQuestions.add(new Question(R.drawable.laptop, "What is this called in Chinese?", "笔记本 (Bǐjìběn)", "桌子 (Zhuōzi)", "电话 (Diànhuà)", "笔记本 (Bǐjìběn)"));
                allQuestions.add(new Question(R.drawable.desk, "What is this called in Chinese?", "书桌 (Shūzhuō)", "椅子 (Yǐzi)", "桌子 (Zhuōzi)", "书桌 (Shūzhuō)"));
                allQuestions.add(new Question(R.drawable.computer, "What is this called in Chinese?", "电脑 (Diànnǎo)", "书 (Shū)", "椅子 (Yǐzi)", "电脑 (Diànnǎo)"));
                allQuestions.add(new Question(R.drawable.window, "What is this called in Chinese?", "窗户 (Chuānghù)", "门 (Mén)", "墙 (Qiáng)", "窗户 (Chuānghù)"));
                allQuestions.add(new Question(R.drawable.door, "What is this called in Chinese?", "门 (Mén)", "窗户 (Chuānghù)", "墙 (Qiáng)", "门 (Mén)"));
                allQuestions.add(new Question(R.drawable.tv, "What is this called in Chinese?", "电视 (Diànshì)", "电话 (Diànhuà)", "电脑 (Diànnǎo)", "电视 (Diànshì)"));
                allQuestions.add(new Question(R.drawable.car, "What is this called in Chinese?", "汽车 (Qìchē)", "自行车 (Zìxíngchē)", "摩托车 (Mótuōchē)", "汽车 (Qìchē)"));
                allQuestions.add(new Question(R.drawable.bike, "What is this called in Chinese?", "自行车 (Zìxíngchē)", "摩托车 (Mótuōchē)", "汽车 (Qìchē)", "自行车 (Zìxíngchē)"));
                allQuestions.add(new Question(R.drawable.clock, "What is this called in Chinese?", "钟 (Zhōng)", "表 (Biǎo)", "电脑 (Diànnǎo)", "钟 (Zhōng)"));
                allQuestions.add(new Question(R.drawable.watch, "What is this called in Chinese?", "手表 (Shǒubiǎo)", "钟 (Zhōng)", "表 (Biǎo)", "手表 (Shǒubiǎo)"));
                allQuestions.add(new Question(R.drawable.bag, "What is this called in Chinese?", "包 (Bāo)", "鞋 (Xié)", "帽子 (Màozi)", "包 (Bāo)"));
                allQuestions.add(new Question(R.drawable.hat, "What is this called in Chinese?", "帽子 (Màozi)", "包 (Bāo)", "鞋 (Xié)", "帽子 (Màozi)"));
                allQuestions.add(new Question(R.drawable.shirt, "What is this called in Chinese?", "衬衫 (Chènshān)", "裤子 (Kùzi)", "鞋 (Xié)", "衬衫 (Chènshān)"));
                allQuestions.add(new Question(R.drawable.pants, "What is this called in Chinese?", "裤子 (Kùzi)", "衬衫 (Chènshān)", "鞋 (Xié)", "裤子 (Kùzi)"));
                allQuestions.add(new Question(R.drawable.shoes, "What is this called in Chinese?", "鞋 (Xié)", "裤子 (Kùzi)", "包 (Bāo)", "鞋 (Xié)"));
                allQuestions.add(new Question(R.drawable.jacket, "What is this called in Chinese?", "夹克 (Jiákè)", "裤子 (Kùzi)", "衬衫 (Chènshān)", "夹克 (Jiákè)"));
                allQuestions.add(new Question(R.drawable.glove, "What is this called in Chinese?", "手套 (Shǒutào)", "鞋 (Xié)", "包 (Bāo)", "手套 (Shǒutào)"));
                allQuestions.add(new Question(R.drawable.scarf, "What is this called in Chinese?", "围巾 (Wéijīn)", "帽子 (Màozi)", "手套 (Shǒutào)", "围巾 (Wéijīn)"));
                allQuestions.add(new Question(R.drawable.shot, "What is this called in Chinese?", "短裤 (Duǎn kù)", "裤子 (Kùzi)", "衬衫 (Chènshān)", "短裤 (Duǎn kù)"));
                allQuestions.add(new Question(R.drawable.glasses, "What is this called in Chinese?", "眼镜 (Yǎnjìng)", "帽子 (Màozi)", "包 (Bāo)", "眼镜 (Yǎnjìng)"));
                allQuestions.add(new Question(R.drawable.key, "What is this called in Chinese?", "钥匙 (Yàoshi)", "门 (Mén)", "窗户 (Chuānghù)", "钥匙 (Yàoshi)"));
                allQuestions.add(new Question(R.drawable.comb, "What is this called in Chinese?", "梳子 (Shūzi)", "镜子 (Jìngzi)", "毛巾 (Máojīn)", "梳子 (Shūzi)"));
                allQuestions.add(new Question(R.drawable.toothbrush, "What is this called in Chinese?", "牙刷 (Yáshuā)", "牙膏 (Yágāo)", "梳子 (Shūzi)", "牙刷 (Yáshuā)"));
                allQuestions.add(new Question(R.drawable.toothpaste, "What is this called in Chinese?", "牙膏 (Yágāo)", "牙刷 (Yáshuā)", "毛巾 (Máojīn)", "牙膏 (Yágāo)"));
                allQuestions.add(new Question(R.drawable.socks, "What is this called in Chinese?", "袜子 (Wàzi)", "鞋 (Xié)", "裤子 (Kùzi)", "袜子 (Wàzi)"));
                allQuestions.add(new Question(R.drawable.blanket, "What is this called in Chinese?", "毯子 (Tǎnzi)", "枕头 (Zhěntou)", "床 (Chuáng)", "毯子 (Tǎnzi)"));
                allQuestions.add(new Question(R.drawable.towel, "What is this called in Chinese?", "毛巾 (Máojīn)", "枕头 (Zhěntou)", "毯子 (Tǎnzi)", "毛巾 (Máojīn)"));
                allQuestions.add(new Question(R.drawable.pillow, "What is this called in Chinese?", "枕头 (Zhěntou)", "床 (Chuáng)", "毯子 (Tǎnzi)", "枕头 (Zhěntou)"));
                allQuestions.add(new Question(R.drawable.broom, "What is this called in Chinese?", "扫帚 (Sàozhǒu)", "拖把 (Tuōbǎ)", "垃圾桶 (Lājītǒng)", "扫帚 (Sàozhǒu)"));

                allQuestions.add(new Question(0, "Translate 'To work' into Chinese:", "上班(Shàngbān)", "去城市(Qù chéngshì)", "去洗手间(Qù xǐshǒujiān)", "上班(Shàngbān)"));
                allQuestions.add(new Question(0, "Translate 'No' into Chinese:", "不(Bù)", "是(Sì)", "也许(Yěxǔ)", "不(Bù)"));
                allQuestions.add(new Question(0, "Translate 'Do you understand?' into Chinese:", "你明白吗？(Nǐ míngbái ma?)", "你会说吗？(Nǐ huì shuō ma?)", "哪里？(Nǎlǐ?)", "你明白吗？(Nǐ míngbái ma?)"));
                allQuestions.add(new Question(0, "Translate 'I don't understand' into Chinese:", "我不明白(Wǒ bù míngbái)", "我明白(Wǒ míngbái)", "你会说英语吗？(Nǐ huì shuō yīngyǔ ma?)", "我不明白(Wǒ bù míngbái)"));
                allQuestions.add(new Question(0, "Translate 'Where?' into Chinese:", "哪里？(Nǎlǐ?)", "什么时候？(Shénme shíhòu?)", "什么？(Shénme?)", "哪里？(Nǎlǐ?)"));
                allQuestions.add(new Question(0, "Translate 'What?' into Chinese:", "什么？(Shénme?)", "怎么？(Zěnme?)", "哪里？(Nǎlǐ?)", "什么？(Shénme?)"));
                allQuestions.add(new Question(0, "Translate 'How?' into Chinese:", "怎么？(Zěnme?)", "什么时候？(Shénme shíhòu?)", "为什么？(Wèishéme?)", "怎么？(Zěnme?)"));
                allQuestions.add(new Question(0, "Translate 'How much?' into Chinese:", "多少钱？(Duōshǎo qián?)", "哪里？(Nǎlǐ?)", "为什么？(Wèishéme?)", "多少钱？(Duōshǎo qián?)"));
                allQuestions.add(new Question(0, "Translate 'When?' into Chinese:", "什么时候？(Shénme shíhòu?)", "怎么？(Zěnme?)", "哪里？(Nǎlǐ?)", "什么时候？(Shénme shíhòu?)"));
                allQuestions.add(new Question(0, "Translate 'Who?' into Chinese:", "谁？(Shéi?)", "什么？(Shénme?)", "怎么？(Zěnme?)", "谁？(Shéi?)"));
                allQuestions.add(new Question(0, "Translate 'Why?' into Chinese:", "为什么？(Wèishéme?)", "怎么？(Zěnme?)", "哪里？(Nǎlǐ?)", "为什么？(Wèishéme?)"));
                allQuestions.add(new Question(0, "Translate 'Thank you' into Chinese:", "谢谢(Xièxiè)", "请(Por favor)", "对不起(Duìbùqǐ)", "谢谢(Xièxiè)"));
                allQuestions.add(new Question(0, "Translate 'I'm sorry' into Chinese:", "对不起(Duìbùqǐ)", "谢谢(Xièxiè)", "请(Por favor)", "对不起(Duìbùqǐ)"));
                allQuestions.add(new Question(0, "Translate 'Congratulations!' into Chinese:", "恭喜(Gōngxǐ)", "干得好！(Gàn dé hǎo!)", "太棒了！(Tài bàngle!)", "恭喜(Gōngxǐ)"));
                allQuestions.add(new Question(0, "Translate 'It's okay' into Chinese:", "没关系(Méi guānxi)", "没事(Méi shì)", "抱歉(Bàoqiàn)", "没关系(Méi guānxi)"));
                allQuestions.add(new Question(0, "Translate 'I don't know' into Chinese:", "我不知道(Wǒ bù zhīdào)", "我知道(Wǒ zhīdào)", "我不明白(Wǒ bù míngbái)", "我不知道(Wǒ bù zhīdào)"));
                allQuestions.add(new Question(0, "Translate 'I don't like it' into Chinese:", "我不喜欢(Wǒ bù xǐhuān)", "我喜欢(Wǒ xǐhuān)", "我想要(Wǒ xiǎng yào)", "我不喜欢(Wǒ bù xǐhuān)"));
                allQuestions.add(new Question(0, "Translate 'I like it' into Chinese:", "我喜欢(Wǒ xǐhuān)", "我不喜欢(Wǒ bù xǐhuān)", "我爱(Wǒ ài)", "我喜欢(Wǒ xǐhuān)"));
                allQuestions.add(new Question(0, "Translate 'You're welcome' into Chinese:", "不客气(Bù kèqì)", "请(Por favor)", "没问题(Méi wèntí)", "不客气(Bù kèqì)"));
                allQuestions.add(new Question(0, "Translate 'I think that...' into Chinese:", "我认为(Wǒ rènwéi)", "我觉得(Wǒ juédé)", "我认为(Wǒ rènwéi)", "我想(Wǒ xiǎng)"));
                allQuestions.add(new Question(0, "Translate 'No, thank you' into Chinese:", "不，谢谢(Bù, xièxiè)", "是，谢谢(Sì, xièxiè)", "不，请(Bù, qǐng)", "不，谢谢(Bù, xièxiè)"));
                allQuestions.add(new Question(0, "Translate 'Excuse me' into Chinese:", "对不起(Duìbùqǐ)", "抱歉(Bàoqiàn)", "没关系(Méi guānxi)", "对不起(Duìbùqǐ)"));
                allQuestions.add(new Question(0, "Translate 'Take care' into Chinese:", "保重(Bǎozhòng)", "走吧(Zǒu ba)", "小心(Xiǎoxīn)", "保重(Bǎozhòng)"));
                allQuestions.add(new Question(0, "Translate 'Don't forget' into Chinese:", "别忘了(Bié wàngle)", "记得(Jìdé)", "忘了(Wàngle)", "别忘了(Bié wàngle)"));
                allQuestions.add(new Question(0, "Translate 'How do you pronounce this?' into Chinese:", "这个怎么读？(Zhège zěnme dú?)", "这是什么意思？(Zhè shì shénme yìsi?)", "这在哪里？(Zhè zài nǎlǐ?)", "这个怎么读？(Zhège zěnme dú?)"));
                allQuestions.add(new Question(0, "Translate 'Before' into Chinese:", "之前(Zhīqián)", "之后(Zhīhòu)", "期间(Qījiān)", "之前(Zhīqián)"));
                allQuestions.add(new Question(0, "Translate 'After' into Chinese:", "之后(Zhīhòu)", "之前(Zhīqián)", "期间(Qījiān)", "之后(Zhīhòu)"));
                allQuestions.add(new Question(0, "Translate 'Wrong' into Chinese:", "错(Cuò)", "对(Duì)", "当然(Dāngrán)", "错(Cuò)"));
                allQuestions.add(new Question(0, "Translate 'Right' into Chinese:", "对(Duì)", "错(Cuò)", "当然(Dāngrán)", "对(Duì)"));
                allQuestions.add(new Question(0, "Translate 'Until' into Chinese:", "直到(Zhídào)", "之后(Zhīhòu)", "之前(Zhīqián)", "直到(Zhídào)"));
                allQuestions.add(new Question(0, "Translate 'Where is the toilet?' into Chinese:", "洗手间在哪里？(Xǐshǒujiān zài nǎlǐ?)", "餐厅在哪里？(Cāntīng zài nǎlǐ?)", "商店在哪里？(Shāngdiàn zài nǎlǐ?)", "洗手间在哪里？(Xǐshǒujiān zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "Translate 'Do you live here?' into Chinese:", "你住这里吗？(Nǐ zhù zhèlǐ ma?)", "你去这里吗？(Nǐ qù zhèlǐ ma?)", "你在这里工作吗？(Nǐ zài zhèlǐ gōngzuò ma?)", "你住这里吗？(Nǐ zhù zhèlǐ ma?)"));
                allQuestions.add(new Question(0, "Translate 'Do you like it?' into Chinese:", "你喜欢吗？(Nǐ xǐhuān ma?)", "你喜欢它吗？(Nǐ xǐhuān tā ma?)", "你更喜欢吗？(Nǐ gèng xǐhuān ma?)", "你喜欢吗？(Nǐ xǐhuān ma?)"));
                allQuestions.add(new Question(0, "Translate 'I love it' into Chinese:", "我爱它(Wǒ ài tā)", "我非常喜欢(Wǒ fēicháng xǐhuān)", "我想要它(Wǒ xiǎng yào tā)", "我爱它(Wǒ ài tā)"));
                allQuestions.add(new Question(0, "Translate 'On business' into Chinese:", "商务出差(Shāngwù chūchāi)", "度假(Dùjià)", "旅行(Lǚxíng)", "商务出差(Shāngwù chūchāi)"));
                allQuestions.add(new Question(0, "Translate 'To work' into Chinese:", "上班(Shàngbān)", "学习(Xuéxí)", "休息(Xiūxí)", "上班(Shàngbān)"));
                allQuestions.add(new Question(0, "Translate 'What happened?' into Chinese:", "发生了什么事？(Fāshēngle shénme shì?)", "你做了什么？(Nǐ zuòle shénme?)", "你去了哪里？(Nǐ qùle nǎlǐ?)", "发生了什么事？(Fāshēngle shénme shì?)"));
                allQuestions.add(new Question(0, "Translate 'Do you need help?' into Chinese:", "你需要帮助吗？(Nǐ xūyào bāngzhù ma?)", "我可以帮你吗？(Wǒ kěyǐ bāng nǐ ma?)", "你有帮助吗？(Nǐ yǒu bāngzhù ma?)", "你需要帮助吗？(Nǐ xūyào bāngzhù ma?)"));
                allQuestions.add(new Question(0, "Translate 'I'm lost' into Chinese:", "我迷路了(Wǒ mílùle)", "我在这里(Wǒ zài zhèlǐ)", "我很好(Wǒ hěn hǎo)", "我迷路了(Wǒ mílùle)"));
                allQuestions.add(new Question(0, "Translate 'What time is it?' into Chinese:", "现在几点钟？(Xiànzài jǐ diǎn zhōng?)", "今天星期几？(Jīntiān xīngqī jǐ?)", "几点了？(Jǐ diǎnle?)", "现在几点钟？(Xiànzài jǐ diǎn zhōng?)"));
                allQuestions.add(new Question(0, "Translate 'I want to go there' into Chinese:", "我想去那里(Wǒ xiǎng qù nàlǐ)", "我去那里(Wǒ qù nàlǐ)", "我要走了(Wǒ yào zǒule)", "我想去那里(Wǒ xiǎng qù nàlǐ)"));
                allQuestions.add(new Question(0, "Translate 'How far is it?' into Chinese:", "有多远？(Yǒu duō yuǎn?)", "它远吗？(Tā yuǎn ma?)", "它近吗？(Tā jìn ma?)", "有多远？(Yǒu duō yuǎn?)"));
                allQuestions.add(new Question(0, "Translate 'Can I help you?' into Chinese:", "我可以帮你吗？(Wǒ kěyǐ bāng nǐ ma?)", "我帮你吗？(Wǒ bāng nǐ ma?)", "你需要帮忙吗？(Nǐ xūyào bāngmáng ma?)", "我可以帮你吗？(Wǒ kěyǐ bāng nǐ ma?)"));
                allQuestions.add(new Question(0, "Translate 'Do you speak English?' into Chinese:", "你会说英语吗？(Nǐ huì shuō yīngyǔ ma?)", "你会说西班牙语吗？(Nǐ huì shuō xībānyá yǔ ma?)", "你知道英语吗？(Nǐ zhīdào yīngyǔ ma?)", "你会说英语吗？(Nǐ huì shuō yīngyǔ ma?)"));
                allQuestions.add(new Question(0, "Translate 'I need a doctor' into Chinese:", "我需要医生(Wǒ xūyào yīshēng)", "我想要医生(Wǒ xiǎng yào yīshēng)", "我不舒服(Wǒ bù shūfú)", "我需要医生(Wǒ xūyào yīshēng)"));
                allQuestions.add(new Question(0, "Translate 'Call the police' into Chinese:", "打电话给警察(Dǎ diànhuà gěi jǐngchá)", "打电话给救护车(Dǎ diànhuà gěi jiùhùchē)", "打电话给出租车(Dǎ diànhuà gěi chūzūchē)", "打电话给警察(Dǎ diànhuà gěi jǐngchá)"));


                allQuestions.add(new Question(0, "What is 'Red' in Chinese?", "红色(Hóng sè)", "蓝色(Lán sè)", "绿色(Lǜ sè)", "红色(Hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Blue' in Chinese?", "蓝色(Lán sè)", "红色(Hóng sè)", "绿色(Lǜ sè)", "蓝色(Lán sè)"));
                allQuestions.add(new Question(0, "What is 'Green' in Chinese?", "绿色(Lǜ sè)", "红色(Hóng sè)", "蓝色(Lán sè)", "绿色(Lǜ sè)"));
                allQuestions.add(new Question(0, "What is 'Yellow' in Chinese?", "黄色(Huáng sè)", "红色(Hóng sè)", "蓝色(Lán sè)", "黄色(Huáng sè)"));
                allQuestions.add(new Question(0, "What is 'Black' in Chinese?", "黑色(Hēi sè)", "白色(Bái sè)", "蓝色(Lán sè)", "黑色(Hēi sè)"));
                allQuestions.add(new Question(0, "What is 'White' in Chinese?", "白色(Bái sè)", "黑色(Hēi sè)", "蓝色(Lán sè)", "白色(Bái sè)"));
                allQuestions.add(new Question(0, "What is 'Pink' in Chinese?", "粉色(Fěn sè)", "红色(Hóng sè)", "黄色(Huáng sè)", "粉色(Fěn sè)"));
                allQuestions.add(new Question(0, "What is 'Purple' in Chinese?", "紫色(Zǐ sè)", "蓝色(Lán sè)", "红色(Hóng sè)", "紫色(Zǐ sè)"));
                allQuestions.add(new Question(0, "What is 'Orange' in Chinese?", "橙色(Chéng sè)", "红色(Hóng sè)", "黄色(Huáng sè)", "橙色(Chéng sè)"));
                allQuestions.add(new Question(0, "What is 'Brown' in Chinese?", "棕色(Zōng sè)", "黑色(Hēi sè)", "蓝色(Lán sè)", "棕色(Zōng sè)"));
                allQuestions.add(new Question(0, "What is 'Gray' in Chinese?", "灰色(Huī sè)", "黑色(Hēi sè)", "白色(Bái sè)", "灰色(Huī sè)"));
                allQuestions.add(new Question(0, "What is 'Beige' in Chinese?", "米色(Mǐ sè)", "灰色(Huī sè)", "棕色(Zōng sè)", "米色(Mǐ sè)"));
                allQuestions.add(new Question(0, "What is 'Turquoise' in Chinese?", "绿松石(Lǜ sōng shí)", "绿色(Lǜ sè)", "蓝色(Lán sè)", "绿松石(Lǜ sōng shí)"));
                allQuestions.add(new Question(0, "What is 'Gold' in Chinese?", "金色(Jīn sè)", "银色(Yín sè)", "铜色(Tóng sè)", "金色(Jīn sè)"));
                allQuestions.add(new Question(0, "What is 'Silver' in Chinese?", "银色(Yín sè)", "金色(Jīn sè)", "铜色(Tóng sè)", "银色(Yín sè)"));
                allQuestions.add(new Question(0, "What is 'Copper' in Chinese?", "铜色(Tóng sè)", "金色(Jīn sè)", "银色(Yín sè)", "铜色(Tóng sè)"));
                allQuestions.add(new Question(0, "What is 'Bronze' in Chinese?", "青铜色(Qīng tóng sè)", "金色(Jīn sè)", "银色(Yín sè)", "青铜色(Qīng tóng sè)"));
                allQuestions.add(new Question(0, "What is 'Magenta' in Chinese?", "品红色(Pǐn hóng sè)", "紫色(Zǐ sè)", "粉色(Fěn sè)", "品红色(Pǐn hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Violet' in Chinese?", "紫罗兰色(Zǐ luó lán sè)", "蓝色(Lán sè)", "红色(Hóng sè)", "紫罗兰色(Zǐ luó lán sè)"));
                allQuestions.add(new Question(0, "What is 'Indigo' in Chinese?", "靛蓝色(Diàn lán sè)", "蓝色(Lán sè)", "绿色(Lǜ sè)", "靛蓝色(Diàn lán sè)"));
                allQuestions.add(new Question(0, "What is 'Lavender' in Chinese?", "薰衣草色(Xūn yī cǎo sè)", "紫色(Zǐ sè)", "粉色(Fěn sè)", "薰衣草色(Xūn yī cǎo sè)"));
                allQuestions.add(new Question(0, "What is 'Peach' in Chinese?", "桃色(Táo sè)", "橙色(Chéng sè)", "黄色(Huáng sè)", "桃色(Táo sè)"));
                allQuestions.add(new Question(0, "What is 'Ivory' in Chinese?", "象牙色(Xiàng yá sè)", "白色(Bái sè)", "灰色(Huī sè)", "象牙色(Xiàng yá sè)"));
                allQuestions.add(new Question(0, "What is 'Cream' in Chinese?", "奶油色(Nǎi yóu sè)", "白色(Bái sè)", "黄色(Huáng sè)", "奶油色(Nǎi yóu sè)"));
                allQuestions.add(new Question(0, "What is 'Charcoal' in Chinese?", "木炭色(Mù tàn sè)", "灰色(Huī sè)", "黑色(Hēi sè)", "木炭色(Mù tàn sè)"));
                allQuestions.add(new Question(0, "What is 'Emerald' in Chinese?", "翡翠色(Fěi cuì sè)", "绿色(Lǜ sè)", "黄色(Huáng sè)", "翡翠色(Fěi cuì sè)"));
                allQuestions.add(new Question(0, "What is 'Cyan' in Chinese?", "青色(Qīng sè)", "蓝色(Lán sè)", "绿色(Lǜ sè)", "青色(Qīng sè)"));
                allQuestions.add(new Question(0, "What is 'Mint' in Chinese?", "薄荷绿(Bò hé lǜ)", "绿色(Lǜ sè)", "黄色(Huáng sè)", "薄荷绿(Bò hé lǜ)"));
                allQuestions.add(new Question(0, "What is 'Lime' in Chinese?", "酸橙色(Suān chéng sè)", "绿色(Lǜ sè)", "黄色(Huáng sè)", "酸橙色(Suān chéng sè)"));
                allQuestions.add(new Question(0, "What is 'Plum' in Chinese?", "李子色(Lǐ zi sè)", "红色(Hóng sè)", "紫色(Zǐ sè)", "李子色(Lǐ zi sè)"));
                allQuestions.add(new Question(0, "What is 'Coral' in Chinese?", "珊瑚色(Shān hú sè)", "粉色(Fěn sè)", "橙色(Chéng sè)", "珊瑚色(Shān hú sè)"));
                allQuestions.add(new Question(0, "What is 'Tan' in Chinese?", "褐色(Hè sè)", "米色(Mǐ sè)", "棕色(Zōng sè)", "褐色(Hè sè)"));
                allQuestions.add(new Question(0, "What is 'Rust' in Chinese?", "铁锈色(Tiě xiù sè)", "棕色(Zōng sè)", "橙色(Chéng sè)", "铁锈色(Tiě xiù sè)"));
                allQuestions.add(new Question(0, "What is 'Sapphire' in Chinese?", "蓝宝石色(Lán bǎo shí sè)", "蓝色(Lán sè)", "紫色(Zǐ sè)", "蓝宝石色(Lán bǎo shí sè)"));
                allQuestions.add(new Question(0, "What is 'Scarlet' in Chinese?", "猩红色(Xīng hóng sè)", "红色(Hóng sè)", "黄色(Huáng sè)", "猩红色(Xīng hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Burgundy' in Chinese?", "酒红色(Jiǔ hóng sè)", "红色(Hóng sè)", "棕色(Zōng sè)", "酒红色(Jiǔ hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Azure' in Chinese?", "天蓝色(Tiān lán sè)", "蓝色(Lán sè)", "绿色(Lǜ sè)", "天蓝色(Tiān lán sè)"));
                allQuestions.add(new Question(0, "What is 'Seafoam' in Chinese?", "海藻绿(Hǎi zǎo lǜ)", "绿色(Lǜ sè)", "蓝色(Lán sè)", "海藻绿(Hǎi zǎo lǜ)"));
                allQuestions.add(new Question(0, "What is 'Sunset' in Chinese?", "日落色(Rì luò sè)", "橙色(Chéng sè)", "红色(Hóng sè)", "日落色(Rì luò sè)"));
                allQuestions.add(new Question(0, "What is 'Fuchsia' in Chinese?", "紫红色(Zǐ hóng sè)", "粉色(Fěn sè)", "紫色(Zǐ sè)", "紫红色(Zǐ hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Rose' in Chinese?", "玫瑰色(Méi guī sè)", "红色(Hóng sè)", "白色(Bái sè)", "玫瑰色(Méi guī sè)"));
                allQuestions.add(new Question(0, "What is 'Jade' in Chinese?", "玉色(Yù sè)", "绿色(Lǜ sè)", "黄色(Huáng sè)", "玉色(Yù sè)"));
                allQuestions.add(new Question(0, "What is 'Onyx' in Chinese?", "缟色(Gǎo sè)", "黑色(Hēi sè)", "灰色(Huī sè)", "缟色(Gǎo sè)"));
                allQuestions.add(new Question(0, "What is 'Chartreuse' in Chinese?", "查特鲁斯绿(Chá tè lǔ sī lǜ)", "绿色(Lǜ sè)", "黄色(Huáng sè)", "查特鲁斯绿(Chá tè lǔ sī lǜ)"));
                allQuestions.add(new Question(0, "What is 'Wisteria' in Chinese?", "紫藤色(Zǐ téng sè)", "紫色(Zǐ sè)", "蓝色(Lán sè)", "紫藤色(Zǐ téng sè)"));
                allQuestions.add(new Question(0, "What is 'Auburn' in Chinese?", "赤褐色(Chì hè sè)", "红色(Hóng sè)", "棕色(Zōng sè)", "赤褐色(Chì hè sè)"));
                allQuestions.add(new Question(0, "What is 'Khaki' in Chinese?", "卡其色(Kǎ qí sè)", "绿色(Lǜ sè)", "米色(Mǐ sè)", "卡其色(Kǎ qí sè)"));
                allQuestions.add(new Question(0, "What is 'Chocolate' in Chinese?", "巧克力色(Qiǎo kè lì sè)", "棕色(Zōng sè)", "黑色(Hēi sè)", "巧克力色(Qiǎo kè lì sè)"));
                allQuestions.add(new Question(0, "What is 'Honey' in Chinese?", "蜂蜜色(Fēng mì sè)", "黄色(Huáng sè)", "金色(Jīn sè)", "蜂蜜色(Fēng mì sè)"));
                allQuestions.add(new Question(0, "What is 'Amber' in Chinese?", "琥珀色(Hǔ pò sè)", "橙色(Chéng sè)", "黄色(Huáng sè)", "琥珀色(Hǔ pò sè)"));
                allQuestions.add(new Question(0, "What is 'Tangerine' in Chinese?", "橘色(Jú sè)", "橙色(Chéng sè)", "黄色(Huáng sè)", "橘色(Jú sè)"));
                allQuestions.add(new Question(0, "What is 'Periwinkle' in Chinese?", "长春花色(Cháng chūn huā sè)", "蓝色(Lán sè)", "紫色(Zǐ sè)", "长春花色(Cháng chūn huā sè)"));
                allQuestions.add(new Question(0, "What is 'Lilac' in Chinese?", "丁香色(Dīng xiāng sè)", "粉色(Fěn sè)", "紫色(Zǐ sè)", "丁香色(Dīng xiāng sè)"));
                allQuestions.add(new Question(0, "What is 'Pewter' in Chinese?", "铅色(Qiān sè)", "灰色(Huī sè)", "银色(Yín sè)", "铅色(Qiān sè)"));
                allQuestions.add(new Question(0, "What is 'Pine' in Chinese?", "松树色(Sōng shù sè)", "绿色(Lǜ sè)", "棕色(Zōng sè)", "松树色(Sōng shù sè)"));
                allQuestions.add(new Question(0, "What is 'Slate' in Chinese?", "板岩色(Bǎn yán sè)", "灰色(Huī sè)", "蓝色(Lán sè)", "板岩色(Bǎn yán sè)"));


                allQuestions.add(new Question(0, "What is 'Where is the nearest bank?' in Chinese?", "最近的银行在哪里? (Zuìjìn de yínháng zài nǎlǐ?)", "最近的火车站在哪里? (Zuìjìn de huǒchēzhàn zài nǎlǐ?)", "我怎么去餐厅? (Wǒ zěnme qù cāntīng?)", "最近的银行在哪里? (Zuìjìn de yínháng zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "What is 'How do I get to the library?' in Chinese?", "我怎么去图书馆? (Wǒ zěnme qù túshūguǎn?)", "医院在哪里? (Yīyuàn zài nǎlǐ?)", "公交车站在哪里? (Gōngjiāo chē zhàn zài nǎlǐ?)", "我怎么去图书馆? (Wǒ zěnme qù túshūguǎn?)"));
                allQuestions.add(new Question(0, "What is 'Is this the road to the beach?' in Chinese?", "这是去海滩的路吗? (Zhè shì qù hǎitān de lù ma?)", "地铁站在哪里? (Dìtiě zhàn zài nǎlǐ?)", "最近的银行在哪里? (Zuìjìn de yínháng zài nǎlǐ?)", "这是去海滩的路吗? (Zhè shì qù hǎitān de lù ma?)"));
                allQuestions.add(new Question(0, "What is 'How much does it cost to go to the museum?' in Chinese?", "去博物馆多少钱? (Qù bówùguǎn duōshǎo qián?)", "火车票多少钱? (Huǒchē piào duōshǎo qián?)", "机场在哪里? (Jīchǎng zài nǎlǐ?)", "去博物馆多少钱? (Qù bówùguǎn duōshǎo qián?)"));
                allQuestions.add(new Question(0, "What is 'Where is the nearest gas station?' in Chinese?", "最近的加油站在哪里? (Zuìjìn de jiāyóu zhàn zài nǎlǐ?)", "火车站在哪里? (Huǒchēzhàn zài nǎlǐ?)", "我怎么去餐厅? (Wǒ zěnme qù cāntīng?)", "最近的加油站在哪里? (Zuìjìn de jiāyóu zhàn zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "What is 'Can you show me the way to the park?' in Chinese?", "你能告诉我怎么去公园吗? (Nǐ néng gàosù wǒ zěnme qù gōngyuán ma?)", "医院在哪里? (Yīyuàn zài nǎlǐ?)", "我怎么去火车站? (Wǒ zěnme qù huǒchēzhàn?)", "你能告诉我怎么去公园吗? (Nǐ néng gàosù wǒ zěnme qù gōngyuán ma?)"));
                allQuestions.add(new Question(0, "What is 'Is there a grocery store nearby?' in Chinese?", "附近有杂货店吗? (Fùjìn yǒu záhuòdiàn ma?)", "公交车站在哪里? (Gōngjiāo chē zhàn zài nǎlǐ?)", "机场在哪里? (Jīchǎng zài nǎlǐ?)", "附近有杂货店吗? (Fùjìn yǒu záhuòdiàn ma?)"));
                allQuestions.add(new Question(0, "What is 'How do I get to the nearest shopping mall?' in Chinese?", "我怎么去最近的购物中心? (Wǒ zěnme qù zuìjìn de gòuwù zhōngxīn?)", "火车站在哪里? (Huǒchēzhàn zài nǎlǐ?)", "医院在哪里? (Yīyuàn zài nǎlǐ?)", "我怎么去最近的购物中心? (Wǒ zěnme qù zuìjìn de gòuwù zhōngxīn?)"));
                allQuestions.add(new Question(0, "What is 'Where can I buy tickets for the subway?' in Chinese?", "我在哪里可以买地铁票? (Wǒ zài nǎlǐ kěyǐ mǎi dìtiě piào?)", "火车站在哪里? (Huǒchēzhàn zài nǎlǐ?)", "机场在哪里? (Jīchǎng zài nǎlǐ?)", "我在哪里可以买地铁票? (Wǒ zài nǎlǐ kěyǐ mǎi dìtiě piào?)"));

                allQuestions.add(new Question(0, "What is 'Bank' in Chinese?", "银行 (s是什么? (Jīntiān de tèbié shì shénme?)", "这个多少钱? (Zhège duōshǎo qián?)", "我需要一个四人桌 (Wǒ xūyào yīgè sì rén zhuō)", "今天的特别是什么? (Jīntiān de tèbié shì shénme?)"));
                allQuestions.add(new Question(0, "What is 'How much does this cost?' in Chinese?", "这个多少钱? (Zhège duōshǎo qián?)", "我需要一个四人桌 (Wǒ xūyào yīgè sì rén zhuō)", "我可以要一杯酒吗? (Wǒ kěyǐ yào yī bēi jiǔ ma?)", "这个多少钱? (Zhège duōshǎo qián?)"));
                allQuestions.add(new Question(0, "What is 'I need a table for five' in Chinese?", "我需要一个五人桌 (Wǒ xūyào yīgè wǔ rén zhuō)", "我可以要一杯酒吗? (Wǒ kěyǐ yào yī bēi jiǔ ma?)", "你们有什么酒水吗? (Nǐmen yǒu shénme jiǔ shuǐ ma?)", "我需要一个五人桌 (Wǒ xūyào yīgè wǔ rén zhuō)"));
                allQuestions.add(new Question(0, "What is 'Can I have a glass of wine?' in Chinese?", "我可以要一杯酒吗? (Wǒ kěyǐ yào yī bēi jiǔ ma?)", "你们有什么酒水吗? (Nǐmen yǒu shénme jiǔ shuǐ ma?)", "我只是看看 (Wǒ zhǐshì kànkan)", "我可以要一杯酒吗? (Wǒ kěyǐ yào yī bēi jiǔ ma?)"));
                allQuestions.add(new Question(0, "Translate this: 'What do you have on tap?' into Chinese:", "你们有什么酒水吗? (Nǐmen yǒu shénme jiǔ shuǐ ma?)", "我只是看看 (Wǒ zhǐshì kànkan)", "我可以不放洋葱吗? (Wǒ kěyǐ bù fàng yángcōng ma?)", "你们有什么酒水吗? (Nǐmen yǒu shénme jiǔ shuǐ ma?)"));
                allQuestions.add(new Question(0, "What is 'I’m just browsing' in Chinese?", "我只是看看 (Wǒ zhǐshì kànkan)", "我可以不放洋葱吗? (Wǒ kěyǐ bù fàng yángcōng ma?)", "我想要汤 (Wǒ xiǎng yào tāng)", "我只是看看 (Wǒ zhǐshì kànkan)"));
                allQuestions.add(new Question(0, "What is 'Can I have it without garlic?' in Chinese?", "我可以不放蒜吗? (Wǒ kěyǐ bù fàng suàn ma?)", "我想要汤 (Wǒ xiǎng yào tāng)", "它不含麸质吗? (Tā bù hán fūzhì ma?)", "我可以不放蒜吗? (Wǒ kěyǐ bù fàng suàn ma?)"));
                allQuestions.add(new Question(0, "What is 'I’ll have the soup' in Chinese?", "我想要汤 (Wǒ xiǎng yào tāng)", "它不含麸质吗? (Tā bù hán fūzhì ma?)", "我喜欢我的肉全熟 (Wǒ xǐhuān wǒ de ròu quán shú)", "我想要汤 (Wǒ xiǎng yào tāng)"));
                allQuestions.add(new Question(0, "What is 'Is it dairy-free?' in Chinese?", "它不含奶制品吗? (Tā bù hán nǎi zhìpǐn ma?)", "我喜欢我的肉全熟 (Wǒ xǐhuān wǒ de ròu quán shú)", "我可以要一些额外的餐巾纸吗? (Wǒ kěyǐ yào yīxiē éwài de cānjīnzhǐ ma?)", "它不含奶制品吗? (Tā bù hán nǎi zhìpǐn ma?)"));
                allQuestions.add(new Question(0, "What is 'I’d like my steak medium-rare' in Chinese?", "我想要我的牛排中等熟 (Wǒ xiǎng yào wǒ de niúpái zhōngděng shú)", "我可以要一些额外的餐巾纸吗? (Wǒ kěyǐ yào yīxiē éwài de cānjīnzhǐ ma?)", "我可以不放洋葱吗? (Wǒ kěyǐ bù fàng yángcōng ma?)", "我想要我的牛排中等熟 (Wǒ xiǎng yào wǒ de niúpái zhōngděng shú)"));
                allQuestions.add(new Question(0, "Translate this: 'Can I have some extra napkins?' into Chinese:", "我可以要一些额外的餐巾纸吗? (Wǒ kěyǐ yào yīxiē éwài de cānjīnzhǐ ma?)", "我可以不放洋葱吗? (Wǒ kěyǐ bù fàng yángcōng ma?)", "它不含麸质吗? (Tā bù hán fūzhì ma?)", "我可以要一些额外的餐巾纸吗? (Wǒ kěyǐ yào yīxiē éwài de cānjīnzhǐ ma?)"));



                allQuestions.add(new Question(0, "What is 'Good morning' in Chinese?", "早上好 (Zǎoshang hǎo)", "下午好 (Xiàwǔ hǎo)", "晚安 (Wǎn'ān)", "早上好 (Zǎoshang hǎo)"));
                allQuestions.add(new Question(0, "What is 'Good afternoon' in Chinese?", "下午好 (Xiàwǔ hǎo)", "早上好 (Zǎoshang hǎo)", "晚安 (Wǎn'ān)", "下午好 (Xiàwǔ hǎo)"));
                allQuestions.add(new Question(0, "What is 'Good night' in Chinese?", "晚安 (Wǎn'ān)", "早上好 (Zǎoshang hǎo)", "下午好 (Xiàwǔ hǎo)", "晚安 (Wǎn'ān)"));
                allQuestions.add(new Question(0, "What is 'How are you?' in Chinese?", "你好吗？ (Nǐ hǎo ma?)", "我很好 (Wǒ hěn hǎo)", "怎么样？ (Zěnme yàng?)", "你好吗？ (Nǐ hǎo ma?)"));
                allQuestions.add(new Question(0, "What is 'I’m okay' in Chinese?", "我很好 (Wǒ hěn hǎo)", "我不好 (Wǒ bù hǎo)", "一般般 (Yìbān bān)", "我很好 (Wǒ hěn hǎo)"));
                allQuestions.add(new Question(0, "What is 'I’m not okay' in Chinese?", "我不好 (Wǒ bù hǎo)", "我很好 (Wǒ hěn hǎo)", "一般般 (Yìbān bān)", "我不好 (Wǒ bù hǎo)"));
                allQuestions.add(new Question(0, "What is 'Fine' in Chinese?", "好 (Hǎo)", "一般般 (Yìbān bān)", "不好 (Bù hǎo)", "好 (Hǎo)"));
                allQuestions.add(new Question(0, "What is 'Okay' in Chinese?", "一般般 (Yìbān bān)", "不好 (Bù hǎo)", "好 (Hǎo)", "一般般 (Yìbān bān)"));
                allQuestions.add(new Question(0, "What is 'Not good' in Chinese?", "不好 (Bù hǎo)", "好 (Hǎo)", "很棒 (Hěn bàng)", "不好 (Bù hǎo)"));
                allQuestions.add(new Question(0, "What is 'Awesome!' in Chinese?", "很棒 (Hěn bàng)", "不知道 (Bù zhīdào)", "谢谢 (Xièxiè)", "很棒 (Hěn bàng)"));
                allQuestions.add(new Question(0, "What is 'What’s your name?' in Chinese?", "你叫什么名字？ (Nǐ jiào shénme míngzì?)", "你做什么？ (Nǐ zuò shénme?)", "我叫... (Wǒ jiào...)", "你叫什么名字？ (Nǐ jiào shénme míngzì?)"));
                allQuestions.add(new Question(0, "What is 'My name is ...' in Chinese?", "我叫... (Wǒ jiào...)", "你叫什么名字？ (Nǐ jiào shénme míngzì?)", "我...岁 (Wǒ... suì)", "我叫... (Wǒ jiào...)"));
                allQuestions.add(new Question(0, "What is 'Be careful' in Chinese?", "小心 (Xiǎoxīn)", "好运 (Hǎo yùn)", "再见 (Zàijiàn)", "小心 (Xiǎoxīn)"));
                allQuestions.add(new Question(0, "What is 'Good luck' in Chinese?", "好运 (Hǎo yùn)", "小心 (Xiǎoxīn)", "再见 (Zàijiàn)", "好运 (Hǎo yùn)"));
                allQuestions.add(new Question(0, "What is 'See you soon' in Chinese?", "很快见 (Hěn kuài jiàn)", "再见 (Zàijiàn)", "拜拜 (Bàibài)", "很快见 (Hěn kuài jiàn)"));
                allQuestions.add(new Question(0, "What is 'See you later' in Chinese?", "再见 (Zàijiàn)", "很快见 (Hěn kuài jiàn)", "明天见 (Míngtiān jiàn)", "再见 (Zàijiàn)"));
                allQuestions.add(new Question(0, "What is 'What’s up?' in Chinese?", "怎么样？ (Zěnme yàng?)", "你过得怎么样？ (Nǐ guò dé zěnme yàng?)", "发生什么事？ (Fāshēng shénme shì?)", "怎么样？ (Zěnme yàng?)"));
                allQuestions.add(new Question(0, "What is 'Goodbye' in Chinese?", "再见 (Zàijiàn)", "再见 (Zàijiàn)", "拜拜 (Bàibài)", "再见 (Zàijiàn)"));
                allQuestions.add(new Question(0, "What is 'How old are you?' in Chinese?", "你几岁？ (Nǐ jǐ suì?)", "你从哪里来？ (Nǐ cóng nǎlǐ lái?)", "我...岁 (Wǒ... suì)", "你几岁？ (Nǐ jǐ suì?)"));
                allQuestions.add(new Question(0, "What is 'I am ... years old' in Chinese?", "我...岁 (Wǒ... suì)", "你几岁？ (Nǐ jǐ suì?)", "我饿了 (Wǒ èle)", "我...岁 (Wǒ... suì)"));
                allQuestions.add(new Question(0, "What is 'Where do you live?' in Chinese?", "你住在哪里？ (Nǐ zhù zài nǎlǐ?)", "你来自哪里？ (Nǐ láizì nǎlǐ?)", "现在几点？ (Xiànzài jǐ diǎn?)", "你住在哪里？ (Nǐ zhù zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "What is 'I live in ...' in Chinese?", "我住在... (Wǒ zhù zài...)", "你来自哪里？ (Nǐ láizì nǎlǐ?)", "你几岁？ (Nǐ jǐ suì?)", "我住在... (Wǒ zhù zài...)"));
                allQuestions.add(new Question(0, "What is 'Can you help me?' in Chinese?", "你能帮我吗？ (Nǐ néng bāng wǒ ma?)", "我不明白 (Wǒ bù míngbái)", "你能帮我吗？ (Nǐ néng bāng wǒ ma?)", "你能帮我吗？ (Nǐ néng bāng wǒ ma?)"));
                allQuestions.add(new Question(0, "What is 'I don’t understand' in Chinese?", "我不明白 (Wǒ bù míngbái)", "我不知道 (Wǒ bù zhīdào)", "我明白 (Wǒ míngbái)", "我不明白 (Wǒ bù míngbái)"));
                allQuestions.add(new Question(0, "What is 'Excuse me' in Chinese?", "对不起 (Duìbuqǐ)", "抱歉 (Bàoqiàn)", "对不起 (Duìbuqǐ)", "对不起 (Duìbuqǐ)"));
                allQuestions.add(new Question(0, "What is 'Thank you very much' in Chinese?", "非常感谢 (Fēicháng gǎnxiè)", "谢谢 (Xièxiè)", "不客气 (Bù kèqì)", "非常感谢 (Fēicháng gǎnxiè)"));
                allQuestions.add(new Question(0, "What is 'You're welcome' in Chinese?", "不客气 (Bù kèqì)", "谢谢 (Xièxiè)", "抱歉 (Bàoqiàn)", "不客气 (Bù kèqì)"));
                allQuestions.add(new Question(0, "What is 'Sorry for the inconvenience' in Chinese?", "抱歉给您带来的不便 (Bàoqiàn gěi nín dàilái de bùbiàn)", "对不起 (Duìbuqǐ)", "我很抱歉 (Wǒ hěn bàoqiàn)", "抱歉给您带来的不便 (Bàoqiàn gěi nín dàilái de bùbiàn)"));
                allQuestions.add(new Question(0, "What is 'What time is it?' in Chinese?", "现在几点？ (Xiànzài jǐ diǎn?)", "它是...点 (Tā shì... diǎn)", "我饿了 (Wǒ èle)", "现在几点？ (Xiànzài jǐ diǎn?)"));
                allQuestions.add(new Question(0, "What is 'It’s ... o’clock' in Chinese?", "现在...点钟 (Xiànzài... diǎn zhōng)", "现在几点？ (Xiànzài jǐ diǎn?)", "我饿了 (Wǒ èle)", "现在...点钟 (Xiànzài... diǎn zhōng)"));
                allQuestions.add(new Question(0, "What is 'I’m thirsty' in Chinese?", "我口渴 (Wǒ kǒukě)", "我饿了 (Wǒ èle)", "洗手间在哪里？ (Xǐshǒujiān zài nǎlǐ?)", "我口渴 (Wǒ kǒukě)"));
                allQuestions.add(new Question(0, "What is 'Where is the restroom?' in Chinese?", "洗手间在哪里？ (Xǐshǒujiān zài nǎlǐ?)", "餐厅在哪里？ (Cāntīng zài nǎlǐ?)", "火车站在哪里？ (Huǒchē zhàn zài nǎlǐ?)", "洗手间在哪里？ (Xǐshǒujiān zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "What is 'How much is it?' in Chinese?", "多少钱？ (Duōshǎo qián?)", "这个多少钱？ (Zhège duōshǎo qián?)", "洗手间在哪里？ (Xǐshǒujiān zài nǎlǐ?)", "多少钱？ (Duōshǎo qián?)"));
                allQuestions.add(new Question(0, "What is 'I like it' in Chinese?", "我喜欢 (Wǒ xǐhuān)", "我不喜欢 (Wǒ bù xǐhuān)", "我明白 (Wǒ míngbái)", "我喜欢 (Wǒ xǐhuān)"));
                allQuestions.add(new Question(0, "What is 'I don’t like it' in Chinese?", "我不喜欢 (Wǒ bù xǐhuān)", "我喜欢 (Wǒ xǐhuān)", "我不知道 (Wǒ bù zhīdào)", "我不喜欢 (Wǒ bù xǐhuān)"));
                allQuestions.add(new Question(0, "What is 'I understand' in Chinese?", "我明白 (Wǒ míngbái)", "我理解 (Wǒ lǐjiě)", "我不知道 (Wǒ bù zhīdào)", "我明白 (Wǒ míngbái)"));
                allQuestions.add(new Question(0, "What is 'I have no idea' in Chinese?", "我不知道 (Wǒ bù zhīdào)", "我不明白 (Wǒ bù míngbái)", "我理解 (Wǒ lǐjiě)", "我不知道 (Wǒ bù zhīdào)"));
                allQuestions.add(new Question(0, "What is 'What is this?' in Chinese?", "这是什么？ (Zhè shì shénme?)", "你怎么样？ (Nǐ zěnme yàng?)", "现在几点？ (Xiànzài jǐ diǎn?)", "这是什么？ (Zhè shì shénme?)"));


                allQuestions.add(new Question(0, "How do you say 'One' in Chinese?", "一 (Yī)", "二 (Èr)", "三 (Sān)", "一 (Yī)"));
                allQuestions.add(new Question(0, "How do you say 'Two' in Chinese?", "二 (Èr)", "三 (Sān)", "四 (Sì)", "二 (Èr)"));
                allQuestions.add(new Question(0, "How do you say 'Three' in Chinese?", "三 (Sān)", "四 (Sì)", "五 (Wǔ)", "三 (Sān)"));
                allQuestions.add(new Question(0, "How do you say 'Four' in Chinese?", "四 (Sì)", "五 (Wǔ)", "六 (Liù)", "四 (Sì)"));
                allQuestions.add(new Question(0, "How do you say 'Five' in Chinese?", "五 (Wǔ)", "六 (Liù)", "七 (Qī)", "五 (Wǔ)"));
                allQuestions.add(new Question(0, "How do you say 'Six' in Chinese?", "六 (Liù)", "七 (Qī)", "八 (Bā)", "六 (Liù)"));
                allQuestions.add(new Question(0, "How do you say 'Seven' in Chinese?", "七 (Qī)", "八 (Bā)", "九 (Jiǔ)", "七 (Qī)"));
                allQuestions.add(new Question(0, "How do you say 'Eight' in Chinese?", "八 (Bā)", "九 (Jiǔ)", "十 (Shí)", "八 (Bā)"));
                allQuestions.add(new Question(0, "How do you say 'Nine' in Chinese?", "九 (Jiǔ)", "十 (Shí)", "十五 (Shí wǔ)", "九 (Jiǔ)"));
                allQuestions.add(new Question(0, "How do you say 'Ten' in Chinese?", "十 (Shí)", "十五 (Shí wǔ)", "二十 (Èrshí)", "十 (Shí)"));
                allQuestions.add(new Question(0, "How do you say 'Twenty' in Chinese?", "二十 (Èrshí)", "三十 (Sānshí)", "四十 (Sìshí)", "二十 (Èrshí)"));
                allQuestions.add(new Question(0, "How do you say 'Thirty' in Chinese?", "三十 (Sānshí)", "四十 (Sìshí)", "五十 (Wǔshí)", "三十 (Sānshí)"));
                allQuestions.add(new Question(0, "How do you say 'Forty' in Chinese?", "四十 (Sìshí)", "五十 (Wǔshí)", "六十 (Liùshí)", "四十 (Sìshí)"));
                allQuestions.add(new Question(0, "How do you say 'Fifty' in Chinese?", "五十 (Wǔshí)", "六十 (Liùshí)", "七十 (Qīshí)", "五十 (Wǔshí)"));
                allQuestions.add(new Question(0, "How do you say 'Sixty' in Chinese?", "六十 (Liùshí)", "七十 (Qīshí)", "八十 (Bāshí)", "六十 (Liùshí)"));
                allQuestions.add(new Question(0, "How do you say 'Seventy' in Chinese?", "七十 (Qīshí)", "八十 (Bāshí)", "九十 (Jiǔshí)", "七十 (Qīshí)"));
                allQuestions.add(new Question(0, "How do you say 'Eighty' in Chinese?", "八十 (Bāshí)", "九十 (Jiǔshí)", "一百 (Yībǎi)", "八十 (Bāshí)"));
                allQuestions.add(new Question(0, "How do you say 'Ninety' in Chinese?", "九十 (Jiǔshí)", "一百 (Yībǎi)", "两百 (Liǎngbǎi)", "九十 (Jiǔshí)"));
                allQuestions.add(new Question(0, "How do you say 'Hundred' in Chinese?", "一百 (Yībǎi)", "两百 (Liǎngbǎi)", "三百 (Sānbǎi)", "一百 (Yībǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Two Hundred' in Chinese?", "两百 (Liǎngbǎi)", "三百 (Sānbǎi)", "四百 (Sìbǎi)", "两百 (Liǎngbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Three Hundred' in Chinese?", "三百 (Sānbǎi)", "四百 (Sìbǎi)", "五百 (Wǔbǎi)", "三百 (Sānbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Four Hundred' in Chinese?", "四百 (Sìbǎi)", "五百 (Wǔbǎi)", "六百 (Liùbǎi)", "四百 (Sìbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Five Hundred' in Chinese?", "五百 (Wǔbǎi)", "六百 (Liùbǎi)", "七百 (Qībǎi)", "五百 (Wǔbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Six Hundred' in Chinese?", "六百 (Liùbǎi)", "七百 (Qībǎi)", "八百 (Bābǎi)", "六百 (Liùbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Seven Hundred' in Chinese?", "七百 (Qībǎi)", "八百 (Bābǎi)", "九百 (Jiǔbǎi)", "七百 (Qībǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Eight Hundred' in Chinese?", "八百 (Bābǎi)", "九百 (Jiǔbǎi)", "一千 (Yīqiān)", "八百 (Bābǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Nine Hundred' in Chinese?", "九百 (Jiǔbǎi)", "一千 (Yīqiān)", "两千 (Liǎngqiān)", "九百 (Jiǔbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Thousand' in Chinese?", "一千 (Yīqiān)", "两千 (Liǎngqiān)", "三千 (Sānqiān)", "一千 (Yīqiān)"));

                allQuestions.add(new Question(0, "What does '你好 (Nǐ hǎo)' mean in English?", "Hello", "Goodbye", "Sorry", "Hello"));
                allQuestions.add(new Question(0, "What does '谢谢 (Xièxiè)' mean in English?", "Thank you", "Sorry", "Goodbye", "Thank you"));
                allQuestions.add(new Question(0, "What does '请 (Qǐng)' mean in English?", "Please", "Thank you", "Sorry", "Please"));
                allQuestions.add(new Question(0, "What does '我爱你 (Wǒ ài nǐ)' mean in English?", "I love you", "I like you", "Thank you", "I love you"));
                allQuestions.add(new Question(0, "What does '再见 (Zàijiàn)' mean in English?", "Goodbye", "Hello", "Thank you", "Goodbye"));
                allQuestions.add(new Question(0, "What does '对不起 (Duìbuqǐ)' mean in English?", "Sorry", "Please", "Thank you", "Sorry"));
                allQuestions.add(new Question(0, "What does '你叫什么名字? (Nǐ jiào shénme míngzì?)' mean in English?", "What is your name?", "Where are you from?", "How old are you?", "What is your name?"));
                allQuestions.add(new Question(0, "What does '今天 (Jīntiān)' mean in English?", "Today", "Tomorrow", "Yesterday", "Today"));
                allQuestions.add(new Question(0, "What does '我很累 (Wǒ hěn lèi)' mean in English?", "I am tired", "I am happy", "I am sad", "I am tired"));
                allQuestions.add(new Question(0, "What does '我饿了 (Wǒ è le)' mean in English?", "I am hungry", "I am thirsty", "I am tired", "I am hungry"));
                allQuestions.add(new Question(0, "What does '很高兴认识你 (Hěn gāoxìng rènshí nǐ)' mean in English?", "Nice to meet you", "Good morning", "Goodbye", "Nice to meet you"));
                allQuestions.add(new Question(0, "What does '这是什么 (Zhè shì shénme)' mean in English?", "What is this?", "Where is this?", "Who is this?", "What is this?"));
                allQuestions.add(new Question(0, "What does '你有时间吗? (Nǐ yǒu shíjiān ma?)' mean in English?", "Do you have time?", "How old are you?", "What time is it?", "Do you have time?"));
                allQuestions.add(new Question(0, "What does '我懂了 (Wǒ dǒng le)' mean in English?", "I understand", "I don’t understand", "I like it", "I understand"));
                allQuestions.add(new Question(0, "What does '请问 (Qǐng wèn)' mean in English?", "Excuse me", "Please", "Thank you", "Excuse me"));
                allQuestions.add(new Question(0, "What does '我有问题 (Wǒ yǒu wèntí)' mean in English?", "I have a question", "I have an answer", "I like it", "I have a question"));
                allQuestions.add(new Question(0, "What does '早安 (Zǎo'ān)' mean in English?", "Good morning", "Good night", "Hello", "Good morning"));
                allQuestions.add(new Question(0, "What does '晚安 (Wǎn'ān)' mean in English?", "Good night", "Good morning", "Goodbye", "Good night"));
                allQuestions.add(new Question(0, "What does '对不起 (Duìbuqǐ)' mean in English?", "Sorry", "Excuse me", "Please", "Sorry"));
                allQuestions.add(new Question(0, "What does '不客气 (Bù kèqì)' mean in English?", "You're welcome", "Please", "Sorry", "You're welcome"));
                allQuestions.add(new Question(0, "What does '我喜欢 (Wǒ xǐhuān)' mean in English?", "I like", "I love", "I want", "I like"));
                allQuestions.add(new Question(0, "What does '我不懂 (Wǒ bù dǒng)' mean in English?", "I don’t understand", "I understand", "I like it", "I don’t understand"));
                allQuestions.add(new Question(0, "What does '请坐 (Qǐng zuò)' mean in English?", "Please sit down", "Please stand up", "Excuse me", "Please sit down"));
                allQuestions.add(new Question(0, "What does '我累了 (Wǒ lèi le)' mean in English?", "I am tired", "I am hungry", "I am thirsty", "I am tired"));
                allQuestions.add(new Question(0, "What does '我喝水 (Wǒ hē shuǐ)' mean in English?", "I drink water", "I am thirsty", "I am hungry", "I drink water"));
                allQuestions.add(new Question(0, "What does '你吃了吗? (Nǐ chī le ma?)' mean in English?", "Have you eaten?", "How are you?", "Where are you?", "Have you eaten?"));
                allQuestions.add(new Question(0, "What does '我在这里 (Wǒ zài zhèlǐ)' mean in English?", "I am here", "I am there", "Where are you?", "I am here"));
                allQuestions.add(new Question(0, "What does '我会说中文 (Wǒ huì shuō zhōngwén)' mean in English?", "I can speak Chinese", "I don’t speak Chinese", "I can understand Chinese", "I can speak Chinese"));
                allQuestions.add(new Question(0, "What does '请问厕所在哪里? (Qǐng wèn cè suǒ zài nǎlǐ?)' mean in English?", "Excuse me, where is the bathroom?", "Excuse me, where is the restaurant?", "Excuse me, where is the hotel?", "Excuse me, where is the bathroom?"));
                allQuestions.add(new Question(0, "What does '我需要帮助 (Wǒ xūyào bāngzhù)' mean in English?", "I need help", "I want help", "I can help", "I need help"));

                Collections.shuffle(allQuestions);
                questions = new ArrayList<>(allQuestions.subList(0, 15));
                break;

            case "mediate":
                allQuestions.add(new Question(R.drawable.plate, "What is this called in Chinese?", null, null, null, "盘子 (pán zi)"));
                allQuestions.add(new Question(R.drawable.fork, "What is this called in Chinese?", null, null, null, "叉子 (chā zi)"));
                allQuestions.add(new Question(R.drawable.glass, "What is this called in Chinese?", null, null, null, "玻璃杯 (bō li bēi)"));
                allQuestions.add(new Question(R.drawable.cup, "What is this called in Chinese?", null, null, null, "杯子 (bēi zi)"));
                allQuestions.add(new Question(R.drawable.spoon, "What is this called in Chinese?", null, null, null, "勺子 (sháo zi)"));
                allQuestions.add(new Question(R.drawable.bottle, "What is this called in Chinese?", null, null, null, "瓶子 (píng zi)"));
                allQuestions.add(new Question(R.drawable.book, "What is this called in Chinese?", null, null, null, "书 (shū)"));
                allQuestions.add(new Question(R.drawable.chair, "What is this called in Chinese?", null, null, null, "椅子 (yǐ zi)"));
                allQuestions.add(new Question(R.drawable.table, "What is this called in Chinese?", null, null, null, "桌子 (zhuō zi)"));
                allQuestions.add(new Question(R.drawable.phone, "What is this called in Chinese?", null, null, null, "手机 (shǒu jī)"));
                allQuestions.add(new Question(R.drawable.laptop, "What is this called in Chinese?", null, null, null, "笔记本 (bǐ jì běn)"));
                allQuestions.add(new Question(R.drawable.desk, "What is this called in Chinese?", null, null, null, "书桌 (shū zhuō)"));
                allQuestions.add(new Question(R.drawable.computer, "What is this called in Chinese?", null, null, null, "电脑 (diàn nǎo)"));
                allQuestions.add(new Question(R.drawable.window, "What is this called in Chinese?", null, null, null, "窗户 (chuāng hù)"));
                allQuestions.add(new Question(R.drawable.door, "What is this called in Chinese?", null, null, null, "门 (mén)"));
                allQuestions.add(new Question(R.drawable.tv, "What is this called in Chinese?", null, null, null, "电视 (diàn shì)"));
                allQuestions.add(new Question(R.drawable.car, "What is this called in Chinese?", null, null, null, "汽车 (qì chē)"));
                allQuestions.add(new Question(R.drawable.bike, "What is this called in Chinese?", null, null, null, "自行车 (zì xíng chē)"));
                allQuestions.add(new Question(R.drawable.clock, "What is this called in Chinese?", null, null, null, "钟 (zhōng)"));
                allQuestions.add(new Question(R.drawable.watch, "What is this called in Chinese?", null, null, null, "手表 (shǒu biǎo)"));
                allQuestions.add(new Question(R.drawable.bag, "What is this called in Chinese?", null, null, null, "包 (bāo)"));
                allQuestions.add(new Question(R.drawable.hat, "What is this called in Chinese?", null, null, null, "帽子 (mào zi)"));
                allQuestions.add(new Question(R.drawable.shirt, "What is this called in Chinese?", null, null, null, "衬衫 (chèn shān)"));
                allQuestions.add(new Question(R.drawable.pants, "What is this called in Chinese?", null, null, null, "裤子 (kù zi)"));
                allQuestions.add(new Question(R.drawable.shoes, "What is this called in Chinese?", null, null, null, "鞋 (xié)"));
                allQuestions.add(new Question(R.drawable.jacket, "What is this called in Chinese?", null, null, null, "夹克 (jiā kè)"));
                allQuestions.add(new Question(R.drawable.glove, "What is this called in Chinese?", null, null, null, "手套 (shǒu tào)"));
                allQuestions.add(new Question(R.drawable.scarf, "What is this called in Chinese?", null, null, null, "围巾 (wéi jīn)"));
                allQuestions.add(new Question(R.drawable.shot, "What is this called in Chinese?", null, null, null, "短裤 (duǎn kù)"));
                allQuestions.add(new Question(R.drawable.glasses, "What is this called in Chinese?", null, null, null, "眼镜 (yǎn jìng)"));
                allQuestions.add(new Question(R.drawable.key, "What is this called in Chinese?", null, null, null, "钥匙 (yào shi)"));
                allQuestions.add(new Question(R.drawable.comb, "What is this called in Chinese?", null, null, null, "梳子 (shū zi)"));
                allQuestions.add(new Question(R.drawable.toothbrush, "What is this called in Chinese?", null, null, null, "牙刷 (yá shuā)"));
                allQuestions.add(new Question(R.drawable.toothpaste, "What is this called in Chinese?", null, null, null, "牙膏 (yá gāo)"));
                allQuestions.add(new Question(R.drawable.socks, "What is this called in Chinese?", null, null, null, "袜子 (wà zi)"));
                allQuestions.add(new Question(R.drawable.blanket, "What is this called in Chinese?", null, null, null, "毯子 (tǎn zi)"));
                allQuestions.add(new Question(R.drawable.towel, "What is this called in Chinese?", null, null, null, "毛巾 (máo jīn)"));
                allQuestions.add(new Question(R.drawable.pillow, "What is this called in Chinese?", null, null, null, "枕头 (zhěn tóu)"));
                allQuestions.add(new Question(R.drawable.broom, "What is this called in Chinese?", null, null, null, "扫帚 (sào zhǒu)"));

                allQuestions.add(new Question(0, "How do you pronounce 'I am learning Chinese' in Chinese?", null, null, null, "我在学习中文"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where is the nearest bus stop?' in Chinese?", null, null, null, "最近的公交车站在哪里"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good place to eat?' in Chinese?", null, null, null, "你能推荐一个好地方吃饭吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to buy a ticket' in Chinese?", null, null, null, "我需要买一张票"));
                allQuestions.add(new Question(0, "How do you pronounce 'What time does the train leave?' in Chinese?", null, null, null, "火车几点开"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to order some food' in Chinese?", null, null, null, "我想点一些食物"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where can I find a hotel?' in Chinese?", null, null, null, "我在哪里可以找到酒店"));
                allQuestions.add(new Question(0, "How do you pronounce 'I don’t understand, can you say it again?' in Chinese?", null, null, null, "我不明白，你能再说一遍吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you help me with my Chinese homework?' in Chinese?", null, null, null, "你能帮我做中文作业吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Is there a good shopping mall around here?' in Chinese?", null, null, null, "这里附近有好的购物中心吗"));


                allQuestions.add(new Question(0, "How do you pronounce 'I’m lost, can you help me find my way?' in Chinese?", null, null, null, "我迷路了，你能帮我找到路吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'How much is this item?' in Chinese?", null, null, null, "这个东西多少钱"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you tell me the way to the train station?' in Chinese?", null, null, null, "你能告诉我去火车站的路吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to book a table for four people' in Chinese?", null, null, null, "我想预定一个四人桌"));
                allQuestions.add(new Question(0, "How do you pronounce 'What time does the last bus leave?' in Chinese?", null, null, null, "最后一班公交车几点发车"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where can I find a taxi?' in Chinese?", null, null, null, "我在哪里能找到出租车"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to visit a museum today' in Chinese?", null, null, null, "我今天想去参观博物馆"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you give me directions to the shopping mall?' in Chinese?", null, null, null, "你能给我去购物中心的方向吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Do you have a map of the city?' in Chinese?", null, null, null, "你有城市的地图吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to buy a SIM card' in Chinese?", null, null, null, "我需要买个SIM卡"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good hotel here?' in Chinese?", null, null, null, "你能推荐一个好的酒店吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m looking for a pharmacy' in Chinese?", null, null, null, "我在找药店"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where is the nearest restaurant?' in Chinese?", null, null, null, "最近的餐馆在哪里"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to go to the airport' in Chinese?", null, null, null, "我需要去机场"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good place for shopping?' in Chinese?", null, null, null, "你能推荐一个好的购物地点吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Is there a post office nearby?' in Chinese?", null, null, null, "附近有邮局吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you help me with my luggage?' in Chinese?", null, null, null, "你能帮我拿行李吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to find a restroom' in Chinese?", null, null, null, "我需要找厕所"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where can I buy tickets for the train?' in Chinese?", null, null, null, "我在哪里可以买火车票"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can I pay with credit card here?' in Chinese?", null, null, null, "我可以在这里用信用卡支付吗"));





                allQuestions.add(new Question(0, "Translate 'No' into Chinese:", null, null, null, "不(Bù)"));
                allQuestions.add(new Question(0, "Translate 'Do you understand?' into Chinese:", null, null, null, "你明白吗？(Nǐ míngbái ma?)"));
                allQuestions.add(new Question(0, "Translate 'I don't understand' into Chinese:", null, null, null, "我不明白(Wǒ bù míngbái)"));
                allQuestions.add(new Question(0, "Translate 'Where?' into Chinese:", null, null, null, "哪里？(Nǎlǐ?)"));
                allQuestions.add(new Question(0, "Translate 'What?' into Chinese:", null, null, null, "什么？(Shénme?)"));
                allQuestions.add(new Question(0, "Translate 'How?' into Chinese:", null, null, null, "怎么？(Zěnme?)"));
                allQuestions.add(new Question(0, "Translate 'How much?' into Chinese:", null, null, null, "多少钱？(Duōshǎo qián?)"));
                allQuestions.add(new Question(0, "Translate 'When?' into Chinese:", null, null, null, "什么时候？(Shénme shíhòu?)"));
                allQuestions.add(new Question(0, "Translate 'Who?' into Chinese:", null, null, null, "谁？(Shéi?)"));
                allQuestions.add(new Question(0, "Translate 'Why?' into Chinese:", null, null, null, "为什么？(Wèishéme?)"));
                allQuestions.add(new Question(0, "Translate 'Thank you' into Chinese:", null, null, null, "谢谢(Xièxiè)"));
                allQuestions.add(new Question(0, "Translate 'I'm sorry' into Chinese:", null, null, null, "对不起(Duìbùqǐ)"));
                allQuestions.add(new Question(0, "Translate 'Congratulations!' into Chinese:", null, null, null, "恭喜(Gōngxǐ)"));
                allQuestions.add(new Question(0, "Translate 'It's okay' into Chinese:", null, null, null, "没关系(Méi guānxi)"));
                allQuestions.add(new Question(0, "Translate 'I don't know' into Chinese:", null, null, null, "我不知道(Wǒ bù zhīdào)"));
                allQuestions.add(new Question(0, "Translate 'I don't like it' into Chinese:", null, null, null, "我不喜欢(Wǒ bù xǐhuān)"));
                allQuestions.add(new Question(0, "Translate 'I like it' into Chinese:", null, null, null, "我喜欢(Wǒ xǐhuān)"));
                allQuestions.add(new Question(0, "Translate 'You're welcome' into Chinese:", null, null, null, "不客气(Bù kèqì)"));
                allQuestions.add(new Question(0, "Translate 'I think that...' into Chinese:", null, null, null, "我认为(Wǒ rènwéi)"));
                allQuestions.add(new Question(0, "Translate 'No, thank you' into Chinese:", null, null, null, "不，谢谢(Bù, xièxiè)"));
                allQuestions.add(new Question(0, "Translate 'Excuse me' into Chinese:", null, null, null, "对不起(Duìbùqǐ)"));
                allQuestions.add(new Question(0, "Translate 'Take care' into Chinese:", null, null, null, "保重(Bǎozhòng)"));
                allQuestions.add(new Question(0, "Translate 'Don't forget' into Chinese:", null, null, null, "别忘了(Bié wàngle)"));
                allQuestions.add(new Question(0, "Translate 'How do you pronounce this?' into Chinese:", null, null, null, "这个怎么读？(Zhège zěnme dú?)"));
                allQuestions.add(new Question(0, "Translate 'Before' into Chinese:", null, null, null, "之前(Zhīqián)"));
                allQuestions.add(new Question(0, "Translate 'After' into Chinese:", null, null, null, "之后(Zhīhòu)"));
                allQuestions.add(new Question(0, "Translate 'Wrong' into Chinese:", null, null, null, "错(Cuò)"));
                allQuestions.add(new Question(0, "Translate 'Right' into Chinese:", null, null, null, "对(Duì)"));
                allQuestions.add(new Question(0, "Translate 'Until' into Chinese:", null, null, null, "直到(Zhídào)"));
                allQuestions.add(new Question(0, "Translate 'Where is the toilet?' into Chinese:", null, null, null, "洗手间在哪里？(Xǐshǒujiān zài nǎlǐ?)"));
                allQuestions.add(new Question(0, "Translate 'Do you live here?' into Chinese:", null, null, null, "你住这里吗？(Nǐ zhù zhèlǐ ma?)"));
                allQuestions.add(new Question(0, "Translate 'Do you like it?' into Chinese:", null, null, null, "你喜欢吗？(Nǐ xǐhuān ma?)"));
                allQuestions.add(new Question(0, "Translate 'I love it' into Chinese:", null, null, null, "我爱它(Wǒ ài tā)"));
                allQuestions.add(new Question(0, "Translate 'On business' into Chinese:", null, null, null, "商务出差(Shāngwù chūchāi)"));
                allQuestions.add(new Question(0, "Translate 'What happened?' into Chinese:", null, null, null, "发生了什么事？(Fāshēngle shénme shì?)"));
                allQuestions.add(new Question(0, "Translate 'Do you need help?' into Chinese:", null, null, null, "你需要帮助吗？(Nǐ xūyào bāngzhù ma?)"));
                allQuestions.add(new Question(0, "Translate 'I'm lost' into Chinese:", null, null, null, "我迷路了(Wǒ mílùle)"));
                allQuestions.add(new Question(0, "Translate 'What time is it?' into Chinese:", null, null, null, "现在几点钟？(Xiànzài jǐ diǎn zhōng?)"));
                allQuestions.add(new Question(0, "Translate 'I want to go there' into Chinese:", null, null, null, "我想去那里(Wǒ xiǎng qù nàlǐ)"));
                allQuestions.add(new Question(0, "Translate 'How far is it?' into Chinese:", null, null, null, "有多远？(Yǒu duō yuǎn?)"));
                allQuestions.add(new Question(0, "Translate 'Can I help you?' into Chinese:", null, null, null, "我可以帮你吗？(Wǒ kěyǐ bāng nǐ ma?)"));
                allQuestions.add(new Question(0, "Translate 'Do you speak English?' into Chinese:", null, null, null, "你会说英语吗？(Nǐ huì shuō yīngyǔ ma?)"));
                allQuestions.add(new Question(0, "Translate 'I need a doctor' into Chinese:", null, null, null, "我需要医生(Wǒ xūyào yīshēng)"));
                allQuestions.add(new Question(0, "Translate 'Call the police' into Chinese:", null, null, null, "打电话给警察(Dǎ diànhuà gěi jǐngchá)"));


                allQuestions.add(new Question(0, "What is 'Red' in Chinese?", null, null, null, "红色(Hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Blue' in Chinese?", null, null, null, "蓝色(Lán sè)"));
                allQuestions.add(new Question(0, "What is 'Green' in Chinese?", null, null, null, "绿色(Lǜ sè)"));
                allQuestions.add(new Question(0, "What is 'Yellow' in Chinese?", null, null, null, "黄色(Huáng sè)"));
                allQuestions.add(new Question(0, "What is 'Black' in Chinese?", null, null, null, "黑色(Hēi sè)"));
                allQuestions.add(new Question(0, "What is 'White' in Chinese?", null, null, null, "白色(Bái sè)"));
                allQuestions.add(new Question(0, "What is 'Pink' in Chinese?", null, null, null, "粉色(Fěn sè)"));
                allQuestions.add(new Question(0, "What is 'Purple' in Chinese?", null, null, null, "紫色(Zǐ sè)"));
                allQuestions.add(new Question(0, "What is 'Orange' in Chinese?", null, null, null, "橙色(Chéng sè)"));
                allQuestions.add(new Question(0, "What is 'Brown' in Chinese?", null, null, null, "棕色(Zōng sè)"));
                allQuestions.add(new Question(0, "What is 'Gray' in Chinese?", null, null, null, "灰色(Huī sè)"));
                allQuestions.add(new Question(0, "What is 'Beige' in Chinese?", null, null, null, "米色(Mǐ sè)"));
                allQuestions.add(new Question(0, "What is 'Turquoise' in Chinese?", null, null, null, "绿松石(Lǜ sōng shí)"));
                allQuestions.add(new Question(0, "What is 'Gold' in Chinese?", null, null, null, "金色(Jīn sè)"));
                allQuestions.add(new Question(0, "What is 'Silver' in Chinese?", null, null, null, "银色(Yín sè)"));
                allQuestions.add(new Question(0, "What is 'Copper' in Chinese?", null, null, null, "铜色(Tóng sè)"));
                allQuestions.add(new Question(0, "What is 'Bronze' in Chinese?", null, null, null, "青铜色(Qīng tóng sè)"));
                allQuestions.add(new Question(0, "What is 'Magenta' in Chinese?", null, null, null, "品红色(Pǐn hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Violet' in Chinese?", null, null, null, "紫罗兰色(Zǐ luó lán sè)"));
                allQuestions.add(new Question(0, "What is 'Indigo' in Chinese?", null, null, null, "靛蓝色(Diàn lán sè)"));
                allQuestions.add(new Question(0, "What is 'Lavender' in Chinese?", null, null, null, "薰衣草色(Xūn yī cǎo sè)"));
                allQuestions.add(new Question(0, "What is 'Peach' in Chinese?", null, null, null, "桃色(Táo sè)"));
                allQuestions.add(new Question(0, "What is 'Ivory' in Chinese?", null, null, null, "象牙色(Xiàng yá sè)"));
                allQuestions.add(new Question(0, "What is 'Cream' in Chinese?", null, null, null, "奶油色(Nǎi yóu sè)"));
                allQuestions.add(new Question(0, "What is 'Charcoal' in Chinese?", null, null, null, "木炭色(Mù tàn sè)"));
                allQuestions.add(new Question(0, "What is 'Emerald' in Chinese?", null, null, null, "翡翠色(Fěi cuì sè)"));
                allQuestions.add(new Question(0, "What is 'Cyan' in Chinese?", null, null, null, "青色(Qīng sè)"));
                allQuestions.add(new Question(0, "What is 'Mint' in Chinese?", null, null, null, "薄荷绿(Bò hé lǜ)"));
                allQuestions.add(new Question(0, "What is 'Lime' in Chinese?", null, null, null, "酸橙色(Suān chéng sè)"));
                allQuestions.add(new Question(0, "What is 'Plum' in Chinese?", null, null, null, "李子色(Lǐ zi sè)"));
                allQuestions.add(new Question(0, "What is 'Coral' in Chinese?", null, null, null, "珊瑚色(Shān hú sè)"));
                allQuestions.add(new Question(0, "What is 'Tan' in Chinese?", null, null, null, "褐色(Hè sè)"));
                allQuestions.add(new Question(0, "What is 'Rust' in Chinese?", null, null, null, "铁锈色(Tiě xiù sè)"));
                allQuestions.add(new Question(0, "What is 'Sapphire' in Chinese?", null, null, null, "蓝宝石色(Lán bǎo shí sè)"));
                allQuestions.add(new Question(0, "What is 'Scarlet' in Chinese?", null, null, null, "猩红色(Xīng hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Burgundy' in Chinese?", null, null, null, "酒红色(Jiǔ hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Azure' in Chinese?", null, null, null, "天蓝色(Tiān lán sè)"));
                allQuestions.add(new Question(0, "What is 'Seafoam' in Chinese?", null, null, null, "海藻绿(Hǎi zǎo lǜ)"));
                allQuestions.add(new Question(0, "What is 'Sunset' in Chinese?", null, null, null, "日落色(Rì luò sè)"));
                allQuestions.add(new Question(0, "What is 'Fuchsia' in Chinese?", null, null, null, "紫红色(Zǐ hóng sè)"));
                allQuestions.add(new Question(0, "What is 'Rose' in Chinese?", null, null, null, "玫瑰色(Méi guī sè)"));
                allQuestions.add(new Question(0, "What is 'Jade' in Chinese?", null, null, null, "玉色(Yù sè)"));
                allQuestions.add(new Question(0, "What is 'Onyx' in Chinese?", null, null, null, "缟色(Gǎo sè)"));
                allQuestions.add(new Question(0, "What is 'Chartreuse' in Chinese?", null, null, null, "查特鲁斯绿(Chá tè lǔ sī lǜ)"));
                allQuestions.add(new Question(0, "What is 'Wisteria' in Chinese?", null, null, null, "紫藤色(Zǐ téng sè)"));
                allQuestions.add(new Question(0, "What is 'Auburn' in Chinese?", null, null, null, "赤褐色(Chì hè sè)"));
                allQuestions.add(new Question(0, "What is 'Khaki' in Chinese?", null, null, null, "卡其色(Kǎ qí sè)"));
                allQuestions.add(new Question(0, "What is 'Chocolate' in Chinese?", null, null, null, "巧克力色(Qiǎo kè lì sè)"));
                allQuestions.add(new Question(0, "What is 'Carmine' in Chinese?", null, null, null, "胭脂红(Yān zhī hóng)"));

                allQuestions.add(new Question(0, "What is 'Bank' in Chinese?", null, null, null, "银行 (Yínháng)"));
                allQuestions.add(new Question(0, "What is 'Book' in Chinese?", null, null, null, "书 (Shū)"));
                allQuestions.add(new Question(0, "Translate this: 'Table for three' into Chinese:", null, null, null, "三人桌 (Sān rén zhuō)"));
                allQuestions.add(new Question(0, "Translate this: 'Check, please' into Chinese:", null, null, null, "账单, 请 (Zhàngdān, qǐng)"));
                allQuestions.add(new Question(0, "What is 'What do you recommend?' in Chinese?", null, null, null, "你推荐什么? (Nǐ tuījiàn shénme?)"));
                allQuestions.add(new Question(0, "Translate this: 'Juice' into Chinese:", null, null, null, "果汁 (Guǒzhī)"));
                allQuestions.add(new Question(0, "What is 'Can I have the bill?' in Chinese?", null, null, null, "我可以拿账单吗? (Wǒ kěyǐ ná zhàngdān ma?)"));
                allQuestions.add(new Question(0, "What is 'I’m allergic to seafood' in Chinese?", null, null, null, "我对海鲜过敏 (Wǒ duì hǎixiān guòmǐn)"));
                allQuestions.add(new Question(0, "Translate this: 'Can we split the bill?' into Chinese:", null, null, null, "我们可以分开付账吗? (Wǒmen kěyǐ fēnkāi fù zhàng ma?)"));
                allQuestions.add(new Question(0, "What is 'Do you have a vegetarian option?' in Chinese?", null, null, null, "你有素食选项吗? (Nǐ yǒu sùshí xuǎnxiàng ma?)"));
                allQuestions.add(new Question(0, "What is 'I’d like to order coffee' in Chinese?", null, null, null, "我想点咖啡 (Wǒ xiǎng diǎn kāfēi)"));
                allQuestions.add(new Question(0, "What is 'What’s in this dish?' in Chinese?", null, null, null, "这道菜有什么? (Zhè dào cài yǒu shénme?)"));
                allQuestions.add(new Question(0, "What is 'Is this dessert gluten-free?' in Chinese?", null, null, null, "这个甜点不含麸质吗? (Zhège tiándiǎn bù hán fū zhì ma?)"));
                allQuestions.add(new Question(0, "Translate this: 'I’ll have the pasta' into Chinese:", null, null, null, "我要意大利面 (Wǒ yào yìdàlì miàn)"));
                allQuestions.add(new Question(0, "What is 'Do you have vegetarian dessert?' in Chinese?", null, null, null, "你有素食甜点吗? (Nǐ yǒu sùshí tiándiǎn ma?)"));
                allQuestions.add(new Question(0, "Translate this: 'Can I take this to go?' into Chinese:", null, null, null, "我可以带走吗? (Wǒ kěyǐ dài zǒu ma?)"));
                allQuestions.add(new Question(0, "What is 'What is today’s special?' in Chinese?", null, null, null, "今天的特别菜是什么? (Jīntiān de tèbié cài shì shénme?)"));
                allQuestions.add(new Question(0, "What is 'How much does this cost?' in Chinese?", null, null, null, "这个多少钱? (Zhège duōshǎo qián?)"));
                allQuestions.add(new Question(0, "What is 'I need a table for five' in Chinese?", null, null, null, "我需要一张五人桌 (Wǒ xūyào yī zhāng wǔ rén zhuō)"));
                allQuestions.add(new Question(0, "What is 'Can I have a glass of wine?' in Chinese?", null, null, null, "我可以要一杯酒吗? (Wǒ kěyǐ yào yī bēi jiǔ ma?)"));
                allQuestions.add(new Question(0, "Translate this: 'What do you have on tap?' into Chinese:", null, null, null, "你们有哪种生啤? (Nǐmen yǒu nǎ zhǒng shēng pí?)"));
                allQuestions.add(new Question(0, "What is 'I’m just browsing' in Chinese?", null, null, null, "我只是随便看看 (Wǒ zhǐshì suíbiàn kàn kàn)"));
                allQuestions.add(new Question(0, "What is 'Can I have it without garlic?' in Chinese?", null, null, null, "我可以不要大蒜吗? (Wǒ kěyǐ bù yào dàsuàn ma?)"));
                allQuestions.add(new Question(0, "What is 'I’ll have the soup' in Chinese?", null, null, null, "我要汤 (Wǒ yào tāng)"));
                allQuestions.add(new Question(0, "What is 'Is it dairy-free?' in Chinese?", null, null, null, "这个不含乳制品吗? (Zhège bù hán rǔ zhìpǐn ma?)"));
                allQuestions.add(new Question(0, "What is 'I’d like my steak medium-rare' in Chinese?", null, null, null, "我想要我的牛排中等偏生 (Wǒ xiǎng yào wǒ de niúpái zhōngděng piān shēng)"));
                allQuestions.add(new Question(0, "Translate this: 'Can I have some extra napkins?' into Chinese:", null, null, null, "我可以要一些额外的纸巾吗? (Wǒ kěyǐ yào yīxiē éwài de zhǐjīn ma?)"));

                allQuestions.add(new Question(0, "How do you say 'One' in Chinese?", null, null, null, "一 (Yī)"));
                allQuestions.add(new Question(0, "How do you say 'Two' in Chinese?", null, null, null, "二 (Èr)"));
                allQuestions.add(new Question(0, "How do you say 'Three' in Chinese?", null, null, null, "三 (Sān)"));
                allQuestions.add(new Question(0, "How do you say 'Four' in Chinese?", null, null, null, "四 (Sì)"));
                allQuestions.add(new Question(0, "How do you say 'Five' in Chinese?", null, null, null, "五 (Wǔ)"));
                allQuestions.add(new Question(0, "How do you say 'Six' in Chinese?", null, null, null, "六 (Liù)"));
                allQuestions.add(new Question(0, "How do you say 'Seven' in Chinese?", null, null, null, "七 (Qī)"));
                allQuestions.add(new Question(0, "How do you say 'Eight' in Chinese?", null, null, null, "八 (Bā)"));
                allQuestions.add(new Question(0, "How do you say 'Nine' in Chinese?", null, null, null, "九 (Jiǔ)"));
                allQuestions.add(new Question(0, "How do you say 'Ten' in Chinese?", null, null, null, "十 (Shí)"));
                allQuestions.add(new Question(0, "How do you say 'Twenty' in Chinese?", null, null, null, "二十 (Èrshí)"));
                allQuestions.add(new Question(0, "How do you say 'Thirty' in Chinese?", null, null, null, "三十 (Sānshí)"));
                allQuestions.add(new Question(0, "How do you say 'Forty' in Chinese?", null, null, null, "四十 (Sìshí)"));
                allQuestions.add(new Question(0, "How do you say 'Fifty' in Chinese?", null, null, null, "五十 (Wǔshí)"));
                allQuestions.add(new Question(0, "How do you say 'Sixty' in Chinese?", null, null, null, "六十 (Liùshí)"));
                allQuestions.add(new Question(0, "How do you say 'Seventy' in Chinese?", null, null, null, "七十 (Qīshí)"));
                allQuestions.add(new Question(0, "How do you say 'Eighty' in Chinese?", null, null, null, "八十 (Bāshí)"));
                allQuestions.add(new Question(0, "How do you say 'Ninety' in Chinese?", null, null, null, "九十 (Jiǔshí)"));
                allQuestions.add(new Question(0, "How do you say 'Hundred' in Chinese?", null, null, null, "百 (Bǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Two Hundred' in Chinese?", null, null, null, "二百 (Èrbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Three Hundred' in Chinese?", null, null, null, "三百 (Sānbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Four Hundred' in Chinese?", null, null, null, "四百 (Sìbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Five Hundred' in Chinese?", null, null, null, "五百 (Wǔbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Six Hundred' in Chinese?", null, null, null, "六百 (Liùbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Seven Hundred' in Chinese?", null, null, null, "七百 (Qībāi)"));
                allQuestions.add(new Question(0, "How do you say 'Eight Hundred' in Chinese?", null, null, null, "八百 (Bābǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Nine Hundred' in Chinese?", null, null, null, "九百 (Jiǔbǎi)"));
                allQuestions.add(new Question(0, "How do you say 'Thousand' in Chinese?", null, null, null, "千 (Qiān)"));

                allQuestions.add(new Question(0, "What is 'Do you have any organic products?' in Chinese?", null, null, null, "你有有机产品吗?"));
                allQuestions.add(new Question(0, "What is 'Can I pay with a credit card?' in Chinese?", null, null, null, "我可以用信用卡付款吗?"));
                allQuestions.add(new Question(0, "What is 'Where are the bathroom products?' in Chinese?", null, null, null, "浴室产品在哪里?"));
                allQuestions.add(new Question(0, "What is 'Do you have any vegan options?' in Chinese?", null, null, null, "你有素食选择吗?"));
                allQuestions.add(new Question(0, "What is 'How much is this item?' in Chinese?", null, null, null, "这个物品多少钱?"));
                allQuestions.add(new Question(0, "What is 'Where can I find the milk?' in Chinese?", null, null, null, "我在哪里可以找到牛奶?"));
                allQuestions.add(new Question(0, "What is 'Do you have any gluten-free snacks?' in Chinese?", null, null, null, "你有无麸质零食吗?"));
                allQuestions.add(new Question(0, "What is 'Can I try this on?' in Chinese?", null, null, null, "我可以试穿这个吗?"));
                allQuestions.add(new Question(0, "What is 'Do you have any discounted items?' in Chinese?", null, null, null, "你有打折商品吗?"));
                allQuestions.add(new Question(0, "What is 'Are these items on sale?' in Chinese?", null, null, null, "这些商品在促销吗?"));
                allQuestions.add(new Question(0, "What is 'Where can I find the shoes?' in Chinese?", null, null, null, "我在哪里可以找到鞋子?"));
                allQuestions.add(new Question(0, "What is 'Do you have any sugar-free options?' in Chinese?", null, null, null, "你有无糖选择吗?"));
                allQuestions.add(new Question(0, "What is 'Is this item available in other colors?' in Chinese?", null, null, null, "这个物品有其他颜色吗?"));
                allQuestions.add(new Question(0, "What is 'Can I get a refund?' in Chinese?", null, null, null, "我可以退款吗?"));
                allQuestions.add(new Question(0, "What is 'Where is the customer service desk?' in Chinese?", null, null, null, "客户服务台在哪里?"));
                allQuestions.add(new Question(0, "What is 'Do you have any seasonal items?' in Chinese?", null, null, null, "你有季节性商品吗?"));
                allQuestions.add(new Question(0, "What is 'Is this item in stock?' in Chinese?", null, null, null, "这个物品有库存吗?"));
                allQuestions.add(new Question(0, "What is 'Do you have any clothing for kids?' in Chinese?", null, null, null, "你有儿童衣服吗?"));
                allQuestions.add(new Question(0, "What is 'Do you have any large sizes?' in Chinese?", null, null, null, "你有大号尺寸吗?"));
                allQuestions.add(new Question(0, "What is 'Is this a new product?' in Chinese?", null, null, null, "这是新产品吗?"));
                allQuestions.add(new Question(0, "What is 'Do you offer free shipping?' in Chinese?", null, null, null, "你们提供免费送货吗?"));
                allQuestions.add(new Question(0, "What is 'Where is the checkout?' in Chinese?", null, null, null, "结账在哪里?"));
                allQuestions.add(new Question(0, "What is 'Can I get this delivered?' in Chinese?", null, null, null, "我可以让它送货吗?"));
                allQuestions.add(new Question(0, "What is 'Do you have any promotional offers?' in Chinese?", null, null, null, "你有促销优惠吗?"));
                allQuestions.add(new Question(0, "What is 'Can I use a discount code?' in Chinese?", null, null, null, "我可以使用折扣码吗?"));
                allQuestions.add(new Question(0, "What is 'Is there a warranty for this item?' in Chinese?", null, null, null, "这个物品有保修吗?"));
                allQuestions.add(new Question(0, "What is 'Do you have any gift cards?' in Chinese?", null, null, null, "你有礼品卡吗?"));
                allQuestions.add(new Question(0, "What is 'Do you offer home delivery?' in Chinese?", null, null, null, "你们提供送货上门服务吗?"));


                allQuestions.add(new Question(0, "What is '你有电子产品吗? (Nǐ yǒu diànzǐ chǎnpǐn ma?)' in English?", null, null, null, "Do you have any electronic gadgets?"));
                allQuestions.add(new Question(0, "What is '我可以得到礼品收据吗? (Wǒ kěyǐ dédào lǐpǐn shōujù ma?)' in English?", null, null, null, "Can I get a gift receipt?"));
                allQuestions.add(new Question(0, "What is '我在哪里可以找到美容产品? (Wǒ zài nǎlǐ kěyǐ zhǎodào měiróng chǎnpǐn?)' in English?", null, null, null, "Where can I find the beauty products?"));
                allQuestions.add(new Question(0, "What is '你有环保产品吗? (Nǐ yǒu huánbǎo chǎnpǐn ma?)' in English?", null, null, null, "Do you have any environmentally-friendly products?"));
                allQuestions.add(new Question(0, "What is '我可以退货吗? (Wǒ kěyǐ tuìhuò ma?)' in English?", null, null, null, "Can I return this item?"));
                allQuestions.add(new Question(0, "What is '你有清仓商品吗? (Nǐ yǒu qīngcāng shāngpǐn ma?)' in English?", null, null, null, "Do you have any clearance items?"));
                allQuestions.add(new Question(0, "What is '厨房电器在哪里? (Chúfáng diànqì zài nǎlǐ?)' in English?", null, null, null, "Where are the kitchen appliances?"));
                allQuestions.add(new Question(0, "What is '我可以批量购买打折吗? (Wǒ kěyǐ pīliàng gòumǎi dǎzhé ma?)' in English?", null, null, null, "Can I get a discount for bulk orders?"));
                allQuestions.add(new Question(0, "What is '有尺码指南吗? (Yǒu chǐmǎ zhǐnán ma?)' in English?", null, null, null, "Is there a size guide?"));
                allQuestions.add(new Question(0, "What is '你有预定商品吗? (Nǐ yǒu yùdìng shāngpǐn ma?)' in English?", null, null, null, "Do you have any items on preorder?"));
                allQuestions.add(new Question(0, "What is '书籍在哪里? (Shūjí zài nǎlǐ?)' in English?", null, null, null, "Where are the books?"));
                allQuestions.add(new Question(0, "What is '我可以把这个物品包装成礼物吗? (Wǒ kěyǐ bǎ zhège wùpǐn bāozhuāng chéng lǐwù ma?)' in English?", null, null, null, "Can I get this item gift-wrapped?"));
                allQuestions.add(new Question(0, "What is '你有家居装饰吗? (Nǐ yǒu jiājū zhuāngshì ma?)' in English?", null, null, null, "Do you have any home decor?"));
                allQuestions.add(new Question(0, "What is '电子配件在哪里? (Diànzǐ pèijiàn zài nǎlǐ?)' in English?", null, null, null, "Where are the electronic accessories?"));
                allQuestions.add(new Question(0, "What is '我可以换个尺寸吗? (Wǒ kěyǐ huàngè chǐcùn ma?)' in English?", null, null, null, "Can I get this in a different size?"));
                allQuestions.add(new Question(0, "What is '你有食品吗? (Nǐ yǒu shípǐn ma?)' in English?", null, null, null, "Do you have any food items?"));
                allQuestions.add(new Question(0, "What is '香水在哪里? (Xiāngshuǐ zài nǎlǐ?)' in English?", null, null, null, "Where are the perfumes?"));
                allQuestions.add(new Question(0, "What is '这是最终价格吗? (Zhè shì zuìzhōng jiàgé ma?)' in English?", null, null, null, "Is this the final price?"));
                allQuestions.add(new Question(0, "What is '你们提供礼品包装吗? (Nǐmen tígōng lǐpǐn bāozhuāng ma?)' in English?", null, null, null, "Do you offer gift wrapping?"));
                allQuestions.add(new Question(0, "What is '这个物品是缺货的吗? (Zhège wùpǐn shì quēhuò de ma?)' in English?", null, null, null, "Is this item on backorder?"));
                allQuestions.add(new Question(0, "What is '我可以分期付款吗? (Wǒ kěyǐ fēnqī fùkuǎn ma?)' in English?", null, null, null, "Can I pay in installments?"));
                allQuestions.add(new Question(0, "What is '你有宠物用品吗? (Nǐ yǒu chǒngwù yòngpǐn ma?)' in English?", null, null, null, "Do you have any pet supplies?"));


                allQuestions.add(new Question(0, "What does '我很喜欢这个地方 (Wǒ hěn xǐhuān zhège dìfāng)' mean in English?", null, null, null, "I really like this place"));
                allQuestions.add(new Question(0, "What does '你会开车吗? (Nǐ huì kāi chē ma?)' mean in English?", null, null, null, "Can you drive?"));
                allQuestions.add(new Question(0, "What does '她正在学习中文 (Tā zhèngzài xuéxí zhōngwén)' mean in English?", null, null, null, "She is studying Chinese"));
                allQuestions.add(new Question(0, "What does '我明天去超市 (Wǒ míngtiān qù chāoshì)' mean in English?", null, null, null, "I am going to the supermarket tomorrow"));
                allQuestions.add(new Question(0, "What does '我今天早上迟到了 (Wǒ jīntiān zǎoshang chídào le)' mean in English?", null, null, null, "I was late this morning"));
                allQuestions.add(new Question(0, "What does '我们可以一起吃饭吗? (Wǒmen kěyǐ yīqǐ chīfàn ma?)' mean in English?", null, null, null, "Can we eat together?"));
                allQuestions.add(new Question(0, "What does '你喜欢什么颜色? (Nǐ xǐhuān shénme yánsè?)' mean in English?", null, null, null, "What color do you like?"));
                allQuestions.add(new Question(0, "What does '我昨天去了博物馆 (Wǒ zuótiān qù le bówùguǎn)' mean in English?", null, null, null, "I went to the museum yesterday"));
                allQuestions.add(new Question(0, "What does '请帮我一下 (Qǐng bāng wǒ yīxià)' mean in English?", null, null, null, "Please help me a little"));
                allQuestions.add(new Question(0, "What does '我有很多问题 (Wǒ yǒu hěn duō wèntí)' mean in English?", null, null, null, "I have many questions"));
                allQuestions.add(new Question(0, "What does '他很高兴见到你 (Tā hěn gāoxìng jiàn dào nǐ)' mean in English?", null, null, null, "He is very happy to meet you"));
                allQuestions.add(new Question(0, "What does '我不能吃辣 (Wǒ bù néng chī là)' mean in English?", null, null, null, "I cannot eat spicy food"));
                allQuestions.add(new Question(0, "What does '你去哪儿了? (Nǐ qù nǎr le?)' mean in English?", null, null, null, "Where did you go?"));
                allQuestions.add(new Question(0, "What does '我有一个好主意 (Wǒ yǒu yīgè hǎo zhǔyì)' mean in English?", null, null, null, "I have a good idea"));
                allQuestions.add(new Question(0, "What does '我不太喜欢运动 (Wǒ bù tài xǐhuān yùndòng)' mean in English?", null, null, null, "I don’t really like sports"));
                allQuestions.add(new Question(0, "What does '我们几点见面? (Wǒmen jǐ diǎn jiànmiàn?)' mean in English?", null, null, null, "What time shall we meet?"));
                allQuestions.add(new Question(0, "What does '我在学中文 (Wǒ zài xué zhōngwén)' mean in English?", null, null, null, "I am learning Chinese"));
                allQuestions.add(new Question(0, "What does '你可以告诉我怎么去吗? (Nǐ kěyǐ gàosù wǒ zěnme qù ma?)' mean in English?", null, null, null, "Can you tell me how to get there?"));
                allQuestions.add(new Question(0, "What does '他昨天没来上班 (Tā zuótiān méi lái shàngbān)' mean in English?", null, null, null, "He didn’t come to work yesterday"));
                allQuestions.add(new Question(0, "What does '你喜欢吃中餐吗? (Nǐ xǐhuān chī zhōngcān ma?)' mean in English?", null, null, null, "Do you like to eat Chinese food?"));
                allQuestions.add(new Question(0, "What does '我们应该早一点出发 (Wǒmen yīnggāi zǎo yīdiǎn chūfā)' mean in English?", null, null, null, "We should leave a little earlier"));
                allQuestions.add(new Question(0, "What does '今天是个特别的日子 (Jīntiān shì gè tèbié de rìzi)' mean in English?", null, null, null, "Today is a special day"));
                allQuestions.add(new Question(0, "What does '这张照片很漂亮 (Zhè zhāng zhàopiàn hěn piàoliang)' mean in English?", null, null, null, "This photo is very beautiful"));
                allQuestions.add(new Question(0, "What does '我希望你喜欢这本书 (Wǒ xīwàng nǐ xǐhuān zhè běn shū)' mean in English?", null, null, null, "I hope you like this book"));
                allQuestions.add(new Question(0, "What does '你知道哪里有出租车吗? (Nǐ zhīdào nǎlǐ yǒu chūzūchē ma?)' mean in English?", null, null, null, "Do you know where there are taxis?"));
                allQuestions.add(new Question(0, "What does '这个星期我很忙 (Zhège xīngqī wǒ hěn máng)' mean in English?", null, null, null, "I am very busy this week"));
                allQuestions.add(new Question(0, "What does '我去过很多地方 (Wǒ qù guò hěn duō dìfāng)' mean in English?", null, null, null, "I have been to many places"));
                allQuestions.add(new Question(0, "What does '他昨天没有电话 (Tā zuótiān méiyǒu diànhuà)' mean in English?", null, null, null, "He didn’t make a call yesterday"));
                allQuestions.add(new Question(0, "What does '你觉得这个怎么样? (Nǐ juéde zhège zěnme yàng?)' mean in English?", null, null, null, "What do you think about this?"));
                allQuestions.add(new Question(0, "What does '我明天会去旅行 (Wǒ míngtiān huì qù lǚxíng)' mean in English?", null, null, null, "I will go travel tomorrow"));
                allQuestions.add(new Question(0, "What does '她今天很高兴 (Tā jīntiān hěn gāoxìng)' mean in English?", null, null, null, "She is very happy today"));
                allQuestions.add(new Question(0, "What does '我找不到我的书 (Wǒ zhǎo bù dào wǒ de shū)' mean in English?", null, null, null, "I can’t find my book"));
                allQuestions.add(new Question(0, "What does '你会做饭吗? (Nǐ huì zuò fàn ma?)' mean in English?", null, null, null, "Can you cook?"));
                allQuestions.add(new Question(0, "What does '我们可以一起去看电影吗? (Wǒmen kěyǐ yīqǐ qù kàn diànyǐng ma?)' mean in English?", null, null, null, "Can we go watch a movie together?"));
                allQuestions.add(new Question(0, "What does '他正在工作 (Tā zhèngzài gōngzuò)' mean in English?", null, null, null, "He is working"));
                allQuestions.add(new Question(0, "What does '这个问题很复杂 (Zhège wèntí hěn fùzá)' mean in English?", null, null, null, "This problem is very complicated"));
                allQuestions.add(new Question(0, "What does '我喜欢在晚上散步 (Wǒ xǐhuān zài wǎnshàng sànbù)' mean in English?", null, null, null, "I like to walk in the evening"));
                allQuestions.add(new Question(0, "What does '你可以再说一遍吗? (Nǐ kěyǐ zài shuō yībiàn ma?)' mean in English?", null, null, null, "Can you say it again?"));
                allQuestions.add(new Question(0, "What does '我们去哪里旅行? (Wǒmen qù nǎlǐ lǚxíng?)' mean in English?", null, null, null, "Where shall we travel?"));
                allQuestions.add(new Question(0, "What does '今天的天气怎么样? (Jīntiān de tiānqì zěnme yàng?)' mean in English?", null, null, null, "How’s the weather today?"));
                allQuestions.add(new Question(0, "What does '我需要更多的时间 (Wǒ xūyào gèng duō de shíjiān)' mean in English?", null, null, null, "I need more time"));
                allQuestions.add(new Question(0, "What does '你会说其他语言吗? (Nǐ huì shuō qítā yǔyán ma?)' mean in English?", null, null, null, "Can you speak other languages?"));
                allQuestions.add(new Question(0, "What does '我不确定 (Wǒ bù quèdìng)' mean in English?", null, null, null, "I am not sure"));
                allQuestions.add(new Question(0, "What does '我们什么时候见面? (Wǒmen shénme shíhòu jiànmiàn?)' mean in English?", null, null, null, "When shall we meet?"));

                Collections.shuffle(allQuestions);

                questions = new ArrayList<>(allQuestions.subList(0, 20));
                break;

            case "hard":
                allQuestions.add(new Question(0, "🔊", null, null, null, "你从哪来"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你几岁了"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你叫什么名字"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你从哪来"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你几岁了"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你叫什么名字"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你住在哪里"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你的爱好是什么"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢什么运动"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢看电影吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢吃什么"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你每天几点起床"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢什么颜色"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你最喜欢的书是什么"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢听音乐吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢旅游吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你的生日是什么时候"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你在哪里上学"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢养宠物吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你会游泳吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢喝茶还是咖啡"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢什么季节"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你最喜欢的节日是什么"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你最想去哪里旅行"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢什么类型的电影"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你通常几点睡觉"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢跑步吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢下雨天吗"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢吃甜的还是咸的"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢看书还是看电视"));
                allQuestions.add(new Question(0, "🔊", null, null, null, "你喜欢喝什么饮料"));


                allQuestions.add(new Question(R.drawable.bike, "What is this called in Chinese?", null, null, null, "自行车"));
                allQuestions.add(new Question(R.drawable.clock, "What is this called in Chinese?", null, null, null, "钟"));
                allQuestions.add(new Question(R.drawable.jacket, "What is this called in Chinese?", null, null, null, "夹克"));
                allQuestions.add(new Question(R.drawable.glove, "What is this called in Chinese?", null, null, null, "手套"));
                allQuestions.add(new Question(R.drawable.scarf, "What is this called in Chinese?", null, null, null, "围巾"));
                allQuestions.add(new Question(R.drawable.shot, "What is this called in Chinese?", null, null, null, "短裤"));
                allQuestions.add(new Question(R.drawable.glasses, "What is this called in Chinese?", null, null, null, "眼镜"));
                allQuestions.add(new Question(R.drawable.key, "What is this called in Chinese?", null, null, null, "钥匙"));
                allQuestions.add(new Question(R.drawable.comb, "What is this called in Chinese?", null, null, null, "梳子"));

                allQuestions.add(new Question(0, "How do you pronounce 'Could you kindly assist me in finding my way?' in Chinese?", null, null, null, "请您帮我指路好吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Do you know how to get to the nearest hospital?' in Chinese?", null, null, null, "你知道怎么去最近的医院吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to confirm my flight details' in Chinese?", null, null, null, "我需要确认我的航班信息"));
                allQuestions.add(new Question(0, "How do you pronounce 'It’s a pleasure to meet you' in Chinese?", null, null, null, "很高兴认识你"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m looking for a reliable taxi service' in Chinese?", null, null, null, "我在找一个可靠的出租车服务"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good restaurant nearby?' in Chinese?", null, null, null, "你能推荐一个附近的好餐厅吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to extend my stay for another week' in Chinese?", null, null, null, "我想延长我的住宿一周"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you please speak a bit slower?' in Chinese?", null, null, null, "你能说得慢一点吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I have an appointment with a friend later today' in Chinese?", null, null, null, "我今天稍后有个朋友约会"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you help me find my luggage?' in Chinese?", null, null, null, "你能帮我找到我的行李吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'How far is the nearest metro station?' in Chinese?", null, null, null, "最近的地铁站有多远"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you kindly provide me with directions to the airport?' in Chinese?", null, null, null, "请您告诉我去机场的路线好吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to purchase tickets for the concert' in Chinese?", null, null, null, "我想购买音乐会的票"));
                allQuestions.add(new Question(0, "How do you pronounce 'Excuse me, where can I find a pharmacy?' in Chinese?", null, null, null, "对不起，我在哪里能找到药店"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m afraid I’m not familiar with this area' in Chinese?", null, null, null, "恐怕我不熟悉这个地区"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need assistance with my luggage' in Chinese?", null, null, null, "我需要帮忙拿我的行李"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’ve lost my passport, can you help?' in Chinese?", null, null, null, "我丢了护照，你能帮我吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to exchange currency, where is the nearest exchange service?' in Chinese?", null, null, null, "我需要兑换货币，哪里有最近的兑换点"));
                allQuestions.add(new Question(0, "How do you pronounce 'What time does the last train depart?' in Chinese?", null, null, null, "最后一班火车什么时候发车"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m allergic to peanuts, could you confirm the ingredients?' in Chinese?", null, null, null, "我对花生过敏，你能确认一下食材吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you recommend a good local guide for sightseeing?' in Chinese?", null, null, null, "你能推荐一个好的本地导游吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to make a reservation for two people at 7 p.m.' in Chinese?", null, null, null, "我想预定七点的两人座位"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to book a guided tour for tomorrow' in Chinese?", null, null, null, "我想预定明天的导游服务"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you help me find a place to buy SIM cards?' in Chinese?", null, null, null, "你能帮我找个地方买SIM卡吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'What time does the museum close?' in Chinese?", null, null, null, "博物馆几点关门"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m afraid I don’t understand the directions' in Chinese?", null, null, null, "恐怕我不明白方向"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to cancel my reservation' in Chinese?", null, null, null, "我想取消我的预订"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to contact customer service' in Chinese?", null, null, null, "我需要联系客户服务"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you give me a receipt for this purchase?' in Chinese?", null, null, null, "你能给我这笔购买的收据吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’ve been here for two weeks and I’m learning a lot' in Chinese?", null, null, null, "我在这里已经两个星期了，我学到了很多"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need a prescription for medicine' in Chinese?", null, null, null, "我需要药物的处方"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m not familiar with the area, could you guide me?' in Chinese?", null, null, null, "我不熟悉这个地方，你能带我吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you recommend a place for sightseeing around here?' in Chinese?", null, null, null, "你能推荐一个附近的旅游景点吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Is there a hospital nearby?' in Chinese?", null, null, null, "附近有医院吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to buy a charger for my phone' in Chinese?", null, null, null, "我需要买个手机充电器"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where is the nearest bank?' in Chinese?", null, null, null, "最近的银行在哪里"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m lost, can you help me find my way?' in Chinese?", null, null, null, "我迷路了，你能帮我找到路吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'How much is this item?' in Chinese?", null, null, null, "这个东西多少钱"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you tell me the way to the train station?' in Chinese?", null, null, null, "你能告诉我去火车站的路吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to book a table for four people' in Chinese?", null, null, null, "我想预定一个四人桌"));
                allQuestions.add(new Question(0, "How do you pronounce 'What time does the last bus leave?' in Chinese?", null, null, null, "最后一班公交车几点发车"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where can I find a taxi?' in Chinese?", null, null, null, "我在哪里能找到出租车"));
                allQuestions.add(new Question(0, "How do you pronounce 'I would like to visit a museum today' in Chinese?", null, null, null, "我今天想去参观博物馆"));
                allQuestions.add(new Question(0, "How do you pronounce 'Could you give me directions to the shopping mall?' in Chinese?", null, null, null, "你能给我去购物中心的方向吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Do you have a map of the city?' in Chinese?", null, null, null, "你有城市的地图吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to buy a SIM card' in Chinese?", null, null, null, "我需要买个SIM卡"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good hotel here?' in Chinese?", null, null, null, "你能推荐一个好的酒店吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I’m looking for a pharmacy' in Chinese?", null, null, null, "我在找药店"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where is the nearest restaurant?' in Chinese?", null, null, null, "最近的餐馆在哪里"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to go to the airport' in Chinese?", null, null, null, "我需要去机场"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you recommend a good place for shopping?' in Chinese?", null, null, null, "你能推荐一个好的购物地点吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Is there a post office nearby?' in Chinese?", null, null, null, "附近有邮局吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can you help me with my luggage?' in Chinese?", null, null, null, "你能帮我拿行李吗"));
                allQuestions.add(new Question(0, "How do you pronounce 'I need to find a restroom' in Chinese?", null, null, null, "我需要找厕所"));
                allQuestions.add(new Question(0, "How do you pronounce 'Where can I buy tickets for the train?' in Chinese?", null, null, null, "我在哪里可以买火车票"));
                allQuestions.add(new Question(0, "How do you pronounce 'Can I pay with credit card here?' in Chinese?", null, null, null, "我可以在这里用信用卡支付吗"));

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

            if (currentQuestion.getQuestionText().startsWith("How do you pronounce")) {
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

            if (currentQuestion.getQuestionText().startsWith("How do you pronounce")) {
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
        editor.putInt(KEY_CHINESE_QUIZ_SCORE, score);
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
                editor.putBoolean(CHINA_EASY_PASSED, true);
                editor.apply();
                resultMessage = "<br><font color='#4CAF50'> Congratulations! You can proceed to the next level.</font><br>";
            } else {
                resultMessage = "<br><font color='#F44336'> You need to improve your score to proceed to the next level.</font><br>";
            }
        } else if (selectedLevel.equalsIgnoreCase("mediate")) {
            if (scorePercentage >= 60) {
                SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(CHINA_MEDIATE_PASSED, true);
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
            // Ensure easy and mediate levels remain unlocked even if hard is failed
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


        dbHelper.insertQuizResult( score, questions.size(), selectedLevel, selectedLevel, "Chinese");


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
