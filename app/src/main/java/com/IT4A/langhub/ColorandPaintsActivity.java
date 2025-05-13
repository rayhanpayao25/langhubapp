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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ColorandPaintsActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> colorandpaintsList;
    private HashMap<String, List<String>> translationsMap;
    private ColorandPaintsExpandableList adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private EditText searchEditText;


    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colorandpaints);  // Changed to activity_search_page

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Colors And Paints");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));



        colorandpaintsList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addColorandpaints();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new ColorandPaintsExpandableList(this, colorandpaintsList, translationsMap);
        expandableListView.setAdapter(adapter);



        searchEditText = findViewById(R.id.searchEditText);

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
            String colorandpaints = adapter.getGroup(groupPosition).toString();
            speakText(colorandpaints);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String colorandpaints = adapter.getGroup(groupPosition).toString();
            speakText(colorandpaints);

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


    private void addColorandpaints() {
        String[][] data = {
                {"Red", "Spanish: Rojo", "Mandarin: 红色 (Hóng sè)"},
                {"Blue", "Spanish: Azul", "Mandarin: 蓝色 (Lán sè)"},
                {"Green", "Spanish: Verde", "Mandarin: 绿色 (Lǜ sè)"},
                {"Yellow", "Spanish: Amarillo", "Mandarin: 黄色 (Huáng sè)"},
                {"Black", "Spanish: Negro", "Mandarin: 黑色 (Hēi sè)"},
                {"White", "Spanish: Blanco", "Mandarin: 白色 (Bái sè)"},
                {"Pink", "Spanish: Rosa", "Mandarin: 粉色 (Fěn sè)"},
                {"Purple", "Spanish: Púrpura", "Mandarin: 紫色 (Zǐ sè)"},
                {"Orange", "Spanish: Naranja", "Mandarin: 橙色 (Chéng sè)"},
                {"Brown", "Spanish: Marrón", "Mandarin: 棕色 (Zōng sè)"},
                {"Gray", "Spanish: Gris", "Mandarin: 灰色 (Huī sè)"},
                {"Beige", "Spanish: Beige", "Mandarin: 米色 (Mǐ sè)"},
                {"Turquoise", "Spanish: Turquesa", "Mandarin: 蓝绿色 (Lán lǜ sè)"},
                {"Gold", "Spanish: Dorado", "Mandarin: 金色 (Jīn sè)"},
                {"Silver", "Spanish: Plateado", "Mandarin: 银色 (Yín sè)"},
                {"Copper", "Spanish: Cobre", "Mandarin: 铜色 (Tóng sè)"},
                {"Bronze", "Spanish: Bronce", "Mandarin: 青铜色 (Qīng tóng sè)"},
                {"Magenta", "Spanish: Magenta", "Mandarin: 品红色 (Pǐn hóng sè)"},
                {"Violet", "Spanish: Violeta", "Mandarin: 紫罗兰色 (Zǐ luólán sè)"},
                {"Indigo", "Spanish: Índigo", "Mandarin: 靛蓝色 (Diàn lán sè)"},
                {"Lavender", "Spanish: Lavanda", "Mandarin: 薰衣草色 (Xūn yī cǎo sè)"},
                {"Peach", "Spanish: Durazno", "Mandarin: 桃色 (Tao sè)"},
                {"Ivory", "Spanish: Marfil", "Mandarin: 象牙色 (Xiàngyá sè)"},
                {"Cream", "Spanish: Crema", "Mandarin: 奶油色 (Nǎi yóu sè)"},
                {"Charcoal", "Spanish: Carbón", "Mandarin: 木炭色 (Mùtàn sè)"},
                {"Emerald", "Spanish: Esmeralda", "Mandarin: 翡翠色 (Fěicuì sè)"},
                {"Cyan", "Spanish: Cian", "Mandarin: 青色 (Qīng sè)"},
                {"Mint", "Spanish: Menta", "Mandarin: 薄荷色 (Bò hé sè)"},
                {"Lime", "Spanish: Lima", "Mandarin: 青柠色 (Qīng níng sè)"},
                {"Plum", "Spanish: Ciruela", "Mandarin: 李子色 (Lǐ zǐ sè)"},
                {"Coral", "Spanish: Coral", "Mandarin: 珊瑚色 (Shānhú sè)"},
                {"Tan", "Spanish: Beige", "Mandarin: 浅棕色 (Qiǎn zōng sè)"},
                {"Rust", "Spanish: Óxido", "Mandarin: 锈色 (Xiù sè)"},
                {"Sapphire", "Spanish: Zafiro", "Mandarin: 蓝宝石色 (Lán bǎoshí sè)"},
                {"Scarlet", "Spanish: Escarlata", "Mandarin: 猩红色 (Xīng hóng sè)"},
                {"Burgundy", "Spanish: Burdeos", "Mandarin: 酒红色 (Jiǔ hóng sè)"},
                {"Azure", "Spanish: Azul celeste", "Mandarin: 蔚蓝色 (Wèilán sè)"},
                {"Seafoam", "Spanish: Espuma de mar", "Mandarin: 海泡色 (Hǎi pào sè)"},
                {"Copper", "Spanish: Cobre", "Mandarin: 铜色 (Tóng sè)"},
                {"Sunset", "Spanish: Atardecer", "Mandarin: 日落色 (Rìluò sè)"},
                {"Fuchsia", "Spanish: Fucsia", "Mandarin: 紫红色 (Zǐ hóng sè)"},
                {"Rose", "Spanish: Rosa", "Mandarin: 玫瑰色 (Méi guī sè)"},
                {"Jade", "Spanish: Jade", "Mandarin: 翡翠色 (Fěicuì sè)"},
                {"Onyx", "Spanish: Ónix", "Mandarin: 黑玉色 (Hēi yù sè)"},
                {"Chartreuse", "Spanish: Chartreuse", "Mandarin: 黄绿色 (Huáng lǜ sè)"},
                {"Mint", "Spanish: Menta", "Mandarin: 薄荷色 (Bò hé sè)"},
                {"Wisteria", "Spanish: Glicina", "Mandarin: 紫藤色 (Zǐ téng sè)"},
                {"Auburn", "Spanish: Castaño", "Mandarin: 红棕色 (Hóng zōng sè)"},
                {"Khaki", "Spanish: Caqui", "Mandarin: 卡其色 (Kǎ qí sè)"},
                {"Chocolate", "Spanish: Chocolate", "Mandarin: 巧克力色 (Qiǎokèlì sè)"},
                {"Honey", "Spanish: Miel", "Mandarin: 蜂蜜色 (Fēngmì sè)"},
                {"Amber", "Spanish: Ámbar", "Mandarin: 琥珀色 (Hǔpò sè)"},
                {"Tangerine", "Spanish: Mandarina", "Mandarin: 橘色 (Jú sè)"},
                {"Periwinkle", "Spanish: Periwinkle", "Mandarin: 长春花色 (Cháng chūn huā sè)"},
                {"Lilac", "Spanish: Lila", "Mandarin: 淡紫色 (Dàn zǐ sè)"},
                {"Pewter", "Spanish: Peltre", "Mandarin: 锡色 (Xī sè)"},
                {"Pine", "Spanish: Pino", "Mandarin: 松树绿 (Sōngshù lǜ)"},
                {"Slate", "Spanish: Pizarra", "Mandarin: 石板色 (Shí bǎn sè)"},
                {"Canary", "Spanish: Canario", "Mandarin: 金丝雀黄 (Jīn sīquè huáng)"},
                {"Slate gray", "Spanish: Gris pizarra", "Mandarin: 石板灰色 (Shí bǎn huī sè)"},
                {"Steel blue", "Spanish: Azul acero", "Mandarin: 钢铁蓝色 (Gāngtiě lán sè)"}
        };


        for (String[] colorandpaints : data) {
            String colorandpaintsText = colorandpaints[0];
            colorandpaintsList.add(colorandpaintsText);
            List<String> translations = new ArrayList<>();
            translations.add(colorandpaints[1]);
            translations.add(colorandpaints[2]);
            translationsMap.put(colorandpaintsText, translations);
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
