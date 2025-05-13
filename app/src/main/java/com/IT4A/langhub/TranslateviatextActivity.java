package com.IT4A.langhub;

import com.IT4A.langhub.R;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import net.sourceforge.pinyin4j.PinyinHelper;

import java.util.Locale;

import android.widget.Toast;

import android.text.Editable;
import android.text.TextWatcher;

import android.view.textservice.SentenceSuggestionsInfo;
import android.view.textservice.SpellCheckerSession;
import android.view.textservice.SpellCheckerSession.SpellCheckerSessionListener;
import android.view.textservice.SuggestionsInfo;
import android.view.textservice.TextInfo;
import android.view.textservice.TextServicesManager;

import java.util.ArrayList;
import java.util.List;

public class TranslateviatextActivity extends AppCompatActivity implements SpellCheckerSessionListener {

    private EditText inputEditText;
    private SpellCheckerSession mScs;

    private TextView outputTextView, outputLabel1, outputLabel, labelCircularButton1, labelCircularButton3, correctText;
    private Button translateButton, circularButton1, circularButton2, circularButton3;

    private String sourceLanguage = "", targetLanguage = "";
    private TextToSpeech textToSpeech;
    private List<String> translationHistory = new ArrayList<>();

    private boolean isEnglish(String text) {
        return text.matches("[a-zA-Z\\s]+");
    }

    private void translateText(String text, String sourceLang, String targetLang) {
        String detectedLang = detectLanguage(text);

        if (!detectedLang.equalsIgnoreCase(sourceLang)) {
            showLanguageSuggestion(text, sourceLang, targetLang, detectedLang);
        } else {
            performTranslation(text, sourceLang, targetLang);
        }
    }

    private String detectLanguage(String text) {
        if (text.matches("[a-zA-Z\\s.,!?'\"]+")) {
            return "English";
        } else if (text.matches("[a-zA-ZáéíóúüñÁÉÍÓÚÜÑ\\s.,!¿?'\"]+")) {
            return "Spanish";
        } else if (text.matches("[\\p{IsHan}\\s.,!?'\"]+")) {
            return "Chinese";
        }
        return "Unknown";
    }

    private void showLanguageSuggestion(String text, String sourceLang, String targetLang, String detectedLang) {
        String suggestionMessage = String.format("Do you mean %s? because '%s' is %s language.",
                detectedLang, text, detectedLang.toLowerCase());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Language Mismatch")
                .setMessage(suggestionMessage)
                .setPositiveButton("Yes", (dialog, which) -> {
                    labelCircularButton1.setText(detectedLang);
                    updateLanguage(getLanguageCode(detectedLang), labelCircularButton1, circularButton1);
                    performTranslation(text, detectedLang, targetLang);
                })
                .setNegativeButton("No", (dialog, which) -> {
                    performTranslation(text, sourceLang, targetLang);
                })
                .show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_translateviatext);
        loadHistory();

