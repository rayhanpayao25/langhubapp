package com.IT4A.langhub;

import android.content.Intent;
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
import android.widget.ImageView;
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

public class SearchPage extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> searchList;
    private HashMap<String, List<String>> translationsMap;
    private SearchExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1;
    private List<String> originalSearchList;
    private List<String> filteredSearchList;
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported");
                }
            } else {
                Log.e("TTS", "Initialization failed");
            }
        });


        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar

        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        searchList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addSearch();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new SearchExpandableListAdapter(this, searchList, translationsMap, expandableListView);
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
                String searchText = charSequence.toString().toLowerCase().trim();
                filteredSearchList = adapter.filterList(searchText);
                // Remove the expansion of all groups here
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        expandableListView.setOnChildClickListener((parent, view, groupPosition, childPosition, id) -> {
            // Use the filtered list for TTS
            String search = adapter.getGroup(groupPosition).toString();
            speakText(search);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, view, groupPosition, id) -> {
            // Use the filtered list for TTS
            String search = adapter.getGroup(groupPosition).toString();
            speakText(search);

            // Check if the clicked group is already expanded
            boolean isExpanded = expandableListView.isGroupExpanded(groupPosition);

            // Update the favorites icon visibility
            View groupView = parent.getChildAt(groupPosition - parent.getFirstVisiblePosition());
            if (groupView != null) {
                ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);

                // Set an OnClickListener for the favorite icon (ImageButton)
                favoriteIcon.setOnClickListener(view1 -> {
                    toggleFavoriteStatus(search);
                });

                // Set the initial favorite icon state
                SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
                boolean isFavorite = prefs.getBoolean("favorite_" + search, false);
                favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_empty);

                // Update visibility based on expanded state
                favoriteIcon.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            }

            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition);
            } else {
                expandableListView.expandGroup(groupPosition);
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

            updateGroupView(groupPosition, expandableListView.isGroupExpanded(groupPosition));

            return true;
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

        ImageView userutton = findViewById(R.id.user_button);
        userutton.setOnClickListener(v -> {
            // Proceed to SearchPage Activity directly
            Intent intent = new Intent(SearchPage.this, com.IT4A.langhub.userActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });

        // Help button functionality
        ImageView helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(view -> {
            Intent intent = new Intent(SearchPage.this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView favoritesButton = findViewById(R.id.favorite_button);
        favoritesButton.setOnClickListener(view -> {
            Intent intent = new Intent(SearchPage.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(view -> {
            Intent intent = new Intent(SearchPage.this, HomepageActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
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

    private void updateGroupView(int groupPosition, boolean isExpanded) {
        View groupView = expandableListView.getChildAt(groupPosition - expandableListView.getFirstVisiblePosition());
        if (groupView != null) {
            ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);
            favoriteIcon.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

            // Update the border color
            updateBorderColor(groupPosition, isExpanded);
        }
    }

    private void addSearch() {
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
                {"Steel blue", "Spanish: Azul acero", "Mandarin: 钢铁蓝色 (Gāngtiě lán sè)"},
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
                {"Please repeat that", "Spanish: Por favor, repítelo", "Mandarin: 请重复一下 (Qǐng chóngfù yīxià)"},
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
                {"How do I get to the nearest subway?", "Spanish: ¿Cómo llego al metro más cercano?", "Mandarin: 怎么到最近的地铁？(Zěnme dào zuì jìn de dìtiě?)"},
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
                {"Do you have soy milk?", "Spanish: ¿Tienen leche de soja?", "Mandarin: 你们有豆浆吗？(Nǐmen yǒu dòujiāng ma?)"},
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
                {"I like it", "Spanish: Me gusta", "Mandarin: 我喜欢 (Wǒ xǐhuān)"},
                {"I don't like it", "Spanish: No me gusta", "Mandarin: 我不喜欢 (Wǒ bù xǐhuān)"},
                {"I understand", "Spanish: Entiendo", "Mandarin: 我明白 (Wǒ míngbái)"},
                {"I don't know", "Spanish: No sé", "Mandarin: 我不知道 (Wǒ bù zhīdào)"},
                {"What is this?", "Spanish: ¿Qué es esto?", "Mandarin: 这是什么？(Zhè shì shénme?)"},
                {"It's a ...", "Spanish: Es un ...", "Mandarin: 这是一个... (Zhè shì yīgè...)"},
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
                {"Thousand", "Spanish: Mil", "Mandarin: 一千 (Yī qiān)"},
                {"Do you have any organic products?", "Spanish: ¿Tienen productos orgánicos?", "Mandarin: 你们有有机产品吗？(Nǐmen yǒu yǒujī chǎnpǐn ma?)"},
                {"Can I pay with a credit card?", "Spanish: ¿Puedo pagar con tarjeta de crédito?", "Mandarin: 我可以用信用卡付款吗？(Wǒ kěyǐ yòng xìnyòngkǎ fùkuǎn ma?)"},
                {"Where are the bathroom products?", "Spanish: ¿Dónde están los productos de baño?", "Mandarin: 洗浴产品在哪里？(Xǐyù chǎnpǐn zài nǎlǐ?)"},
                {"Do you have any vegan options?", "Spanish: ¿Tienen opciones veganas?", "Mandarin: 你们有素食选择吗？(Nǐmen yǒu sùshí xuǎnzé ma?)"},
                {"How much is this item?", "Spanish: ¿Cuánto cuesta este artículo?", "Mandarin: 这个商品多少钱？(Zhège shāngpǐn duōshǎo qián?)"},
                {"Where can I find the milk?", "Spanish: ¿Dónde puedo encontrar la leche?", "Mandarin: 牛奶在哪里？(Niúnǎi zài nǎlǐ?)"},
                {"Do you have any gluten-free snacks?", "Spanish: ¿Tienen bocadillos sin gluten?", "Mandarin: 你们有无麸质零食吗？(Nǐmen yǒu wú fūzhì língshí ma?)"},
                {"Can I try this on?", "Spanish: ¿Puedo probármelo?", "Mandarin: 我可以试穿这个吗？(Wǒ kěyǐ shì chuān zhège ma?)"},
                {"Do you have any discounted items?", "Spanish: ¿Tienen artículos con descuento?", "Mandarin: 你们有打折商品吗？(Nǐmen yǒu dǎzhé shāngpǐn ma?)"},
                {"Are these items on sale?", "Spanish: ¿Están estos artículos en oferta?", "Mandarin: 这些商品正在促销吗？(Zhèxiē shāngpǐn zhèngzài cùxiāo ma?)"},
                {"Where can I find the shoes?", "Spanish: ¿Dónde puedo encontrar los zapatos?", "Mandarin: 鞋子在哪里？(Xiézi zài nǎlǐ?)"},
                {"Do you have any sugar-free options?", "Spanish: ¿Tienen opciones sin azúcar?", "Mandarin: 你们有无糖选项吗？(Nǐmen yǒu wútáng xuǎnxiàng ma?)"},
                {"Is this item available in other colors?", "Spanish: ¿Este artículo está disponible en otros colores?", "Mandarin: 这个商品有其他颜色吗？(Zhège shāngpǐn yǒu qítā yánsè ma?)"},
                {"Can I get a refund?", "Spanish: ¿Puedo obtener un reembolso?", "Mandarin: 我可以退款吗？(Wǒ kěyǐ tuìkuǎn ma?)"},
                {"Where is the customer service desk?", "Spanish: ¿Dónde está el mostrador de servicio al cliente?", "Mandarin: 客服台在哪里？(Kèfú tái zài nǎlǐ?)"},
                {"Do you have any seasonal items?", "Spanish: ¿Tienen artículos de temporada?", "Mandarin: 你们有季节性商品吗？(Nǐmen yǒu jìjiéxìng shāngpǐn ma?)"},
                {"Is this item in stock?", "Spanish: ¿Este artículo está en stock?", "Mandarin: 这个商品有货吗？(Zhège shāngpǐn yǒu huò ma?)"},
                {"Do you have any clothing for kids?", "Spanish: ¿Tienen ropa para niños?", "Mandarin: 你们有儿童衣服吗？(Nǐmen yǒu értóng yīfú ma?)"},
                {"Do you have any large sizes?", "Spanish: ¿Tienen tallas grandes?", "Mandarin: 你们有大号吗？(Nǐmen yǒu dà hào ma?)"},
                {"Is this a new product?", "Spanish: ¿Este producto es nuevo?", "Mandarin: 这是新产品吗？(Zhè shì xīn chǎnpǐn ma?)"},
                {"Do you offer free shipping?", "Spanish: ¿Ofrecen envío gratis?", "Mandarin: 你们提供免费送货服务吗？(Nǐmen tígōng miǎnfèi sòng huò fúwù ma?)"},
                {"Where is the checkout?", "Spanish: ¿Dónde está la caja?", "Mandarin: 结账处在哪里？(Jiézhàng chù zài nǎlǐ?)"},
                {"Can I get this delivered?", "Spanish: ¿Puedo recibir este pedido a domicilio?", "Mandarin: 我可以送货上门吗？(Wǒ kěyǐ sòng huò shàngmén ma?)"},
                {"Do you have any promotional offers?", "Spanish: ¿Tienen ofertas promocionales?", "Mandarin: 你们有促销优惠吗？(Nǐmen yǒu cùxiāo yōuhuì ma?)"},
                {"Can I use a discount code?", "Spanish: ¿Puedo usar un código de descuento?", "Mandarin: 我可以使用折扣码吗？(Wǒ kěyǐ shǐyòng zhékòu mǎ ma?)"},
                {"Is there a warranty for this item?", "Spanish: ¿Este artículo tiene garantía?", "Mandarin: 这个商品有保修吗？(Zhège shāngpǐn yǒu bǎoxiū ma?)"},
                {"Do you have any gift cards?", "Spanish: ¿Tienen tarjetas de regalo?", "Mandarin: 你们有礼品卡吗？(Nǐmen yǒu lǐpǐn kǎ ma?)"},
                {"Do you offer home delivery?", "Spanish: ¿Ofrecen entrega a domicilio?", "Mandarin: 你们提供送货上门服务吗？(Nǐmen tígōng sòng huò shàngmén fúwù ma?)"},
                {"Can I pay with cash?", "Spanish: ¿Puedo pagar en efectivo?", "Mandarin: 我可以用现金付款吗？(Wǒ kěyǐ yòng xiànjīn fùkuǎn ma?)"},
                {"Do you have any electronic gadgets?", "Spanish: ¿Tienen dispositivos electrónicos?", "Mandarin: 你们有电子产品吗？(Nǐmen yǒu diànzǐ chǎnpǐn ma?)"},
                {"Can I get a gift receipt?", "Spanish: ¿Puedo obtener un recibo de regalo?", "Mandarin: 我可以获得礼品收据吗？(Wǒ kěyǐ huòdé lǐpǐn shōujù ma?)"},
                {"Where can I find the beauty products?", "Spanish: ¿Dónde puedo encontrar los productos de belleza?", "Mandarin: 美容产品在哪里？(Měiróng chǎnpǐn zài nǎlǐ?)"},
                {"Do you have any environmentally-friendly products?", "Spanish: ¿Tienen productos ecológicos?", "Mandarin: 你们有环保产品吗？(Nǐmen yǒu huánbǎo chǎnpǐn ma?)"},
                {"Can I return this item?", "Spanish: ¿Puedo devolver este artículo?", "Mandarin: 我可以退货吗？(Wǒ kěyǐ tuìhuò ma?)"},
                {"Do you have any clearance items?", "Spanish: ¿Tienen artículos en liquidación?", "Mandarin: 你们有清仓商品吗？(Nǐmen yǒu qīngcāng shāngpǐn ma?)"},
                {"Where are the kitchen appliances?", "Spanish: ¿Dónde están los electrodomésticos?", "Mandarin: 厨房电器在哪里？(Chúfáng diànqì zài nǎlǐ?)"},
                {"Can I get a discount for bulk orders?", "Spanish: ¿Puedo obtener un descuento por compras al por mayor?", "Mandarin: 如果批量购买可以打折吗？(Rúguǒ pīliàng gòumǎi kěyǐ dǎzhé ma?)"},
                {"Is there a size guide?", "Spanish: ¿Hay una guía de tallas?", "Mandarin: 有尺寸指南吗？(Yǒu chǐcùn zhǐnán ma?)"},
                {"Do you have any items on preorder?", "Spanish: ¿Tienen artículos en preventa?", "Mandarin: 你们有预购商品吗？(Nǐmen yǒu yùgòu shāngpǐn ma?)"},
                {"Where are the books?", "Spanish: ¿Dónde están los libros?", "Mandarin: 书籍在哪里？(Shūjí zài nǎlǐ?)"},
                {"Can I get this item gift-wrapped?", "Spanish: ¿Puedo hacer que envuelvan este artículo para regalo?", "Mandarin: 我可以要求包装成礼物吗？(Wǒ kěyǐ yāoqiú bāozhuāng chéng lǐwù ma?)"},
                {"Do you have any home decor?", "Spanish: ¿Tienen decoración para el hogar?", "Mandarin: 你们有家居装饰品吗？(Nǐmen yǒu jiājū zhuāngshì pǐn ma?)"},
                {"Where are the electronic accessories?", "Spanish: ¿Dónde están los accesorios electrónicos?", "Mandarin: 电子配件在哪里？(Diànzǐ pèijiàn zài nǎlǐ?)"},
                {"Can I get this in a different size?", "Spanish: ¿Puedo obtener esto en otro tamaño?", "Mandarin: 我可以换个尺寸吗？(Wǒ kěyǐ huàn gè chǐcùn ma?)"},
                {"Do you have any food items?", "Spanish: ¿Tienen artículos alimenticios?", "Mandarin: 你们有食品吗？(Nǐmen yǒu shípǐn ma?)"},
                {"Where are the perfumes?", "Spanish: ¿Dónde están los perfumes?", "Mandarin: 香水在哪里？(Xiāngshuǐ zài nǎlǐ?)"},
                {"Is this the final price?", "Spanish: ¿Este es el precio final?", "Mandarin: 这是最终价格吗？(Zhè shì zuìzhōng jiàgé ma?)"},
                {"Do you offer gift wrapping?", "Spanish: ¿Ofrecen envoltura de regalo?", "Mandarin: 你们提供礼品包装吗？(Nǐmen tígōng lǐpǐn bāozhuāng ma?)"},
                {"Is this item on backorder?", "Spanish: ¿Este artículo está en pedido pendiente?", "Mandarin: 这个商品有延迟吗？(Zhège shāngpǐn yǒu yánchí ma?)"},
                {"Can I pay in installments?", "Spanish: ¿Puedo pagar en cuotas?", "Mandarin: 我可以分期付款吗？(Wǒ kěyǐ fēnqī fùkuǎn ma?)"},
                {"Do you have any pet supplies?", "Spanish: ¿Tienen suministros para mascotas?", "Mandarin: 你们有宠物用品吗？(Nǐmen yǒu chǒngwù yòngpǐn ma?)"},
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

        for (String[] search : data) {
            String searchText = search[0];
            searchList.add(searchText);
            List<String> translations = new ArrayList<>();
            translations.add(search[1]);
            translations.add(search[2]);
            translationsMap.put(searchText, translations);
        }
    }


    private void toggleFavoriteStatus(String search) {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        // Get the current favorites
        String favoritesJson = prefs.getString("favorites_Search", "");
        Map<String, List<String>> favorites = favoritesJson.isEmpty() ?
                new HashMap<>() : gson.fromJson(favoritesJson, new TypeToken<Map<String, List<String>>>(){}.getType());

        boolean isFavorite = favorites.containsKey(search);
        if (isFavorite) {
            favorites.remove(search);
        } else {
            favorites.put(search, translationsMap.get(search));
        }

        // Save updated favorites
        editor.putString("favorites_Search", gson.toJson(favorites));

        // Save the favorite status for this specific item
        editor.putBoolean("favorite_" + search, !isFavorite);

        editor.apply();

        // Update the icon based on the new favorite status
        updateFavoriteIcon(search, !isFavorite);


    }

    private void updateFavoriteIcon(String search, boolean isFavorite) {
        for (int i = 0; i < expandableListView.getChildCount(); i++) {
            View groupView = expandableListView.getChildAt(i);
            if (groupView != null) {
                ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);
                if (favoriteIcon != null) {
                    favoriteIcon.setImageResource(isFavorite ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_empty);
                }
            }
        }
    }

    private void initializeFavoriteIcons() {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        Gson gson = new Gson();
        String favoritesJson = prefs.getString("favorites_Search", "");
        Map<String, List<String>> favorites = favoritesJson.isEmpty() ?
                new HashMap<>() : gson.fromJson(favoritesJson, new TypeToken<Map<String, List<String>>>(){}.getType());

        for (int i = 0; i < searchList.size(); i++) {
            String search = searchList.get(i);
            boolean isFavorite = favorites.containsKey(search);
            updateFavoriteIcon(search, isFavorite);
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
    public void onBackPressed() {
        // Navigate to device home screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}

