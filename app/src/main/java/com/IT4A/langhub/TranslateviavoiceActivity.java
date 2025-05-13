package com.IT4A.langhub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.speech.tts.TextToSpeech;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.ArrayList;
import java.util.Locale;

public class TranslateviavoiceActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_VOICE_INPUT = 1000;

    private ImageButton speakButton;
    private TextView translatedText;
    private SpeechRecognizer speechRecognizer;
    private Spinner languageSpinner;
    private ProgressBar progressBar;
    private Spinner optionsSpinner;
    private TextView recognizingText;
    private Intent speechRecognizerIntent;
    private EditText outputtranslate;
    private Button translateButton;
    private ArrayList<String> optionsList;
    private TextToSpeech textToSpeech;
    private String selectedLanguage;
    private boolean isLanguageSelected = false;
    private ArrayList<String> translationHistory = new ArrayList<>();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translateviavoice);

        sharedPreferences = getSharedPreferences("TranslateViaVoicePrefs", Context.MODE_PRIVATE);

        outputtranslate = findViewById(R.id.outputtranslate);
        outputtranslate.setEnabled(false);

        translatedText = findViewById(R.id.translated_text);
        translatedText.setEnabled(false);

        languageSpinner = findViewById(R.id.language_spinner);
        speakButton = findViewById(R.id.speak_button);
        optionsSpinner = findViewById(R.id.optionsSpinner);
        progressBar = findViewById(R.id.progressBar);
        translatedText = findViewById(R.id.translated_text);
        translateButton = findViewById(R.id.translateButton);
        translateButton.setOnClickListener(v -> translateTextOnButtonClick());

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.language_array, R.layout.spinner_items);
        adapter.setDropDownViewResource(R.layout.spinner_items);
        languageSpinner.setAdapter(adapter);
        optionsSpinner.setAdapter(adapter);

        optionsList = new ArrayList<>();
        optionsList.add("Translate to");
        optionsList.add("English");
        optionsList.add("Chinese");
        optionsList.add("Spanish");

        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<>(this, R.layout.spinner_items, optionsList);
        optionsAdapter.setDropDownViewResource(R.layout.spinner_items);
        optionsSpinner.setAdapter(optionsAdapter);

        Dialog customDialog = new Dialog(this);
        customDialog.setContentView(R.layout.dialog_speech_recognition);
        customDialog.setCancelable(false);

        View resetButton = findViewById(R.id.resetButton);
        resetButton.setOnClickListener(view -> resetTranslationData());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {}

            @Override
            public void onBeginningOfSpeech() {}

            @Override
            public void onRmsChanged(float v) {}

            @Override
            public void onBufferReceived(byte[] bytes) {}

            @Override
            public void onEndOfSpeech() {}

            @Override
            public void onError(int i) {}

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String spokenText = data.get(0);
                translateText(spokenText);
                runOnUiThread(() -> {
                    translatedText.setEnabled(true);
                    outputtranslate.setEnabled(false);
                });
            }

            @Override
            public void onPartialResults(Bundle bundle) {}

            @Override
            public void onEvent(int i, Bundle bundle) {}
        });

        speakButton.setOnClickListener(view -> startVoiceRecognition(outputtranslate));

        translateButton.setOnClickListener(view -> translateTextOnButtonClick());

        Toolbar appBar = findViewById(R.id.appBar);
        setSupportActionBar(appBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
        appBar.setNavigationOnClickListener(view -> onBackPressed());

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if (position == 0) {
                    isLanguageSelected = false;
                } else {
                    isLanguageSelected = true;
                    if (position == 1) {
                        selectedLanguage = "en";
                    } else if (position == 2) {
                        selectedLanguage = "zh-CN";
                    } else if (position == 3) {
                        selectedLanguage = "es-ES";
                    }
                }
                updateSpeakButtonState();
                saveSpinnerState(languageSpinner, "languagePosition");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                isLanguageSelected = false;
                updateSpeakButtonState();
            }
        });

        optionsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                saveSpinnerState(optionsSpinner, "optionsPosition");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        updateSpeakButtonState();

        ImageView speakerIcon = findViewById(R.id.speakerIcon);
        speakerIcon.setOnClickListener(view -> {
            String textToRead = outputtranslate.getText().toString();

            if (!textToRead.isEmpty()) {
                String selectedOption = optionsSpinner.getSelectedItem().toString();

                if (selectedOption.equals("Chinese")) {
                    int langResult = textToSpeech.setLanguage(Locale.CHINA);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(TranslateviavoiceActivity.this, "Chinese language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String chineseText = getChineseCharactersOnly(textToRead);
                    textToRead = chineseText;
                } else if (selectedOption.equals("Spanish")) {
                    int langResult = textToSpeech.setLanguage(new Locale("es", "ES"));
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(TranslateviavoiceActivity.this, "Spanish language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (selectedOption.equals("English")) {
                    int langResult = textToSpeech.setLanguage(Locale.US);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(TranslateviavoiceActivity.this, "English language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(TranslateviavoiceActivity.this, "Please select a valid language in options", Toast.LENGTH_SHORT).show();
                    return;
                }

                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(TranslateviavoiceActivity.this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });

        textToSpeech = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                Toast.makeText(TranslateviavoiceActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                speakerIcon.setEnabled(false);
            }
        });

        ImageView copyIcon = findViewById(R.id.copyIcon);
        copyIcon.setOnClickListener(view -> {
            String textToCopy = outputtranslate.getText().toString();

            if (!textToCopy.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(TranslateviavoiceActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(TranslateviavoiceActivity.this, "No text to copy", Toast.LENGTH_SHORT).show();
            }
        });

        restoreState();
    }

    private void saveSpinnerState(Spinner spinner, String key) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, spinner.getSelectedItemPosition());
        editor.apply();
    }

    private void restoreState() {
        int languagePosition = sharedPreferences.getInt("languagePosition", 0);
        int optionsPosition = sharedPreferences.getInt("optionsPosition", 0);
        String savedTranslatedText = sharedPreferences.getString("translatedText", "");
        String savedOutputTranslate = sharedPreferences.getString("outputTranslate", "");

        languageSpinner.setSelection(languagePosition);
        optionsSpinner.setSelection(optionsPosition);
        translatedText.setText(savedTranslatedText);
        outputtranslate.setText(savedOutputTranslate);
    }


    private void translateTextOnButtonClick(String text) {

        speechRecognizer.startListening(speechRecognizerIntent);

        outputtranslate.setEnabled(true);

        // Update 4
        String translatedTextString = "Translated text: " + text;

        // Replace with actual translation
        outputtranslate.setText(translatedTextString);
    }


    private void updateSpeakButtonState() {
        speakButton.setEnabled(isLanguageSelected);
        speakButton.setAlpha(isLanguageSelected ? 1.0f : 0.5f);
    }

    private String getChineseCharactersOnly(String text) {
        StringBuilder chineseText = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.toString(character).matches("[\\u4e00-\\u9fa5]+")) {
                chineseText.append(character);
            }
        }
        return chineseText.toString();
    }

    private void startVoiceRecognition(EditText outputtranslate) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(intent, REQUEST_CODE_VOICE_INPUT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_VOICE_INPUT && resultCode == RESULT_OK && data != null) {
            ArrayList<String> recognizedWords = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (recognizedWords != null && !recognizedWords.isEmpty()) {
                String spokenText = recognizedWords.get(0);
                translateText(spokenText);
            }
        }
    }

    private void translateText(String text) {
        translatedText.setText(text);
        updateTranslatedTextEditability();
    }

    private void translateTextOnButtonClick() {
        String textToTranslate = translatedText.getText().toString().trim();

        if (textToTranslate.isEmpty()) {
            Toast.makeText(TranslateviavoiceActivity.this, "Please enter text to translate", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetLanguage = "en";
        String selectedLanguage = optionsSpinner.getSelectedItem().toString();

        if (selectedLanguage.equals("Translate to")) {
            Toast.makeText(TranslateviavoiceActivity.this, "Please select a target language", Toast.LENGTH_SHORT).show();
            return;
        } else if (selectedLanguage.equals("Chinese")) {
            targetLanguage = "zh";
        } else if (selectedLanguage.equals("Spanish")) {
            targetLanguage = "es";
        }

        progressBar.setVisibility(View.VISIBLE);
        translateButton.setEnabled(false);

        final String finalTargetLanguage = targetLanguage;
        TranslateHelper.translateText(textToTranslate, targetLanguage, new TranslateHelper.TranslateCallback() {
            @Override
            public void onTranslateSuccess(String translatedTextResult) {
                runOnUiThread(() -> {
                    String finalTranslatedTextResult = translatedTextResult;
                    if (finalTargetLanguage.equals("zh")) {
                        String pinyin = getPinyin(finalTranslatedTextResult);
                        finalTranslatedTextResult = finalTranslatedTextResult + " (" + pinyin + ")";
                    }

                    outputtranslate.setText(finalTranslatedTextResult);
                    //outputtranslate.setEnabled(true); //Removed as per update 2
                    progressBar.setVisibility(View.GONE);
                    translateButton.setEnabled(true);

                    String historyEntry = textToTranslate + " -> " + finalTranslatedTextResult;
                    translationHistory.add(historyEntry);

                    saveState();
                });
            }

            @Override
            public void onTranslateFailure(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(TranslateviavoiceActivity.this, "Translation failed: " + error, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    translateButton.setEnabled(true);
                });
            }
        }, TranslateviavoiceActivity.this);
    }






    private String getPinyin(String chineseText) {
        StringBuilder pinyin = new StringBuilder();
        for (int i = 0; i < chineseText.length(); i++) {
            char character = chineseText.charAt(i);
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(character);
            if (pinyinArray != null) {
                String pinyinString = pinyinArray[0];
                pinyinString = pinyinString.replaceAll("[0-9]", "");
                pinyin.append(pinyinString).append(" ");
            }
        }
        return pinyin.toString().trim();
    }

    private void resetTranslationData() {
        outputtranslate.setText("");
        outputtranslate.setEnabled(false);
        translatedText.setText("");
        updateTranslatedTextEditability();

        languageSpinner.setSelection(0);
        optionsSpinner.setSelection(0);
        progressBar.setVisibility(View.GONE);
        speakButton.setVisibility(View.VISIBLE);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    private void updateTranslatedTextEditability() {
        boolean hasText = !translatedText.getText().toString().trim().isEmpty();
        translatedText.setEnabled(true);
        translatedText.setFocusable(true);
        translatedText.setFocusableInTouchMode(true);
        if (!hasText) {
            translatedText.setHint("Enter text to translate");
        } else {
            translatedText.setHint("");
        }
    }

    private void saveState() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("translatedText", translatedText.getText().toString());
        editor.putString("outputTranslate", outputtranslate.getText().toString());
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTranslatedTextEditability();
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveState();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