        inputEditText = findViewById(R.id.inputEditText);
        outputTextView = findViewById(R.id.outputTextView);
        findViewById(R.id.historyButton).setOnClickListener(v -> showHistoryDialog());
        translateButton = findViewById(R.id.translateButton);
        circularButton1 = findViewById(R.id.circularButton1);
        circularButton2 = findViewById(R.id.circularButton2);
        circularButton3 = findViewById(R.id.circularButton3);
        outputLabel1 = findViewById(R.id.outputLabel1);
        outputLabel = findViewById(R.id.outputLabel);
        labelCircularButton1 = findViewById(R.id.labelCircularButton1);
        labelCircularButton3 = findViewById(R.id.labelCircularButton3);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Translate via Text");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        // Initialize Text-to-Speech
        textToSpeech = new TextToSpeech(this, new OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int langResult = textToSpeech.setLanguage(Locale.ENGLISH);  // Default to English for TTS initialization
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Handle language error
                    }
                } else {
                    // Handle TTS initialization failure
                }
            }
        });

        // Set initial button states and colors
        translateButton.setEnabled(false);
        translateButton.setBackgroundColor(getResources().getColor(R.color.grays));
        circularButton2.setEnabled(false);
        circularButton2.setBackgroundColor(getResources().getColor(R.color.grays));

        // Set initial icons for circular buttons
        circularButton1.setTag(R.drawable.en);  // Set initial icon as English
        circularButton3.setTag(R.drawable.es);  // Set initial icon as Spanish

        // Set click listeners
        circularButton1.setOnClickListener(view -> showLanguageSelectionDialog("Select Source", labelCircularButton1, circularButton1));
        circularButton3.setOnClickListener(view -> showLanguageSelectionDialog("Select Target", labelCircularButton3, circularButton3));
        circularButton2.setOnClickListener(view -> swapButtonsAndLabels());
        translateButton.setOnClickListener(view -> {
            String textToTranslate = inputEditText.getText().toString();
            String sourceLang = labelCircularButton1.getText().toString();
            String targetLang = labelCircularButton3.getText().toString();
            translateText(textToTranslate, sourceLang, targetLang);
        });

        findViewById(R.id.speakerIcon).setOnClickListener(v -> {
            String textToRead = outputTextView.getText().toString();

            // Ensure there's text to read
            if (!textToRead.isEmpty()) {
                // Determine the language of the outputTextView and set the TTS language
                if (targetLanguage.equals("zh")) {  // Check if the target language is Chinese (zh)
                    int langResult = textToSpeech.setLanguage(Locale.CHINA);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        // Fallback to Traditional Chinese if Simplified is not supported
                        langResult = textToSpeech.setLanguage(Locale.CHINA);
                    }

                    // Remove Pinyin from the text, keep only the Chinese characters
                    String chineseText = getChineseCharactersOnly(textToRead);
                    // Speak only the Chinese characters, not the Pinyin
                    textToSpeech.speak(chineseText, TextToSpeech.QUEUE_FLUSH, null, null);
                } else if (targetLanguage.equals("es")) {  // Spanish
                    textToSpeech.setLanguage(new Locale("es", "ES"));
                    textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
                } else {  // Default to English
                    textToSpeech.setLanguage(Locale.ENGLISH);
                    textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });

        findViewById(R.id.resetButton).setOnClickListener(v -> resetFields());

        // Add copy functionality for outputTextView
        ImageView copyIcon = findViewById(R.id.copyIcon);
        copyIcon.setOnClickListener(view -> {
            String textToCopy = outputTextView.getText().toString();

            if (!textToCopy.isEmpty()) {
                // Copy text to the clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
                clipboard.setPrimaryClip(clip);

                // Provide feedback to the user
                Toast.makeText(TranslateviatextActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(TranslateviatextActivity.this, "No text to copy", Toast.LENGTH_SHORT).show();
            }
        });

        correctText = findViewById(R.id.CorrectText);
        correctText.setMovementMethod(LinkMovementMethod.getInstance());
        initSpellChecker();
        inputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString().trim();
                String currentLanguage = labelCircularButton1.getText().toString().toLowerCase();
                if (!text.isEmpty()) {
                    if (currentLanguage.equals("english") || currentLanguage.equals("spanish")) {
                        checkSpelling(text);
                    } else {
                        correctText.setVisibility(View.GONE);
                    }
                } else {
                    correctText.setVisibility(View.GONE);
                }
            }
        });

        SharedPreferences prefs = getSharedPreferences("TranslateViaTextPrefs", MODE_PRIVATE);

        if (savedInstanceState != null) {
            labelCircularButton1.setText(savedInstanceState.getString("labelCircularButton1", "Select"));
            labelCircularButton3.setText(savedInstanceState.getString("labelCircularButton3", "Translate To"));
            circularButton1.setTag(savedInstanceState.getInt("circularButton1", R.drawable.ic_language));
            circularButton3.setTag(savedInstanceState.getInt("circularButton3", R.drawable.ic_language));
            outputLabel.setText(savedInstanceState.getString("outputLabel", "Translated Text"));
            outputTextView.setText(savedInstanceState.getString("outputTextView", ""));
            inputEditText.setText(prefs.getString("inputEditText", ""));

            // Restore button backgrounds
            circularButton1.setBackgroundResource((Integer) circularButton1.getTag());
            circularButton3.setBackgroundResource((Integer) circularButton3.getTag());
        } else {
            // Restore from SharedPreferences if savedInstanceState is null
            labelCircularButton1.setText(prefs.getString("labelCircularButton1", "Select"));
            labelCircularButton3.setText(prefs.getString("labelCircularButton3", "Translate To"));
            circularButton1.setTag(prefs.getInt("circularButton1", R.drawable.ic_language));
            circularButton3.setTag(prefs.getInt("circularButton3", R.drawable.ic_language));
            outputLabel.setText(prefs.getString("outputLabel", "Translated Text"));
            outputTextView.setText(prefs.getString("outputTextView", ""));
            inputEditText.setText(prefs.getString("inputEditText", ""));

            // Restore button backgrounds
            circularButton1.setBackgroundResource((Integer) circularButton1.getTag());
            circularButton3.setBackgroundResource((Integer) circularButton3.getTag());
        }

    }

    private void initSpellChecker() {
        TextServicesManager tsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        mScs = tsm.newSpellCheckerSession(null, Locale.getDefault(), this, true);
    }

    private void checkSpelling(String text) {
        if (mScs != null && !text.isEmpty()) {
            String currentLanguage = labelCircularButton1.getText().toString().toLowerCase();

            if (currentLanguage.equals("english")) {
                mScs.getSentenceSuggestions(new TextInfo[]{new TextInfo(text)}, 5);
            } else if (currentLanguage.equals("spanish")) {
                // Add handling for Spanish if needed in the future
                runOnUiThread(() -> correctText.setVisibility(View.GONE));
            } else {
                runOnUiThread(() -> correctText.setVisibility(View.GONE));
            }
        } else {
            runOnUiThread(() -> correctText.setVisibility(View.GONE));
        }
    }

    @Override
    public void onGetSuggestions(SuggestionsInfo[] results) {
        // This method is not used for sentence-level corrections
    }

    @Override
    public void onGetSentenceSuggestions(SentenceSuggestionsInfo[] results) {
        String originalText = inputEditText.getText().toString();
        StringBuilder correctedText = new StringBuilder(originalText);

        boolean hasSuggestions = false;

        for (SentenceSuggestionsInfo result : results) {
            for (int i = 0; i < result.getSuggestionsCount(); i++) {
                int offset = result.getOffsetAt(i);
                int length = result.getLengthAt(i);
                SuggestionsInfo suggestionsInfo = result.getSuggestionsInfoAt(i);
                if (suggestionsInfo.getSuggestionsCount() > 0) {
                    String suggestion = suggestionsInfo.getSuggestionAt(0);
                    correctedText.replace(offset, offset + length, suggestion);
                    hasSuggestions = true;
                }
            }
        }

        final String finalCorrectedText = correctedText.toString();
        final boolean finalHasSuggestions = hasSuggestions;

        runOnUiThread(() -> {
            if (finalHasSuggestions && !finalCorrectedText.equals(originalText)) {
                correctText.setText(finalCorrectedText);
                makeSuggestionsClickable(finalCorrectedText);
                correctText.setVisibility(View.VISIBLE);
            } else {
                correctText.setVisibility(View.GONE);
            }
        });
    }

    private void makeSuggestionsClickable(String suggestion) {
        SpannableString spannableString = new SpannableString(suggestion);
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                inputEditText.setText(suggestion);
                inputEditText.setSelection(suggestion.length());
                correctText.setVisibility(View.GONE);
                checkSpelling(suggestion);
            }
        }, 0, suggestion.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        correctText.setText(spannableString);
        correctText.setMovementMethod(LinkMovementMethod.getInstance());
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("labelCircularButton1", labelCircularButton1.getText().toString());
        outState.putString("labelCircularButton3", labelCircularButton3.getText().toString());
        outState.putInt("circularButton1", (Integer) circularButton1.getTag());
        outState.putInt("circularButton3", (Integer) circularButton3.getTag());
        outState.putString("outputLabel", outputLabel.getText().toString());
        outState.putString("outputTextView", outputTextView.getText().toString());
    }


    private void showLanguageSelectionDialog(String dialogType, TextView targetLabel, Button targetButton) {
        Dialog dialog = new Dialog(TranslateviatextActivity.this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_dialog, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        Button englishButton = dialogView.findViewById(R.id.dialogOption1);
        Button chineseButton = dialogView.findViewById(R.id.dialogOption2);
        Button spanishButton = dialogView.findViewById(R.id.dialogOption3);

        englishButton.setOnClickListener(v -> {
            targetLabel.setText("English");
            updateLanguage("en", targetLabel, targetButton);
            dialog.dismiss();
        });

        chineseButton.setOnClickListener(v -> {
            targetLabel.setText("Chinese");
            updateLanguage("zh", targetLabel, targetButton);
            dialog.dismiss();
        });

        spanishButton.setOnClickListener(v -> {
            targetLabel.setText("Spanish");
            updateLanguage("es", targetLabel, targetButton);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void updateLanguage(String languageCode, TextView targetLabel, Button targetButton) {
        if (targetLabel == labelCircularButton1) {
            sourceLanguage = languageCode;
        } else if (targetLabel == labelCircularButton3) {
            targetLanguage = languageCode;
        }

        // Update button icon and text based on the selected language
        if (languageCode.equals("en")) {
            targetButton.setBackgroundResource(R.drawable.en);
            targetLabel.setText("English");
        } else if (languageCode.equals("es")) {
            targetButton.setBackgroundResource(R.drawable.es);
            targetLabel.setText("Spanish");
        } else if (languageCode.equals("zh")) {
            targetButton.setBackgroundResource(R.drawable.cn);
            targetLabel.setText("Chinese");
        }

        updateTranslateButtonState();
        updateOutputLabels();
    }

    private void updateOutputLabels() {
        if (labelCircularButton1.getText().toString().equals("English")) {
            outputLabel1.setText("English");
        } else if (labelCircularButton1.getText().toString().equals("Chinese")) {
            outputLabel1.setText("Chinese");
        } else if (labelCircularButton1.getText().toString().equals("Spanish")) {
            outputLabel1.setText("Spanish");
        }

        if (labelCircularButton3.getText().toString().equals("English")) {
            outputLabel.setText("English");
        } else if (labelCircularButton3.getText().toString().equals("Chinese")) {
            outputLabel.setText("Chinese");
        } else if (labelCircularButton3.getText().toString().equals("Spanish")) {
            outputLabel.setText("Spanish");
        }
    }

    private void updateTranslateButtonState() {
        if (!sourceLanguage.isEmpty() && !targetLanguage.isEmpty() && !sourceLanguage.equals(targetLanguage)) {
            translateButton.setEnabled(true);
            translateButton.setBackgroundColor(getResources().getColor(R.color.blue));
            circularButton2.setEnabled(true);
            circularButton2.setBackgroundColor(getResources().getColor(R.color.blue));
        } else {
            translateButton.setEnabled(false);
            translateButton.setBackgroundColor(getResources().getColor(R.color.grays));
            circularButton2.setEnabled(false);
            circularButton2.setBackgroundColor(getResources().getColor(R.color.grays));
        }
    }

    private void swapButtonsAndLabels() {
        // Swap button backgrounds
        int tempIcon = (int) circularButton1.getTag();
        circularButton1.setBackgroundResource((int) circularButton3.getTag());
        circularButton3.setBackgroundResource(tempIcon);

        circularButton1.setTag(circularButton3.getTag());
        circularButton3.setTag(tempIcon);

        // Swap labels
        CharSequence tempLabel = labelCircularButton1.getText();
        labelCircularButton1.setText(labelCircularButton3.getText());
        labelCircularButton3.setText(tempLabel);

        // Swap output labels
        CharSequence tempOutputLabel = outputLabel1.getText();
        outputLabel1.setText(outputLabel.getText());
        outputLabel.setText(tempOutputLabel);

        // Swap input and output text
        String tempInputText = inputEditText.getText().toString();
        String tempOutputText = outputTextView.getText().toString();

        // After swapping, we don't immediately check for language mismatch
        // The check will happen when the user clicks the translate button

        if (targetLanguage.equals("zh")) {
            tempOutputText = getChineseCharactersOnly(tempOutputText);
        }

        inputEditText.setText(tempOutputText);
        outputTextView.setText(tempInputText);

        // Swap source and target languages
        String tempLanguage = sourceLanguage;
        sourceLanguage = targetLanguage;
        targetLanguage = tempLanguage;

        // Update language codes based on swapped labels
        sourceLanguage = getLanguageCode(labelCircularButton1.getText().toString());
        targetLanguage = getLanguageCode(labelCircularButton3.getText().toString());

        updateTranslateButtonState();
        correctText.setText("");
        checkSpelling(inputEditText.getText().toString().trim());
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

    private void performTranslation(String text, String sourceLang, String targetLang) {
        String apiKey = "AIzaSyD2f7nNg0At5w6PYwjJgi0qybEboFr26EQ";
        String url = "https://translation.googleapis.com/language/translate/v2?key=" + apiKey;

        String sourceLangCode = getLanguageCode(sourceLang);
        String targetLangCode = getLanguageCode(targetLang);

        if (sourceLangCode.isEmpty() || targetLangCode.isEmpty()) {
            outputTextView.setText("Invalid languages selected.");
            return;
        }

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("q", text);
            requestBody.put("source", sourceLangCode);
            requestBody.put("target", targetLangCode);
            requestBody.put("format", "text");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
                response -> {
                    try {
                        Log.d("Translation Response", response.toString());
                        String translatedText = response.getJSONObject("data")
                                .getJSONArray("translations")
                                .getJSONObject(0)
                                .getString("translatedText");

                        // Save the translated text to history
                        if ("zh".equals(targetLangCode)) {
                            // If the target language is Chinese, append the Pinyin
                            String pinyin = getPinyin(translatedText);
                            String translatedWithPinyin = translatedText + " (" + pinyin + ")";

                            // Add to history with Pinyin
                            translationHistory.add(sourceLang + ": " + text + "\n" + targetLang + ": " + translatedWithPinyin);
                            saveHistory();
                            // Set output text with Pinyin
                            outputTextView.setText(translatedWithPinyin);
                        } else {
                            // If the target language is not Chinese, just add the translation
                            translationHistory.add(sourceLang + ": " + text + "\n" + targetLang + ": " + translatedText);
                            saveHistory();
                            // Set output text without Pinyin
                            outputTextView.setText(translatedText);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        outputTextView.setText("Error parsing translation response.");
                    }
                },
                error -> {
                    Log.e("API Error", error.toString());
                    outputTextView.setText("Error");
                });

        Volley.newRequestQueue(this).add(jsonRequest);
    }

    private void saveHistory() {
        StringBuilder historyBuilder = new StringBuilder();
        for (String translation : translationHistory) {
            historyBuilder.append(translation).append("||"); // Use `||` as a separator
        }
        getSharedPreferences("TranslatePrefs", MODE_PRIVATE)
                .edit()
                .putString("translationHistory", historyBuilder.toString())
                .apply();
    }

    private void loadHistory() {
        String savedHistory = getSharedPreferences("TranslatePrefs", MODE_PRIVATE)
                .getString("translationHistory", "");
        if (!savedHistory.isEmpty()) {
            String[] translations = savedHistory.split("\\|\\|"); // Split by `||`
            translationHistory.clear();
            for (String translation : translations) {
                translationHistory.add(translation);
            }
        }
    }

    private String getPinyin(String chineseText) {
        StringBuilder pinyin = new StringBuilder();
        for (int i = 0; i < chineseText.length(); i++) {
            char character = chineseText.charAt(i);
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(character);
            if (pinyinArray != null) {
                String pinyinString = pinyinArray[0];
                // Remove the numbers from the pinyin string
                pinyinString = pinyinString.replaceAll("[0-9]", "");
                pinyin.append(pinyinString).append(" ");
            }
        }
        return pinyin.toString().trim();
    }

    private String getLanguageCode(String language) {
        switch (language.toLowerCase()) {
            case "english":
                return "en";
            case "spanish":
                return "es";
            case "chinese":
                return "zh";
            default:
                return "";
        }
    }

    private void resetFields() {
        // Clear input fields
        inputEditText.setText("");
        outputTextView.setText("");

        // Reset the source and target languages to default
        sourceLanguage = "Select";
        targetLanguage = "";

        // Reset the labels on the circular buttons
        labelCircularButton1.setText("Select");
        labelCircularButton3.setText("Translate To");

        // Reset the output labels
        outputLabel1.setText("Select");
        outputLabel.setText("Translate To");

        // Disable the Translate and Swap buttons
        translateButton.setEnabled(false);
        translateButton.setBackgroundColor(getResources().getColor(R.color.grays));
        circularButton2.setEnabled(false);
        circularButton2.setBackgroundColor(getResources().getColor(R.color.grays));

        // Reset the icons on the circular buttons
        circularButton1.setBackgroundResource(R.drawable.ic_language);  // Reset to default icon
        circularButton3.setBackgroundResource(R.drawable.ic_language);  // Reset to default icon
    }

    private void showHistoryDialog() {
        Dialog dialog = new Dialog(TranslateviatextActivity.this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.popup_history_dialog, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setGravity(Gravity.CENTER);
        dialog.getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

        TextView historyTextView = dialogView.findViewById(R.id.historyTextView);
        Button closeButton = dialogView.findViewById(R.id.closeButton);
        Button resetButton = dialogView.findViewById(R.id.resetButton);

        // Create a SpannableString to hold the history text with bold formatting
        StringBuilder history = new StringBuilder();
        for (String translation : translationHistory) {
            // Add each translation to the history string
            history.append(translation).append("\n\n");
        }

        SpannableString spannableHistory = new SpannableString(history.toString());
        boldLanguageName(spannableHistory, "English");
        boldLanguageName(spannableHistory, "Spanish");
        boldLanguageName(spannableHistory, "Chinese");

        historyTextView.setText(spannableHistory);

        closeButton.setOnClickListener(v -> dialog.dismiss());

        resetButton.setOnClickListener(v -> {
            // Clear history
            translationHistory.clear();
            saveHistory(); // Update shared preferences
            historyTextView.setText(""); // Clear history display
        });

        dialog.show();
    }

    private void boldLanguageName(SpannableString spannableString, String language) {
        String text = spannableString.toString();  // Convert SpannableString to regular String
        int start = text.indexOf(language);  // Find the start position of the language name
        while (start != -1) {  // If the language name exists, apply bold styling
            spannableString.setSpan(new StyleSpan(Typeface.BOLD), start, start + language.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Find the next occurrence of the same language name (if any)
            start = text.indexOf(language, start + 1);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mScs != null) {
            mScs.close();
        }

        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }



    @Override
    public void onBackPressed() {
        // Save the current state to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("TranslateViaTextPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("labelCircularButton1", labelCircularButton1.getText().toString());
        editor.putString("labelCircularButton3", labelCircularButton3.getText().toString());
        editor.putInt("circularButton1", (Integer) circularButton1.getTag());
        editor.putInt("circularButton3", (Integer) circularButton3.getTag());
        editor.putString("outputLabel", outputLabel.getText().toString());
        editor.putString("outputTextView", outputTextView.getText().toString());
        editor.putString("inputEditText", inputEditText.getText().toString());

        editor.apply();

        super.onBackPressed();
    }

}

