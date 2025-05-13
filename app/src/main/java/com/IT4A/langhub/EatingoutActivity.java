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

public class EatingoutActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> eatingoutList;
    private HashMap<String, List<String>> translationsMap;
    private EatingoutExpandableList adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eatingout);  // Changed to activity_search_page

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Eating Out");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));



        eatingoutList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addEatingout();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new EatingoutExpandableList(this, eatingoutList, translationsMap);
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
            String eatingout = adapter.getGroup(groupPosition).toString();
            speakText(eatingout);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String eatingout = adapter.getGroup(groupPosition).toString();
            speakText(eatingout);

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


    private void addEatingout() {
        String[][] data = {
                {"Restaurant", "Spanish: Restaurante", "Mandarin: 餐厅 (Cāntīng)"},
                {"Menu", "Spanish: Menú", "Mandarin: 菜单 (Càidān)"},
                {"Table for two", "Spanish: Mesa para dos", "Mandarin: 两人桌 (Liǎng rén zhuō)"},
                {"Bill, please", "Spanish: La cuenta, por favor", "Mandarin: 买单 (Mǎidān)"},
                {"What do you recommend?", "Spanish: ¿Qué recomienda?", "Mandarin: 你推荐什么？(Nǐ tuījiàn shénme?)"},
                {"Water", "Spanish: Agua", "Mandarin: 水 (Shuǐ)"},
                {"Can I have the menu?", "Spanish: ¿Puedo tener el menú?", "Mandarin: 我可以看菜单吗？(Wǒ kěyǐ kàn càidān ma?)"},
                {"I’m vegetarian", "Spanish: Soy vegetariano/a", "Mandarin: 我是素食者 (Wǒ shì sùshí zhě)"},
                {"Do you have Wi-Fi?", "Spanish: ¿Tienen Wi-Fi?", "Mandarin: 你们有Wi-Fi吗？(Nǐmen yǒu Wi-Fi ma?)"},
                {"Could I have the check?", "Spanish: ¿Puedo tener la cuenta?", "Mandarin: 可以给我账单吗？(Kěyǐ gěi wǒ zhàngdān ma?)"},
                {"I’m allergic to nuts", "Spanish: Soy alérgico/a a los frutos secos", "Mandarin: 我对坚果过敏 (Wǒ duì jiānguǒ guòmǐn)"},
                {"Can we get the bill separately?", "Spanish: ¿Podemos pagar por separado?", "Mandarin: 我们可以分别付款吗？(Wǒmen kěyǐ fēnbié fùkuǎn ma?)"},
                {"Do you have a vegetarian option?", "Spanish: ¿Tienen opción vegetariana?", "Mandarin: 你们有素食选项吗？(Nǐmen yǒu sùshí xuǎnxiàng ma?)"},
                {"I’d like to order", "Spanish: Me gustaría ordenar", "Mandarin: 我想点餐 (Wǒ xiǎng diǎn cān)"},
                {"What’s in this dish?", "Spanish: ¿Qué lleva este plato?", "Mandarin: 这道菜有什么？(Zhè dào cài yǒu shénme?)"},
                {"Is this spicy?", "Spanish: ¿Está picante?", "Mandarin: 这个辣吗？(Zhège là ma?)"},
                {"I’ll have the same", "Spanish: Yo tomaré lo mismo", "Mandarin: 我也要一样的 (Wǒ yě yào yīyàng de)"},
                {"Do you have dessert?", "Spanish: ¿Tienen postre?", "Mandarin: 你们有甜点吗？(Nǐmen yǒu tiándiǎn ma?)"},
                {"Can I get it to go?", "Spanish: ¿Puedo llevarlo?", "Mandarin: 我可以打包吗？(Wǒ kěyǐ dǎbāo ma?)"},
                {"What’s today’s special?", "Spanish: ¿Cuál es el plato del día?", "Mandarin: 今天的特别菜是什么？(Jīntiān de tèbié cài shì shénme?)"},
                {"How much is this?", "Spanish: ¿Cuánto cuesta esto?", "Mandarin: 这个多少钱？(Zhège duōshao qián?)"},
                {"I need a table for four", "Spanish: Necesito una mesa para cuatro", "Mandarin: 我需要一张四人桌 (Wǒ xūyào yī zhāng sì rén zhuō)"},
                {"Can I have a glass of wine?", "Spanish: ¿Puedo tener una copa de vino?", "Mandarin: 我可以要一杯酒吗？(Wǒ kěyǐ yào yī bēi jiǔ ma?)"},
                {"What do you have on tap?", "Spanish: ¿Qué tienen de grifo?", "Mandarin: 你们有什么酒吧生啤？(Nǐmen yǒu shénme jiǔ bā shēng pí?)"},
                {"I’m just looking", "Spanish: Solo estoy mirando", "Mandarin: 我只是看看 (Wǒ zhǐshì kànkan)"},
                {"Can I have it without onions?", "Spanish: ¿Puedo pedirlo sin cebolla?", "Mandarin: 可以没有洋葱吗？(Kěyǐ méiyǒu yángcōng ma?)"},
                {"I’ll have the soup", "Spanish: Tomaré la sopa", "Mandarin: 我想要汤 (Wǒ xiǎng yào tāng)"},
                {"Is it gluten-free?", "Spanish: ¿Es libre de gluten?", "Mandarin: 这个没有麸质吗？(Zhège méiyǒu fūzhì ma?)"},
                {"I’d like my steak well done", "Spanish: Me gustaría mi carne bien cocida", "Mandarin: 我要我的牛排熟一点 (Wǒ yào wǒ de niúpái shú yīdiǎn)"},
                {"Can I have some extra napkins?", "Spanish: ¿Puedo tener servilletas extras?", "Mandarin: 可以给我一些额外的餐巾纸吗？(Kěyǐ gěi wǒ yīxiē éwài de cānjīnzhǐ ma?)"},
                {"Is this fresh?", "Spanish: ¿Está fresco?", "Mandarin: 这个新鲜吗？(Zhège xīnxiān ma?)"},
                {"Can you bring me some salt?", "Spanish: ¿Me puede traer sal?", "Mandarin: 可以给我一些盐吗？(Kěyǐ gěi wǒ yīxiē yán ma?)"},
                {"Do you have any specials today?", "Spanish: ¿Tienen especiales hoy?", "Mandarin: 今天有特别优惠吗？(Jīntiān yǒu tèbié yōuhuì ma?)"},
                {"Is it open?", "Spanish: ¿Está abierto?", "Mandarin: 这家开门吗？(Zhè jiā kāimén ma?)"},
                {"I’d like to try the pizza", "Spanish: Me gustaría probar la pizza", "Mandarin: 我想试试披萨 (Wǒ xiǎng shìshì pīsà)"},
                {"Can I sit here?", "Spanish: ¿Puedo sentarme aquí?", "Mandarin: 我可以坐这里吗？(Wǒ kěyǐ zuò zhèlǐ ma?)"},
                {"How long is the wait?", "Spanish: ¿Cuánto tiempo de espera?", "Mandarin: 需要等多久？(Xūyào děng duōjiǔ?)"},
                {"Can I get a refill?", "Spanish: ¿Puedo pedir un recambio?", "Mandarin: 我可以再来一杯吗？(Wǒ kěyǐ zài lái yī bēi ma?)"},
                {"We need a high chair", "Spanish: Necesitamos una silla alta", "Mandarin: 我们需要一个高脚椅 (Wǒmen xūyào yīgè gāo jiǎo yǐ)"},
                {"I don’t want it too hot", "Spanish: No lo quiero demasiado caliente", "Mandarin: 我不想要太热 (Wǒ bù xiǎng yào tài rè)"},
                {"Do you have any vegetarian soups?", "Spanish: ¿Tienen sopas vegetarianas?", "Mandarin: 你们有素食汤吗？(Nǐmen yǒu sùshí tāng ma?)"},
                {"I’ll have the chicken", "Spanish: Tomaré el pollo", "Mandarin: 我要鸡肉 (Wǒ yào jīròu)"},
                {"I’ll have it with rice", "Spanish: Lo tomaré con arroz", "Mandarin: 我要米饭 (Wǒ yào mǐfàn)"},
                {"Can I have a side of fries?", "Spanish: ¿Puedo tener una guarnición de papas fritas?", "Mandarin: 可以来一份薯条吗？(Kěyǐ lái yī fèn shǔ tiáo ma?)"},
                {"I’d like my coffee with milk", "Spanish: Me gustaría mi café con leche", "Mandarin: 我要加奶的咖啡 (Wǒ yào jiā nǎi de kāfēi)"},
                {"Are there any vegan options?", "Spanish: ¿Tienen opciones veganas?", "Mandarin: 你们有纯素选项吗？(Nǐmen yǒu chún sù xuǎnxiàng ma?)"},
                {"Do you have any gluten-free bread?", "Spanish: ¿Tienen pan sin gluten?", "Mandarin: 你们有无麸质面包吗？(Nǐmen yǒu wú fūzhì miànbāo ma?)"},
                {"Can I get a side salad?", "Spanish: ¿Puedo tener una ensalada como acompañante?", "Mandarin: 可以来一份沙拉吗？(Kěyǐ lái yī fèn shālā ma?)"},
                {"Is the food made from scratch?", "Spanish: ¿La comida es hecha desde cero?", "Mandarin: 这些菜是现做的吗？(Zhèxiē cài shì xiàn zuò de ma?)"},
                {"Do you have soy milk?", "Spanish: ¿Tienen leche de soja?", "Mandarin: 你们有豆浆吗？(Nǐmen yǒu dòujiāng ma?)"}
        };


        for (String[] eatingout : data) {
            String eatingoutText = eatingout[0];
            eatingoutList.add(eatingoutText);
            List<String> translations = new ArrayList<>();
            translations.add(eatingout[1]);
            translations.add(eatingout[2]);
            translationsMap.put(eatingoutText, translations);
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
