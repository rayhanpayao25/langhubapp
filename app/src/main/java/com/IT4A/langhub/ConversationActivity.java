package com.IT4A.langhub;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.text.Editable;
import android.text.TextWatcher;
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

public class ConversationActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> conversationList;
    private HashMap<String, List<String>> translationsMap;
    private ConversationExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group

    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);  // Changed to activity_search_page

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Conversations");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));


        // Initialize conversation list and translations map
        conversationList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addConversations();  // Add conversation data

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new ConversationExpandableListAdapter(this, conversationList, translationsMap);
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
            String conversation = adapter.getGroup(groupPosition).toString();
            speakText(conversation);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String conversation = adapter.getGroup(groupPosition).toString();
            speakText(conversation);

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

        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

    }

    // Method to populate conversations and translations
    private void addConversations() {
        String[][] data = {
                {"To work", "Spanish: Para trabajar", "Mandarin: 去工作 (Qù gōngzuò)"},
                {"No", "Spanish: No", "Mandarin: 不是 (Bù shì)"},
                {"Do you understand?", "Spanish: ¿Entiendes?", "Mandarin: 你明白吗？(Nǐ míngbái ma?)"},
                {"I don't understand", "Spanish: No entiendo", "Mandarin: 我不明白 (Wǒ bù míngbái)"},
                {"Where?", "Spanish: ¿Dónde?", "Mandarin: 哪里？(Nǎlǐ?)"},
                {"What?", "Spanish: ¿Qué?", "Mandarin: 什么？(Shénme?)"},
                {"How?", "Spanish: ¿Cómo?", "Mandarin: 怎么样？(Zěnme yàng?)"},
                {"How much?", "Spanish: ¿Cuánto cuesta?", "Mandarin: 多少钱？(Duōshǎo qián?)"},
                {"When?", "Spanish: ¿Cuándo?", "Mandarin: 什么时候？(Shénme shíhòu?)"},
                {"Who?", "Spanish: ¿Quién?", "Mandarin: 谁？(Shéi?)"},
                {"Why?", "Spanish: ¿Por qué?", "Mandarin: 为什么？(Wèishéme?)"},
                {"Thank you", "Spanish: Gracias", "Mandarin: 谢谢 (Xièxiè)"},
                {"I'm sorry", "Spanish: Lo siento", "Mandarin: 对不起 (Duìbùqǐ)"},
                {"Congratulations!", "Spanish: ¡Felicidades!", "Mandarin: 恭喜 (Gōngxǐ)"},
                {"It's okay", "Spanish: Está bien", "Mandarin: 没关系 (Méiguānxì)"},
                {"I don't know", "Spanish: No sé", "Mandarin: 我不知道 (Wǒ bù zhīdào)"},
                {"I don't like it", "Spanish: No me gusta", "Mandarin: 我不喜欢 (Wǒ bù xǐhuān)"},
                {"I like it", "Spanish: Me gusta", "Mandarin: 我喜欢 (Wǒ xǐhuān)"},
                {"You're welcome", "Spanish: De nada", "Mandarin: 不客气 (Bù kèqì)"},
                {"I think that...", "Spanish: Pienso que", "Mandarin: 我认为... (Wǒ rènwéi...)"},
                {"No, thank you", "Spanish: No, gracias", "Mandarin: 不，谢谢 (Bù, xièxiè)"},
                {"Excuse me", "Spanish: Disculpe", "Mandarin: 劳驾 (Láojià)"},
                {"Take care", "Spanish: Cuídate", "Mandarin: 保重 (Bǎozhòng)"},
                {"Don't forget", "Spanish: No olvides", "Mandarin: 不要忘记 (Bùyào wàngjì)"},
                {"How do you pronounce this?", "Spanish: ¿Cómo se pronuncia esto?", "Mandarin: 这个怎么发音？(Zhège zěnme fāyīn?)"},
                {"Before", "Spanish: Antes", "Mandarin: 之前 (Zhīqián)"},
                {"After", "Spanish: Después", "Mandarin: 之后 (Zhīhòu)"},
                {"Wrong", "Spanish: Incorrecto", "Mandarin: 错 (Cuò)"},
                {"Right", "Spanish: Correcto", "Mandarin: 对 (Duì)"},
                {"Until", "Spanish: Hasta", "Mandarin: 直到 (Zhídào)"},
                {"Where is the toilet?", "Spanish: ¿Dónde está el baño?", "Mandarin: 洗手间在哪里？(Xǐshǒujiān zài nǎlǐ?)"},
                {"Do you live here?", "Spanish: ¿Vives aquí?", "Mandarin: 你住在这里吗？(Nǐ zhù zài zhèlǐ ma?)"},
                {"Do you like it?", "Spanish: ¿Te gusta?", "Mandarin: 你喜欢吗？(Nǐ xǐhuān ma?)"},
                {"I love it", "Spanish: Me encanta", "Mandarin: 我爱它 (Wǒ ài tā)"},
                {"On business", "Spanish: En negocios", "Mandarin: 出差 (Chūchāi)"},
                {"To work", "Spanish: Para trabajar", "Mandarin: 去工作 (Qù gōngzuò)"},
                {"What happened?", "Spanish: ¿Qué pasó?", "Mandarin: 发生了什么？(Fāshēng le shénme?)"},
                {"Do you need help?", "Spanish: ¿Necesitas ayuda?", "Mandarin: 你需要帮助吗？(Nǐ xūyào bāngzhù ma?)"},
                {"I'm lost", "Spanish: Estoy perdido", "Mandarin: 我迷路了 (Wǒ mílù le)"},
                {"What time is it?", "Spanish: ¿Qué hora es?", "Mandarin: 现在几点了？(Xiànzài jǐ diǎn le?)"},
                {"I want to go there", "Spanish: Quiero ir allí", "Mandarin: 我想去那里 (Wǒ xiǎng qù nàlǐ)"},
                {"How far is it?", "Spanish: ¿Qué tan lejos está?", "Mandarin: 离这里有多远？(Lí zhèlǐ yǒu duō yuǎn?)"},
                {"Can I help you?", "Spanish: ¿Puedo ayudarte?", "Mandarin: 我能帮你吗？(Wǒ néng bāng nǐ ma?)"},
                {"Do you speak English?", "Spanish: ¿Hablas inglés?", "Mandarin: 你会说英语吗？(Nǐ huì shuō Yīngyǔ ma?)"},
                {"I need a doctor", "Spanish: Necesito un médico", "Mandarin: 我需要医生 (Wǒ xūyào yīshēng)"},
                {"Call the police", "Spanish: Llama a la policía", "Mandarin: 报警 (Bàojǐng)"},
                {"Can I sit here?", "Spanish: ¿Puedo sentarme aquí?", "Mandarin: 我可以坐在这里吗？(Wǒ kěyǐ zuò zài zhèlǐ ma?)"},
                {"Please repeat that", "Spanish: Por favor, repítelo", "Mandarin: 请重复一下 (Qǐng chóngfù yīxià)"}
        };

        // Loop to populate conversationList and translationsMap
        for (String[] conversation : data) {
            String conversationText = conversation[0];
            conversationList.add(conversationText);
            List<String> translations = new ArrayList<>();
            translations.add(conversation[1]);
            translations.add(conversation[2]);
            translationsMap.put(conversationText, translations);
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
}
