package com.IT4A.langhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import android.widget.SeekBar;
import android.widget.TextView;

public class FavoritesActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech textToSpeech;
    private List<String> favoritesList = new ArrayList<>();
    private HashMap<String, List<String>> translationsMap = new HashMap<>();
    private FavoritesExpandableListAdapter adapter;
    private ExpandableListView expandableListView;
    private int lastExpandedGroupPosition = -1;
    private SeekBar ttsSpeedSeekBar;
    private TextView ttsSpeedText;
    private float ttsSpeed = 1.0f;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

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
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setTitle("Favorites");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));

        // Initialize favorites list and translations map
        favoritesList = new ArrayList<>();
        translationsMap = new HashMap<>();

        // Load favorites from SharedPreferences
        loadFavorites();

        // Set up the ExpandableListView with custom adapter
        expandableListView = findViewById(R.id.expandableListView);
        adapter = new FavoritesExpandableListAdapter(this, favoritesList, translationsMap);
        expandableListView.setAdapter(adapter);

        // Set up click listeners


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
            boolean isExpanded = ((ExpandableListView) parent).isGroupExpanded(groupPosition);
            ((ExpandableListView) parent).collapseGroup(groupPosition);

            View groupView = parent.getChildAt(groupPosition - parent.getFirstVisiblePosition());
            if (groupView != null) {
                ImageButton favoriteIcon = groupView.findViewById(R.id.favoriteIcon);

                // Set an OnClickListener for the favorite icon (ImageButton)
                favoriteIcon.setOnClickListener(view1 -> {
                    removeFavorite(groupPosition);
                });

                // Set the initial favorite icon state (always filled in FavoritesActivity)
                favoriteIcon.setImageResource(R.drawable.ic_favorite_filled);

                // Update visibility based on expanded state
                favoriteIcon.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
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



        ImageView searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            // Proceed to SearchPage Activity directly
            Intent intent = new Intent(FavoritesActivity.this, com.IT4A.langhub.SearchPage.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });

        // Help button functionality
        ImageView helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(view -> {
            Intent intent = new Intent(FavoritesActivity.this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView userButton = findViewById(R.id.user_button);
        userButton.setOnClickListener(view -> {
            Intent intent = new Intent(FavoritesActivity.this, userActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(view -> {
            Intent intent = new Intent(FavoritesActivity.this, HomepageActivity.class);
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

    private void loadFavorites() {
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        Gson gson = new Gson();

        String favoritesJson = prefs.getString("favorites_Search", "");
        if (!favoritesJson.isEmpty()) {
            LinkedHashMap<String, List<String>> favorites = gson.fromJson(favoritesJson,
                    new TypeToken<LinkedHashMap<String, List<String>>>() {}.getType());

            // Add favorites in reverse order (most recent first)
            List<String> keys = new ArrayList<>(favorites.keySet());
            for (int i = keys.size() - 1; i >= 0; i--) {
                String key = keys.get(i);
                favoritesList.add(key);
                translationsMap.put(key, favorites.get(key));
            }
        }
    }


    private void removeFavorite(int groupPosition) {
        String favoriteToRemove = favoritesList.get(groupPosition);
        favoritesList.remove(groupPosition);
        translationsMap.remove(favoriteToRemove);

        // Update SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyAppPreferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();

        String favoritesJson = prefs.getString("favorites_Search", "");
        if (!favoritesJson.isEmpty()) {
            LinkedHashMap<String, List<String>> favorites = gson.fromJson(favoritesJson,
                    new TypeToken<LinkedHashMap<String, List<String>>>(){}.getType());
            favorites.remove(favoriteToRemove);
            editor.putString("favorites_Search", gson.toJson(favorites));
        }

        editor.remove("favorite_" + favoriteToRemove);
        editor.apply();

        // Notify adapter of data change
        adapter.notifyDataSetChanged();
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
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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

