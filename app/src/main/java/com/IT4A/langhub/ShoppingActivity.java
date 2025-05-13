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

public class ShoppingActivity extends AppCompatActivity implements OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> shoppingList;
    private HashMap<String, List<String>> translationsMap;
    private ShoppingExpandableList adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1; // Variable to track the last expanded group
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);  // Changed to activity_search_page

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        // Set up the AppBar (Toolbar)
        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);  // Set the Toolbar as the action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // Disable default title
        toolbar.setTitle("Shopping");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));



        shoppingList = new ArrayList<>();
        translationsMap = new HashMap<>();
        addShopping();

        // Set up the ExpandableListView with custoem adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new ShoppingExpandableList(this, shoppingList, translationsMap);
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
            String shopping = adapter.getGroup(groupPosition).toString();
            speakText(shopping);

            String translation = (String) adapter.getChild(groupPosition, childPosition);
            speakText(translation); // Speak the translation text

            return true; // Return true to indicate the click event was handled
        });

        expandableListView.setOnGroupClickListener((parent, v, groupPosition, id) -> {
            // Use the filtered list for TTS
            String shopping = adapter.getGroup(groupPosition).toString();
            speakText(shopping);

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


    private void addShopping() {
        String[][] data = {
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
                {"Do you have any pet supplies?", "Spanish: ¿Tienen suministros para mascotas?", "Mandarin: 你们有宠物用品吗？(Nǐmen yǒu chǒngwù yòngpǐn ma?)"}
        };


        for (String[] shopping : data) {
            String shoppingText = shopping[0];
            shoppingList.add(shoppingText);
            List<String> translations = new ArrayList<>();
            translations.add(shopping[1]);
            translations.add(shopping[2]);
            translationsMap.put(shoppingText, translations);
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
