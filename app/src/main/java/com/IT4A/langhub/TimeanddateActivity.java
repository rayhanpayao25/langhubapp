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

public class TimeanddateActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> timeanddateList;
    private HashMap<String, List<String>> translationsMap;
    private TimeanddateExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeanddate);  // Changed to activity_search_page

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Time and date");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));



        timeanddateList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addTimeanddate();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new TimeanddateExpandableListAdapter(this, timeanddateList, translationsMap);
        expandableListView.setAdapter(adapter);

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

        expandableListView.setOnChildClickListener((parent, v, groupPosition, childPosition, id) -> {
            // Use the filtered list for TTS
            String timeanddate = adapter.getGroup(groupPosition).toString();
            speakText(timeanddate);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String timeanddate = adapter.getGroup(groupPosition).toString();
            speakText(timeanddate);

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


    private void addTimeanddate() {
        String[][] data = {
                {"What time is it?", "Spanish: ¿Qué hora es?", "Mandarin: 现在几点钟？(Xiànzài jǐ diǎn zhōng?)"},
                {"What is the date today?", "Spanish: ¿Qué fecha es hoy?", "Mandarin: 今天几号？(Jīntiān jǐ hào?)"},
                {"Tomorrow's date?", "Spanish: ¿Cuál es la fecha de mañana?", "Mandarin: 明天几号？(Míngtiān jǐ hào?)"},
                {"What time do we meet?", "Spanish: ¿A qué hora nos encontramos?", "Mandarin: 我们几点见面？(Wǒmen jǐ diǎn jiànmiàn?)"},
                {"What day is it?", "Spanish: ¿Qué día es hoy?", "Mandarin: 今天星期几？(Jīntiān xīngqī jǐ?)"},
                {"See you at 3 PM", "Spanish: Nos vemos a las 3 PM", "Mandarin: 下午三点见 (Xiàwǔ sān diǎn jiàn)"},
                {"It's midnight", "Spanish: Es medianoche", "Mandarin: 午夜 (Wǔyè)"},
                {"The meeting is at 10 AM", "Spanish: La reunión es a las 10 AM", "Mandarin: 会议在上午10点 (Huìyì zài shàngwǔ 10 diǎn)"},
                {"It's 12 o'clock", "Spanish: Son las 12 en punto", "Mandarin: 现在十二点 (Xiànzài shí'èr diǎn)"},
                {"What time does the train leave?", "Spanish: ¿A qué hora sale el tren?", "Mandarin: 火车几点出发？(Huǒchē jǐ diǎn chūfā?)"},
                {"I’ll arrive at 6 PM", "Spanish: Llegaré a las 6 PM", "Mandarin: 我将在下午六点到达 (Wǒ jiāng zài xiàwǔ liù diǎn dào dá)"},
                {"It's already late", "Spanish: Ya es tarde", "Mandarin: 已经很晚了 (Yǐjīng hěn wǎn le)"},
                {"It's early", "Spanish: Es temprano", "Mandarin: 很早 (Hěn zǎo)"},
                {"The event starts at 7 PM", "Spanish: El evento empieza a las 7 PM", "Mandarin: 活动在晚上七点开始 (Huódòng zài wǎnshàng qī diǎn kāishǐ)"},
                {"See you in the morning", "Spanish: Nos vemos en la mañana", "Mandarin: 早上见 (Zǎoshàng jiàn)"},
                {"It’s past noon", "Spanish: Ya pasó el mediodía", "Mandarin: 已经过午了 (Yǐjīng guò wǔ le)"},
                {"The flight departs at 2 PM", "Spanish: El vuelo sale a las 2 PM", "Mandarin: 航班在下午2点起飞 (Hángbān zài xiàwǔ 2 diǎn qǐfēi)"},
                {"I woke up at 8 AM", "Spanish: Me desperté a las 8 AM", "Mandarin: 我在早上八点醒来 (Wǒ zài zǎoshàng bā diǎn xǐng lái)"},
                {"It's the 1st of May", "Spanish: Es el 1 de mayo", "Mandarin: 今天是五月一号 (Jīntiān shì wǔ yuè yī hào)"},
                {"Next Friday", "Spanish: El próximo viernes", "Mandarin: 下周五 (Xià zhōu wǔ)"},
                {"It's 3 minutes past 5", "Spanish: Son las 5 y 3 minutos", "Mandarin: 现在五点三分 (Xiànzài wǔ diǎn sān fēn)"},
                {"What time does the store close?", "Spanish: ¿A qué hora cierra la tienda?", "Mandarin: 商店几点关门？(Shāngdiàn jǐ diǎn guānmén?)"},
                {"We meet at noon", "Spanish: Nos encontramos al mediodía", "Mandarin: 我们中午见 (Wǒmen zhōngwǔ jiàn)"},
                {"It's already 9 PM", "Spanish: Ya son las 9 PM", "Mandarin: 已经是晚上9点了 (Yǐjīng shì wǎnshàng 9 diǎn le)"},
                {"The movie starts at 8 PM", "Spanish: La película comienza a las 8 PM", "Mandarin: 电影在晚上8点开始 (Diànyǐng zài wǎnshàng 8 diǎn kāishǐ)"},
                {"What day is tomorrow?", "Spanish: ¿Qué día es mañana?", "Mandarin: 明天星期几？(Míngtiān xīngqī jǐ?)"},
                {"It's already the weekend", "Spanish: Ya es fin de semana", "Mandarin: 已经是周末了 (Yǐjīng shì zhōumò le)"},
                {"The meeting is at 11 AM", "Spanish: La reunión es a las 11 AM", "Mandarin: 会议在上午11点 (Huìyì zài shàngwǔ 11 diǎn)"},
                {"I’ll be there in 10 minutes", "Spanish: Estaré allí en 10 minutos", "Mandarin: 我十分钟后到 (Wǒ shí fēnzhōng hòu dào)"},
                {"It’s 2 hours ahead", "Spanish: Está 2 horas adelante", "Mandarin: 它比我们快2小时 (Tā bǐ wǒmen kuài 2 xiǎoshí)"},
                {"It's a leap year", "Spanish: Es un año bisiesto", "Mandarin: 这是闰年 (Zhè shì rùnnián)"},
                {"The concert is next month", "Spanish: El concierto es el próximo mes", "Mandarin: 演唱会在下个月 (Yǎnchànghuì zài xià gè yuè)"}

        };


        for (String[] timeanddate : data) {
            String timeanddateText = timeanddate[0];
            timeanddateList.add(timeanddateText);
            List<String> translations = new ArrayList<>();
            translations.add(timeanddate[1]);
            translations.add(timeanddate[2]);
            translationsMap.put(timeanddateText, translations);
        }
    }

    // Method to update the border color based on group expansion state
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
