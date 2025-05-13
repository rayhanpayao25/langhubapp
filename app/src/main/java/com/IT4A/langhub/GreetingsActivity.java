package com.IT4A.langhub;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
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

public class GreetingsActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> greetingList;
    private HashMap<String, List<String>> translationsMap;
    private GreetingsExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_greetings);  // Changed to activity_greetings.xml

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Greetings");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        // Initialize greeting list and translations map
        greetingList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addGreetings();  // Add greeting data

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new GreetingsExpandableListAdapter(this, greetingList, translationsMap);
        expandableListView.setAdapter(adapter);

        // Search functionality
        EditText searchEditText = findViewById(R.id.searchEditText);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
            }

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
            public void afterTextChanged(Editable editable) {
            }
        });

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Use the filtered list for TTS
            String greeting = adapter.getGroup(groupPosition).toString();
            speakText(greeting);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text


            return true; // Return true to indicate the click event was handled
        });


        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {

            // Use the filtered list for TTS
            String greeting = adapter.getGroup(groupPosition).toString();
            speakText(greeting);

            // Check if the clicked group is already expanded
            boolean isExpanded = expandableListView.isGroupExpanded(groupPosition);

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


            // Collapse all groups and reset their border color
            for (int i = 0; i < adapter.getGroupCount(); i++) {
                if (i != groupPosition) {
                    // Reset the border color for collapsed groups
                    updateBorderColor(i, false);
                }
            }

            if (isExpanded) {
                expandableListView.collapseGroup(groupPosition); // Collapse the group if already expanded
            } else {
                // Collapse the previously expanded group
                if (lastExpandedGroupPosition != -1 && lastExpandedGroupPosition != groupPosition) {
                    expandableListView.collapseGroup(lastExpandedGroupPosition);
                }
                expandableListView.expandGroup(groupPosition); // Expand the clicked group
                lastExpandedGroupPosition = groupPosition; // Update the last expanded group position
            }

            // Update the border color for the clicked group (expanded or collapsed)
            updateBorderColor(groupPosition, !isExpanded);  // Pass the opposite of the current state (expanded -> collapsed)

            return true; // Indicate that the click event is fully handled

        });
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

    // Method to populate greetings and translations
    private void addGreetings() {
        String[][] data = {
                {"Hello/Hi", "Spanish: Hola", "Mandarin: 你好 (Nǐ hǎo)"},
                {"Good Afternoon", "Spanish: Buenas Tardes", "Mandarin: 下午好 (Xiàwǔ hǎo)"},
                {"Good Evening", "Spanish: Buenas Noches", "Mandarin: 晚上好 (Wǎnshàng hǎo)"},
                {"Good Night", "Spanish: Buenas Noches", "Mandarin: 晚安 (Wǎn'ān)"},
                {"How are you?", "Spanish: ¿Cómo estás?", "Mandarin: 你好吗？(Nǐ hǎo ma?)"},
                {"I'm fine", "Spanish: Estoy bien", "Mandarin: 我很好 (Wǒ hěn hǎo)"},
                {"I'm not well", "Spanish: No estoy bien", "Mandarin: 我不好 (Wǒ bù hǎo)"},
                {"Good", "Spanish: Bueno", "Mandarin: 好 (Hǎo)"},
                {"So so", "Spanish: Así así", "Mandarin: 马马虎虎 (Mǎmǎhūhū)"},
                {"Bad", "Spanish: Mal", "Mandarin: 差 (Chà)"},
                {"Great!", "Spanish: ¡Genial!", "Mandarin: 太好了 (Tài hǎo le)"},
                {"What's your name?", "Spanish: ¿Cómo te llamas?", "Mandarin: 你叫什么名字？(Nǐ jiào shénme míngzì?)"},
                {"My name is ...", "Spanish: Me llamo ...", "Mandarin: 我叫... (Wǒ jiào...)"},
                {"Take care", "Spanish: Cuídate", "Mandarin: 保重 (Bǎozhòng)"},
                {"Good luck", "Spanish: Buena suerte", "Mandarin: 祝你好运 (Zhù nǐ hǎo yùn)"},
                {"See you later", "Spanish: Hasta luego", "Mandarin: 回头见 (Huítóu jiàn)"},
                {"See you tomorrow", "Spanish: Hasta mañana", "Mandarin: 明天见 (Míngtiān jiàn)"},
                {"What about you?", "Spanish: ¿Y tú?", "Mandarin: 你呢？(Nǐ ne?)"},
                {"Goodbye", "Spanish: Adiós", "Mandarin: 再见 (Zàijiàn)"},
                {"How old are you?", "Spanish: ¿Cuántos años tienes?", "Mandarin: 你几岁？(Nǐ jǐ suì?)"},
                {"I'm ... years old", "Spanish: Tengo ... años", "Mandarin: 我...岁 (Wǒ ... suì)"},
                {"Where are you from?", "Spanish: ¿De dónde eres?", "Mandarin: 你来自哪里？(Nǐ láizì nǎlǐ?)"},
                {"I am from ...", "Spanish: Soy de ...", "Mandarin: 我来自... (Wǒ láizì...)"},
                {"Can you help me?", "Spanish: ¿Puedes ayudarme?", "Mandarin: 你能帮我吗？(Nǐ néng bāng wǒ ma?)"},
                {"I don't understand", "Spanish: No entiendo", "Mandarin: 我不明白 (Wǒ bù míngbái)"},
                {"Please", "Spanish: Por favor", "Mandarin: 请 (Qǐng)"},
                {"Thank you", "Spanish: Gracias", "Mandarin: 谢谢 (Xièxiè)"},
                {"You're welcome", "Spanish: De nada", "Mandarin: 不客气 (Bù kèqì)"},
                {"Excuse me", "Spanish: Perdón", "Mandarin: 对不起 (Duìbuqǐ)"},
                {"Sorry", "Spanish: Lo siento", "Mandarin: 对不起 (Duìbuqǐ)"},
                {"What time is it?", "Spanish: ¿Qué hora es?", "Mandarin: 现在几点？(Xiànzài jǐ diǎn?)"},
                {"It's ... o'clock", "Spanish: Son las ...", "Mandarin: 现在...点 (Xiànzài ... diǎn)"},
                {"I'm hungry", "Spanish: Tengo hambre", "Mandarin: 我饿了 (Wǒ è le)"},
                {"I'm thirsty", "Spanish: Tengo sed", "Mandarin: 我渴了 (Wǒ kě le)"},
                {"Where is the bathroom?", "Spanish: ¿Dónde está el baño?", "Mandarin: 洗手间在哪里？(Xǐshǒujiān zài nǎlǐ?)"},
                {"How much is this?", "Spanish: ¿Cuánto cuesta esto?", "Mandarin: 这个多少钱？(Zhège duōshǎo qián?)"},
                {"I like it", "Spanish: Me gusta", "Mandarin: 我喜欢 (Wǒ xǐhuān)"},
                {"I don't like it", "Spanish: No me gusta", "Mandarin: 我不喜欢 (Wǒ bù xǐhuān)"},
                {"I understand", "Spanish: Entiendo", "Mandarin: 我明白 (Wǒ míngbái)"},
                {"I don't know", "Spanish: No sé", "Mandarin: 我不知道 (Wǒ bù zhīdào)"},
                {"What is this?", "Spanish: ¿Qué es esto?", "Mandarin: 这是什么？(Zhè shì shénme?)"},
                {"It's a ...", "Spanish: Es un ...", "Mandarin: 这是一个... (Zhè shì yīgè...)"}
        };

        // Loop to populate greetingList and translationsMap
        for (String[] greeting : data) {
            String greetingText = greeting[0];
            greetingList.add(greetingText);
            List<String> translations = new ArrayList<>();
            translations.add(greeting[1]);
            translations.add(greeting[2]);
            translationsMap.put(greetingText, translations);
        }
    }

    public void updateBorderColor(int groupPosition, boolean isExpanded) {
        // Find the View for the group
        View groupView = expandableListView.getChildAt(groupPosition);

        if (groupView != null) {
            // Instead of applying the border_with_shadow, you can just set a plain background or none
            // Remove the shadow effect, just set a simple background color (e.g., white or transparent)
            if (isExpanded) {
                groupView.setBackgroundResource(android.R.color.transparent); // No border and no shadow for expanded state
            } else {
                groupView.setBackgroundResource(android.R.color.transparent); // No border and no shadow for collapsed state
            }
        }
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
