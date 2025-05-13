package com.IT4A.langhub;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import android.widget.SeekBar;
import android.widget.TextView;

public class NumbersActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> numbersList;
    private HashMap<String, List<String>> translationsMap;
    private NumberExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_numbers);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Numbers");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));


        // Initialize numbers list and translations map
        numbersList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addNumbers();  // Add number data

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new NumberExpandableListAdapter(this, numbersList, translationsMap);
        expandableListView.setAdapter(adapter);

        textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d("TTS", "Started speaking");
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d("TTS", "Done speaking");
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("TTS", "Error in speaking");
            }
        });


        // Search functionality
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                Log.d("SearchQuery", "Searching for: " + charSequence.toString());
                adapter.filterList(charSequence.toString());

                // Collapse all groups
                for (int i = 0; i < adapter.getGroupCount(); i++) {
                    expandableListView.collapseGroup(i);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Use the filtered list for TTS
            String number = adapter.getGroup(groupPosition).toString();
            speakText(number);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String number = adapter.getGroup(groupPosition).toString();
            speakText(number);

            View groupView = parent.getChildAt(groupPosition - parent.getFirstVisiblePosition());
            if (groupView != null) {
                ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);

                // Set an OnClickListener for the favorite icon (ImageButton)
                favoriteIcon.setOnClickListener(view1 -> {
                    toggleFavoriteStatus(groupPosition);
                });

                // Set the initial favorite icon state
                SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
                boolean isFavorite = prefs.getBoolean("favorite_" + groupPosition, false);
                favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_empty);
            }

            // Check if the clicked group is already expanded
            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition); // Collapse the group if already expanded
            } else {
                // Collapse the previously expanded group
                if (lastExpandedGroupPosition != -1 && lastExpandedGroupPosition != groupPosition) {
                    expandableListView.collapseGroup(lastExpandedGroupPosition);
                }
                expandableListView.expandGroup(groupPosition); // Expand the clicked group
                lastExpandedGroupPosition = groupPosition; // Update the last expanded group position
            }

            return true; // Indicate that the click event is fully handled
        });


        ttsSpeedSeekBar = findViewById(R.id.ttsSpeedSeekBar);
        ttsSpeedText = findViewById(R.id.ttsSpeedText);

        ttsSpeedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ttsSpeed = progress / 100f;
                ttsSpeedText.setText(String.format("TTS Speed: %.1fx", ttsSpeed));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }
    private void toggleFavoriteStatus(int groupPosition) {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Toggle favorite status (use groupPosition as the key)
        boolean isFavorite = !prefs.getBoolean("favorite_" + groupPosition, false);
        editor.putBoolean("favorite_" + groupPosition, isFavorite);
        editor.apply();

        // Update the icon based on the new favorite status
        View groupView = expandableListView.getChildAt(groupPosition - expandableListView.getFirstVisiblePosition());
        if (groupView != null) {
            ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);
            favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_empty);
        }
    }

    // Method to populate numbers and translations
    private void addNumbers() {
        String[][] data = {
                {"One", "Spanish: Uno", "Mandarin: 一 (Yī)"},
                {"Two", "Spanish: Dos", "Mandarin: 二 (Èr)"},
                {"Three", "Spanish: Tres", "Mandarin: 三 (Sān)"},
                {"Four", "Spanish: Cuatro", "Mandarin: 四 (Sì)"},
                {"Five", "Spanish: Cinco", "Mandarin: 五 (Wǔ)"},
                {"Six", "Spanish: Seis", "Mandarin: 六 (Liù)"},
                {"Seven", "Spanish: Siete", "Mandarin: 七 (Qī)"},
                {"Eight", "Spanish: Ocho", "Mandarin: 八 (Bā)"},
                {"Nine", "Spanish: Nueve", "Mandarin: 九 (Jiǔ)"},
                {"Ten", "Spanish: Diez", "Mandarin: 十 (Shí)"},
                {"Twenty", "Spanish: Veinte", "Mandarin: 二十 (Èr shí)"},
                {"Thirty", "Spanish: Treinta", "Mandarin: 三十 (Sān shí)"},
                {"Forty", "Spanish: Cuarenta", "Mandarin: 四十 (Sì shí)"},
                {"Fifty", "Spanish: Cincuenta", "Mandarin: 五十 (Wǔ shí)"},
                {"Sixty", "Spanish: Sesenta", "Mandarin: 六十 (Liù shí)"},
                {"Seventy", "Spanish: Setenta", "Mandarin: 七十 (Qī shí)"},
                {"Eighty", "Spanish: Ochenta", "Mandarin: 八十 (Bā shí)"},
                {"Ninety", "Spanish: Noventa", "Mandarin: 九十 (Jiǔ shí)"},
                {"Hundred", "Spanish: Cien", "Mandarin: 一百 (Yī bǎi)"},
                {"Two Hundred", "Spanish: Doscientos", "Mandarin: 二百 (Èr bǎi)"},
                {"Three Hundred", "Spanish: Trescientos", "Mandarin: 三百 (Sān bǎi)"},
                {"Four Hundred", "Spanish: Cuatrocientos", "Mandarin: 四百 (Sì bǎi)"},
                {"Five Hundred", "Spanish: Quinientos", "Mandarin: 五百 (Wǔ bǎi)"},
                {"Six Hundred", "Spanish: Seiscientos", "Mandarin: 六百 (Liù bǎi)"},
                {"Seven Hundred", "Spanish: Setecientos", "Mandarin: 七百 (Qī bǎi)"},
                {"Eight Hundred", "Spanish: Ochocientos", "Mandarin: 八百 (Bā bǎi)"},
                {"Nine Hundred", "Spanish: Novecientos", "Mandarin: 九百 (Jiǔ bǎi)"},
                {"Thousand", "Spanish: Mil", "Mandarin: 一千 (Yī qiān)"}
        };

        // Loop to populate numbersList and translationsMap
        for (String[] number : data) {
            String numberText = number[0];
            numbersList.add(numberText);
            List<String> translations = new ArrayList<>();
            translations.add(number[1]);
            translations.add(number[2]);
            translationsMap.put(numberText, translations);
        }
    }

    public void speakText(String text) {
        if (textToSpeech != null) {
            String languageTag;
            String textToSpeak = text;

            if (text.contains("Mandarin:")) {
                languageTag = "zh-CN";
                textToSpeak = text.replaceAll("Mandarin: ", "")
                        .replaceAll("[^\\p{InCJK Unified Ideographs}]", "")
                        .trim();
            } else if (text.contains("Spanish:")) {
                languageTag = "es-ES";
                textToSpeak = text.replaceAll("Spanish: ", "").trim();
            } else {
                languageTag = "en-US";
            }

            Locale locale = Locale.forLanguageTag(languageTag);
            int result = textToSpeech.setLanguage(locale);

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported: " + languageTag);
                Toast.makeText(this, "Language not supported: " + locale.getDisplayLanguage(), Toast.LENGTH_SHORT).show();
            } else {
                textToSpeech.setSpeechRate(ttsSpeed);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "utteranceId");
                } else {
                    HashMap<String, String> params = new HashMap<>();
                    params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utteranceId");
                    textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params);
                }
            }
        }
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Set default language to US English
            textToSpeech.setLanguage(Locale.US);
        } else {
            Toast.makeText(this, "Initialization failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        // Shutdown the TTS engine when activity is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Check if the selected item is the back button
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Perform the back action
            return true; // Return true to indicate the event is handled
        }
        return super.onOptionsItemSelected(item); // Handle other menu items if any
    }
}