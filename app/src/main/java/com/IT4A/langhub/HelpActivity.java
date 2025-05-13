package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HelpActivity extends AppCompatActivity {

    private String _instructionLanguage = "en"; // Default language
    private Spinner languageSpinner;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);


        languageSpinner = findViewById(R.id.language_spinner);



        // Dropdown for language selection
        Spinner languageSpinner = findViewById(R.id.language_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);



        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                // Map the selected language name to language code
                switch (position) {
                    case 0:
                        _instructionLanguage = "en"; // English
                        break;
                    case 1:
                        _instructionLanguage = "es"; // Spanish
                        break;
                    case 2:
                        _instructionLanguage = "zh-cn"; // Chinese
                        break;
                    default:
                        _instructionLanguage = "en"; // Default to English
                }
                updateInstructions(); // Update instructions after language change
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Do nothing
            }
        });

        // Initial instructions load
        updateInstructions();
    }

    private void updateInstructions() {
        // Get references to the TextViews
        TextView titleTextView = findViewById(R.id.title_text_view);
        TextView welcomeTextView = findViewById(R.id.welcome_text_view);
        TextView step1TextView = findViewById(R.id.step1_text_view);
        TextView step1DescTextView = findViewById(R.id.step1_desc_text_view);
        TextView step2TextView = findViewById(R.id.step2_text_view);
        TextView step2DescTextView = findViewById(R.id.step2_desc_text_view);
        TextView step3TextView = findViewById(R.id.step3_text_view);
        TextView step3DescTextView = findViewById(R.id.step3_desc_text_view);
        TextView step4TextView = findViewById(R.id.step4_text_view);
        TextView step4DescTextView = findViewById(R.id.step4_desc_text_view);
        TextView step5TextView = findViewById(R.id.step5_text_view);
        TextView step5DescTextView = findViewById(R.id.step5_desc_text_view);
        TextView step6TextView = findViewById(R.id.step6_text_view);
        TextView step6DescTextView = findViewById(R.id.step6_desc_text_view);
        TextView step7TextView = findViewById(R.id.step7_text_view);
        TextView step7DescTextView = findViewById(R.id.step7_desc_text_view);
        TextView step8TextView = findViewById(R.id.step8_text_view);
        TextView step8DescTextView = findViewById(R.id.step8_desc_text_view);
        TextView tipsTextView = findViewById(R.id.tips_text_view);

        // Load translated content based on the selected language
        switch (_instructionLanguage) {
            case "es":
                titleTextView.setText("Cómo usar LangHub");
                welcomeTextView.setText("¡Bienvenido a LangHub! Aquí tienes algunas instrucciones para comenzar a usar la aplicación:");

                // Translate via Text
                step1TextView.setText("Paso 1: Usar 'Traducir por Texto'");
                step1DescTextView.setText("En la página principal, haz clic en 'Traducir por Texto'. Introduce el texto que deseas traducir y selecciona los idiomas de origen y destino.");

                // Translate via Voice
                step2TextView.setText("Paso 2: Usar 'Traducir por Voz'");
                step2DescTextView.setText("Haz clic en 'Traducir por Voz' y usa el micrófono para grabar tu voz. El texto se traducirá automáticamente al idioma seleccionado.");

                // OCR
                step3TextView.setText("Paso 3: Usar 'OCR'");
                step3DescTextView.setText("Haz clic en 'OCR' para capturar una imagen de texto. El texto se extraerá y podrá ser traducido automáticamente.");

                // Greetings
                step4TextView.setText("Paso 4: Usar 'Saludos'");
                step4DescTextView.setText("Haz clic en 'Saludos' para aprender saludos comunes en diferentes idiomas y escuchar la pronunciación.");

                // Conversation
                step5TextView.setText("Paso 5: Usar 'Conversación'");
                step5DescTextView.setText("Selecciona 'Conversación' para practicar frases comunes en situaciones como pedir comida o preguntar direcciones.");

                // Numbers
                step6TextView.setText("Paso 6: Usar 'Números'");
                step6DescTextView.setText("Haz clic en 'Números' para aprender a contar en diferentes idiomas y escuchar la pronunciación.");

                // Time and Date
                step7TextView.setText("Paso 7: Usar 'Hora y Fecha'");
                step7DescTextView.setText("Haz clic en 'Hora y Fecha' para aprender cómo decir la hora y las fechas en varios idiomas.");

                // Games
                step8TextView.setText("Paso 8: Usar 'Juegos'");
                step8DescTextView.setText("Haz clic en 'Juegos' para jugar y practicar tus habilidades lingüísticas mientras te diviertes.");

                // Tips
                tipsTextView.setText("Consejos:\n• Usa la aplicación todos los días para mejorar tus habilidades lingüísticas.\n• No dudes en volver a lecciones anteriores para reforzar el aprendizaje.\n• Practica hablar en voz alta para una mejor pronunciación.");
                break;

            case "zh-cn":
                titleTextView.setText("如何使用LangHub");
                welcomeTextView.setText("欢迎使用LangHub！以下是一些帮助您开始使用应用程序的说明：");

                // Translate via Text
                step1TextView.setText("步骤 1：使用'文字翻译'");
                step1DescTextView.setText("在主页上，点击'文字翻译'。输入你想翻译的文字，选择源语言和目标语言。");

                // Translate via Voice
                step2TextView.setText("步骤 2：使用'语音翻译'");
                step2DescTextView.setText("点击'语音翻译'并使用麦克风录制你的声音。录制的文本会自动翻译成选定的语言。");

                // OCR
                step3TextView.setText("步骤 3：使用'OCR'");
                step3DescTextView.setText("点击'OCR'以捕捉文本图像。应用会提取文本并自动翻译。");

                // Greetings
                step4TextView.setText("步骤 4：使用'问候语'");
                step4DescTextView.setText("点击'问候语'以学习不同语言中的常用问候语，并听到发音。");

                // Conversation
                step5TextView.setText("步骤 5：使用'对话'");
                step5DescTextView.setText("选择'对话'来练习在各种场合中常用的短语，例如点餐或问路。");

                // Numbers
                step6TextView.setText("步骤 6：使用'数字'");
                step6DescTextView.setText("点击'数字'来学习如何在不同的语言中数数，并听到发音。");

                // Time and Date
                step7TextView.setText("步骤 7：使用'时间与日期'");
                step7DescTextView.setText("点击'时间与日期'来学习如何用不同语言表达时间和日期。");

                // Games
                step8TextView.setText("步骤 8：使用'游戏'");
                step8DescTextView.setText("点击'游戏'来通过游戏练习你的语言技能，同时享受乐趣。");

                // Tips
                tipsTextView.setText("提示：\n• 每天使用该应用程序来提高语言技能。\n• 随时回顾之前的课程以巩固学习。\n• 大声朗读练习，以提高发音。");
                break;

            default:
                titleTextView.setText("How to Use LangHub");
                welcomeTextView.setText("Welcome to LangHub! Here are some instructions to get you started with the app:");

                // Translate via Text
                step1TextView.setText("Step 1: Use 'Translate via Text'");
                step1DescTextView.setText("On the homepage, click on 'Translate via Text'. Enter the text you want to translate and select the source and target languages.");

                // Translate via Voice
                step2TextView.setText("Step 2: Use 'Translate via Voice'");
                step2DescTextView.setText("Click on 'Translate via Voice' and use the microphone to record your voice. The text will be automatically translated into the selected language.");

                // OCR
                step3TextView.setText("Step 3: Use 'OCR'");
                step3DescTextView.setText("Click on 'OCR' to capture an image of text. The app will extract the text and translate it automatically.");

                // Greetings
                step4TextView.setText("Step 4: Use 'Greetings'");
                step4DescTextView.setText("Click on 'Greetings' to learn common greetings in different languages and hear the pronunciation.");

                // Conversation
                step5TextView.setText("Step 5: Use 'Conversation'");
                step5DescTextView.setText("Select 'Conversation' to practice common phrases in situations like ordering food or asking for directions.");

                // Numbers
                step6TextView.setText("Step 6: Use 'Numbers'");
                step6DescTextView.setText("Click on 'Numbers' to learn how to count in different languages and hear the pronunciation.");

                // Time and Date
                step7TextView.setText("Step 7: Use 'Time and Date'");
                step7DescTextView.setText("Click on 'Time and Date' to learn how to tell the time and dates in various languages.");

                // Games
                step8TextView.setText("Step 8: Use 'Games'");
                step8DescTextView.setText("Click on 'Games' to play and practice your language skills while having fun.");

                // Tips
                tipsTextView.setText("Tips:\n• Use the app daily to improve your language skills.\n• Don't hesitate to revisit previous lessons to reinforce learning.\n• Practice speaking out loud for better pronunciation.");
                break;
        }

        // Set button actions
        ImageView searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            // Navigate to SearchActivity without any transition animation
            Intent intent = new Intent(HelpActivity.this, SearchPage.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView favoritesButton = findViewById(R.id.favorite_button);
        favoritesButton.setOnClickListener(v -> {
            // Navigate to SearchActivity without any transition animation
            Intent intent = new Intent(HelpActivity.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(v -> {
            // Navigate to HomepageActivity without any transition animation
            Intent intent = new Intent(HelpActivity.this, HomepageActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
        ImageView userButton = findViewById(R.id.user_button);
        userButton.setOnClickListener(view -> {
            Intent intent = new Intent(HelpActivity.this, userActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });
    }

    @Override
    public void onBackPressed() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
        super.onBackPressed();
    }
}
