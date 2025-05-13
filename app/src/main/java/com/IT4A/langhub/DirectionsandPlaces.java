package com.IT4A.langhub;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.widget.SeekBar;
import android.widget.TextView;

public class DirectionsandPlaces extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> directionsandPlacesList;
    private HashMap<String, List<String>> translationsMap;
    private DirectionsandPlacesExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directionsandplaces);


        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Directions and Places");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        directionsandPlacesList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addDirectionsandPlace();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new DirectionsandPlacesExpandableListAdapter(this, directionsandPlacesList, translationsMap);
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

        expandableListView.setOnChildClickListener((parent, view, groupPosition, childPosition, id) -> {
            // Use the filtered list for TTS
            String directionsandPlace = adapter.getGroup(groupPosition).toString();
            speakText(directionsandPlace);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {
            // Use the filtered list for TTS
            String directionsandPlace = adapter.getGroup(groupPosition).toString();
            speakText(directionsandPlace);

            // Check if the clicked group is already expanded
            boolean isExpanded = expandableListView.isGroupExpanded(groupPosition);

            // Update the favorites icon visibility
            View groupView = parent.getChildAt(groupPosition - parent.getFirstVisiblePosition());
            if (groupView != null) {
                ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);

                // Set an OnClickListener for the favorite icon (ImageButton)
                favoriteIcon.setOnClickListener(view1 -> {
                    toggleFavoriteStatus(groupPosition);
                });

                // Set the initial favorite icon state
                SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
                boolean isFavorite = prefs.getBoolean("favorite_" + directionsandPlace, false);
                favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled :   R.drawable.ic_favorite_empty);

                // Update visibility based on expanded state
                favoriteIcon.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
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



    private void addDirectionsandPlace() {
        String[][] data = {

        {"Where is the nearest hospital?", "Spanish: ¿Dónde está el hospital más cercano?", "Mandarin: 最近的医院在哪里？(Zuì jìn de yīyuán zài nǎlǐ?)"},
        {"How do I get to the train station?", "Spanish: ¿Cómo llego a la estación de tren?", "Mandarin: 我怎么到火车站？(Wǒ zěnme dào huǒchē zhàn?)"},
        {"Is this the way to the airport?", "Spanish: ¿Es este el camino al aeropuerto?", "Mandarin: 这是去机场的路吗？(Zhè shì qù jīchǎng de lù ma?)"},
        {"How much does it cost to go to the airport?", "Spanish: ¿Cuánto cuesta ir al aeropuerto?", "Mandarin: 去机场要多少钱？(Qù jīchǎng yào duōshao qián?)"},
        {"Where is the nearest bus stop?", "Spanish: ¿Dónde está la parada de autobús más cercana?", "Mandarin: 最近的公交车站在哪里？(Zuì jìn de gōngjiāo chē zhàn zài nǎlǐ?)"},
        {"Can you show me the way to the restaurant?", "Spanish: ¿Puedes mostrarme el camino al restaurante?", "Mandarin: 你能给我指路到餐厅吗？(Nǐ néng gěi wǒ zhǐlù dào cāntīng ma?)"},
        {"Is there a pharmacy nearby?", "Spanish: ¿Hay una farmacia cerca?", "Mandarin: 附近有药店吗？(Fùjìn yǒu yàodiàn ma?)"},
        {"How do I get to the nearest subway station?", "Spanish: ¿Cómo llego a la estación de metro más cercana?", "Mandarin: 怎么到最近的地铁站？(Zěnme dào zuì jìn de dìtiě zhàn?)"},
        {"Where can I buy tickets for the bus?", "Spanish: ¿Dónde puedo comprar boletos para el autobús?", "Mandarin: 我可以在哪里买公交车票？(Wǒ kěyǐ zài nǎlǐ mǎi gōngjiāo chē piào?)"},
        {"Is this the way to the beach?", "Spanish: ¿Es este el camino a la playa?", "Mandarin: 这是去海滩的路吗？(Zhè shì qù hǎitān de lù ma?)"},
        {"How far is the shopping mall from here?", "Spanish: ¿Qué tan lejos está el centro comercial de aquí?", "Mandarin: 购物中心离这里有多远？(Gòuwù zhōngxīn lí zhèlǐ yǒu duō yuǎn?)"},
        {"Where is the nearest ATM?", "Spanish: ¿Dónde está el cajero automático más cercano?", "Mandarin: 最近的ATM在哪里？(Zuì jìn de ATM zài nǎlǐ?)"},
        {"How do I get to the nearest park?", "Spanish: ¿Cómo llego al parque más cercano?", "Mandarin: 怎么到最近的公园？(Zěnme dào zuì jìn de gōngyuán?)"},
        {"Where is the nearest police station?", "Spanish: ¿Dónde está la comisaría de policía más cercana?", "Mandarin: 最近的警察局在哪里？(Zuì jìn de jǐngchá jú zài nǎlǐ?)"},
        {"Can you recommend a good restaurant around here?", "Spanish: ¿Puedes recomendarme un buen restaurante por aquí?", "Mandarin: 你能推荐一下这附近的好餐厅吗？(Nǐ néng tuījiàn yīxià zhè fùjìn de hǎo cāntīng ma?)"},
        {"How do I get to the museum?", "Spanish: ¿Cómo llego al museo?", "Mandarin: 怎么到博物馆？(Zěnme dào bówùguǎn?)"},
        {"Where is the nearest subway station?", "Spanish: ¿Dónde está la estación de metro más cercana?", "Mandarin: 最近的地铁站在哪里？(Zuì jìn de dìtiě zhàn zài nǎlǐ?)"},
        {"What is the best way to get to the hospital?", "Spanish: ¿Cuál es la mejor manera de llegar al hospital?", "Mandarin: 去医院的最佳方式是什么？(Qù yīyuán de zuì jiā fāngshì shì shénme?)"},
        {"How much does a taxi ride to the airport cost?", "Spanish: ¿Cuánto cuesta un taxi al aeropuerto?", "Mandarin: 坐出租车去机场要多少钱？(Zuò chūzūchē qù jīchǎng yào duōshao qián?)"},
        {"Can you help me find the nearest hotel?", "Spanish: ¿Puedes ayudarme a encontrar el hotel más cercano?", "Mandarin: 你能帮我找到最近的酒店吗？(Nǐ néng bāng wǒ zhǎodào zuì jìn de jiǔdiàn ma?)"},
        {"Is there a taxi stand around here?", "Spanish: ¿Hay una parada de taxis por aquí?", "Mandarin: 这附近有出租车站吗？(Zhè fùjìn yǒu chūzūchē zhàn ma?)"},
        {"Where can I find a supermarket?", "Spanish: ¿Dónde puedo encontrar un supermercado?", "Mandarin: 我在哪里可以找到超市？(Wǒ zài nǎlǐ kěyǐ zhǎodào chāoshì?)"},
        {"How do I get to the nearest gas station?", "Spanish: ¿Cómo llego a la gasolinera más cercana?", "Mandarin: 怎么到最近的加油站？(Zěnme dào zuì jìn de jiāyóu zhàn?)"},
        {"Is there a hospital nearby?", "Spanish: ¿Hay un hospital cerca?", "Mandarin: 附近有医院吗？(Fùjìn yǒu yīyuán ma?)"},
        {"Can you take me to the nearest bank?", "Spanish: ¿Puedes llevarme al banco más cercano?", "Mandarin: 你能带我去最近的银行吗？(Nǐ néng dài wǒ qù zuì jìn de yínháng ma?)"},
        {"Where is the nearest post office?", "Spanish: ¿Dónde está la oficina de correos más cercana?", "Mandarin: 最近的邮局在哪里？(Zuì jìn de yóujú zài nǎlǐ?)"},
        {"What’s the quickest route to the city center?", "Spanish: ¿Cuál es la ruta más rápida al centro de la ciudad?", "Mandarin: 到市中心的最快路线是什么？(Dào shì zhōngxīn de zuì kuài lùxiàn shì shénme?)"},
        {"Where is the nearest library?", "Spanish: ¿Dónde está la biblioteca más cercana?", "Mandarin: 最近的图书馆在哪里？(Zuì jìn de túshūguǎn zài nǎlǐ?)"},
        {"How do I get to the nearest hotel?", "Spanish: ¿Cómo llego al hotel más cercano?", "Mandarin: 怎么到最近的酒店？(Zěnme dào zuì jìn de jiǔdiàn?)"},
        {"Is there a shopping mall around here?", "Spanish: ¿Hay un centro comercial por aquí?", "Mandarin: 这附近有购物中心吗？(Zhè fùjìn yǒu gòuwù zhōngxīn ma?)"},
        {"Where can I find a taxi?", "Spanish: ¿Dónde puedo encontrar un taxi?", "Mandarin: 我在哪里可以找到出租车？(Wǒ zài nǎlǐ kěyǐ zhǎodào chūzūchē?)"},
        {"How do I get to the nearest gas station?", "Spanish: ¿Cómo llego a la gasolinera más cercana?", "Mandarin: 怎么到最近的加油站？(Zěnme dào zuì jìn de jiāyóu zhàn?)"},
        {"Where is the nearest bakery?", "Spanish: ¿Dónde está la panadería más cercana?", "Mandarin: 最近的面包店在哪里？(Zuì jìn de miànbāo diàn zài nǎlǐ?)"},
        {"How do I get to the nearest shopping mall?", "Spanish: ¿Cómo llego al centro comercial más cercano?", "Mandarin: 怎么到最近的购物中心？(Zěnme dào zuì jìn de gòuwù zhōngxīn?)"},
        {"Where is the nearest car rental service?", "Spanish: ¿Dónde está el servicio de alquiler de coches más cercano?", "Mandarin: 最近的汽车租赁服务在哪里？(Zuì jìn de qìchē zūlìn fúwù zài nǎlǐ?)"},
        {"Can you show me the way to the nearest park?", "Spanish: ¿Puedes mostrarme el camino al parque más cercano?", "Mandarin: 你能给我指路到最近的公园吗？(Nǐ néng gěi wǒ zhǐlù dào zuì jìn de gōngyuán ma?)"},
        {"Is there a restaurant nearby?", "Spanish: ¿Hay un restaurante cerca?", "Mandarin: 附近有餐厅吗？(Fùjìn yǒu cāntīng ma?)"},
        {"How do I get to the beach?", "Spanish: ¿Cómo llego a la playa?", "Mandarin: 怎么到海滩？(Zěnme dào hǎitān?)"},
        {"Where is the nearest subway entrance?", "Spanish: ¿Dónde está la entrada al metro más cercana?", "Mandarin: 最近的地铁入口在哪里？(Zuì jìn de dìtiě rùkǒu zài nǎlǐ?)"},
        {"Where is the nearest fire station?", "Spanish: ¿Dónde está la estación de bomberos más cercana?", "Mandarin: 最近的消防局在哪里？(Zuì jìn de xiāofángjú zài nǎlǐ?)"},
        {"How far is the airport from here?", "Spanish: ¿Qué tan lejos está el aeropuerto de aquí?", "Mandarin: 机场离这里有多远？(Jīchǎng lí zhèlǐ yǒu duō yuǎn?)"},
        {"Can you help me find the nearest market?", "Spanish: ¿Puedes ayudarme a encontrar el mercado más cercano?", "Mandarin: 你能帮我找到最近的市场吗？(Nǐ néng bāng wǒ zhǎodào zuì jìn de shìchǎng ma?)"},
        {"Where is the nearest restroom?", "Spanish: ¿Dónde está el baño más cercano?", "Mandarin: 最近的厕所在哪里？(Zuì jìn de cèsuǒ zài nǎlǐ?)"},
        {"Is there a hospital nearby?", "Spanish: ¿Hay un hospital cerca?", "Mandarin: 附近有医院吗？(Fùjìn yǒu yīyuán ma?)"},
        {"Where is the nearest taxi stand?", "Spanish: ¿Dónde está la parada de taxis más cercana?", "Mandarin: 最近的出租车站在哪里？(Zuì jìn de chūzūchē zhàn zài nǎlǐ?)"},
        {"How do I get to the airport from here?", "Spanish: ¿Cómo llego al aeropuerto desde aquí?", "Mandarin: 从这里怎么去机场？(Cóng zhèlǐ zěnme qù jīchǎng?)"},
        {"Where can I find a coffee shop?", "Spanish: ¿Dónde puedo encontrar una cafetería?", "Mandarin: 我在哪里可以找到咖啡馆？(Wǒ zài nǎlǐ kěyǐ zhǎodào kāfēi guǎn?)"},
        {"How far is the nearest hospital?", "Spanish: ¿Qué tan lejos está el hospital más cercano?", "Mandarin: 最近的医院有多远？(Zuì jìn de yīyuán yǒu duō yuǎn?)"},
        {"Where is the nearest convenience store?", "Spanish: ¿Dónde está la tienda de conveniencia más cercana?", "Mandarin: 最近的便利店在哪里？(Zuì jìn de biànlì diàn zài nǎlǐ?)"},
        {"How do I get to the bus station?", "Spanish: ¿Cómo llego a la estación de autobuses?", "Mandarin: 怎么到汽车站？(Zěnme dào qìchē zhàn?)"},
        {"Is there a park nearby?", "Spanish: ¿Hay un parque cerca?", "Mandarin: 附近有公园吗？(Fùjìn yǒu gōngyuán ma?)"},
        {"Where is the nearest tourist information center?", "Spanish: ¿Dónde está el centro de información turística más cercano?", "Mandarin: 最近的旅游信息中心在哪里？(Zuì jìn de lǚyóu xìnxī zhōngxīn zài nǎlǐ?)"},
        {"How do I get to the nearest subway?", "Spanish: ¿Cómo llego al metro más cercano?", "Mandarin: 怎么到最近的地铁？(Zěnme dào zuì jìn de dìtiě?)"}
        };
        for (String[] directionsandPlaces : data) {
            String directionsandPlacesText = directionsandPlaces[0];
            directionsandPlacesList.add(directionsandPlacesText);
            List<String> translations = new ArrayList<>();
            translations.add(directionsandPlaces[1]);
            translations.add(directionsandPlaces[2]);
            translationsMap.put(directionsandPlacesText, translations);
        }
    }

    private void toggleFavoriteStatus(int groupPosition) {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        // Get the current favorites
        String favoritesJson = prefs.getString("favorites_DirectionsandPlaces", "");
        Map<String, List<String>> favorites = favoritesJson.isEmpty() ?
                new HashMap<>() : gson.fromJson(favoritesJson, new TypeToken<Map<String, List<String>>>(){}.getType());

        String directionsandPlace = directionsandPlacesList.get(groupPosition);
        boolean isFavorite = favorites.containsKey(directionsandPlace);
        if (isFavorite) {
            favorites.remove(directionsandPlace);
        } else {
            favorites.put(directionsandPlace, translationsMap.get(directionsandPlace));
        }

        // Save updated favorites
        editor.putString("favorites_DirectionsandPlaces", gson.toJson(favorites));

        // Save the favorite status for this specific item
        editor.putBoolean("favorite_" + directionsandPlace, !isFavorite);

        editor.apply();

        // Update the icon based on the new favorite status
        updateFavoriteIcon(groupPosition, !isFavorite);
    }

    private void updateFavoriteIcon(int groupPosition, boolean isFavorite) {
        View groupView = expandableListView.getChildAt(groupPosition - expandableListView.getFirstVisiblePosition());
        if (groupView != null) {
            ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);
            favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled:  R.drawable.ic_favorite_empty );
        }
    }


    private void initializeFavoriteIcons() {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String favoritesJson = prefs.getString("favorites_DirectionsandPlaces", "");
        Map<String, List<String>> favorites = favoritesJson.isEmpty() ?
                new HashMap<>() : gson.fromJson(favoritesJson, new TypeToken<Map<String, List<String>>>(){}.getType());

        for (int i = 0; i < directionsandPlacesList.size(); i++) {
            String directionsandPlace = directionsandPlacesList.get(i);
            boolean isFavorite = favorites.containsKey(directionsandPlace);
            updateFavoriteIcon(i, isFavorite);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initializeFavoriteIcons();
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