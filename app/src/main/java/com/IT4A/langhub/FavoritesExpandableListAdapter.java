package com.IT4A.langhub;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> favoritesList;
    private Map<String, List<String>> translationsMap;
    private List<String> filteredFavoritesList;

    public FavoritesExpandableListAdapter(Context context, List<String> favoritesList, Map<String, List<String>> translationsMap) {
        this.context = context;
        this.favoritesList = favoritesList;
        this.translationsMap = translationsMap;
        this.filteredFavoritesList = favoritesList; // Start with the full list
    }

    @Override
    public int getGroupCount() {
        return filteredFavoritesList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 0;  // No children to display as we are handling translations inside the group
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filteredFavoritesList.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return null; // No children to return
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.group_items, parent, false);
        }

        ImageView favoriteIcon = convertView.findViewById(R.id.favoriteIcon);
        LinearLayout titleLayout = (LinearLayout) convertView.findViewById(R.id.groupTitle).getParent();

        // Set group title (e.g., "Hello")
        TextView title = convertView.findViewById(R.id.groupTitle);
        title.setText(filteredFavoritesList.get(groupPosition));

        // Set Spanish and Mandarin translations (only display translated text)
        TextView spanishText = convertView.findViewById(R.id.spanishTranslation);
        TextView mandarinText = convertView.findViewById(R.id.mandarinTranslation);
        LinearLayout expandedContent = convertView.findViewById(R.id.expandedContent);

        // Get translations (Spanish and Mandarin)
        String spanishTranslation = translationsMap.get(filteredFavoritesList.get(groupPosition)).get(0).replace("Spanish: ", "");
        String mandarinTranslation = translationsMap.get(filteredFavoritesList.get(groupPosition)).get(1).replace("Mandarin: ", "");

        // Set the translated text with formatting
        spanishText.setText(formatTranslationText("Spanish", spanishTranslation));
        mandarinText.setText(formatTranslationText("Mandarin", mandarinTranslation));

        // Handle expanded and collapsed states
        if (isExpanded) {
            expandedContent.setVisibility(View.VISIBLE); // Show the expanded content
            favoriteIcon.setVisibility(View.VISIBLE); // Show the favorite icon
            titleLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_background_expanded)); // Apply the expanded background
        } else {
            expandedContent.setVisibility(View.GONE); // Hide the expanded content
            favoriteIcon.setVisibility(View.GONE); // Hide the favorite icon
            titleLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_background)); // Apply the default background
        }

        if (isExpanded) {
            title.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            title.setTextColor(ContextCompat.getColor(context, R.color.gray_text));
        }

        // Speaker icon click listener (Spanish)
        ImageView spanishSpeakerIcon = convertView.findViewById(R.id.spanishSpeakerIcon);
        spanishSpeakerIcon.setOnClickListener(v -> {
            // Get the Spanish translation and speak it
            String spanishTextToSpeak = translationsMap.get(filteredFavoritesList.get(groupPosition)).get(0);
            if (context instanceof FavoritesActivity) {
                ((FavoritesActivity) context).speakText(spanishTextToSpeak);
            }
        });

        // Speaker icon click listener (Mandarin)
        ImageView mandarinSpeakerIcon = convertView.findViewById(R.id.mandarinSpeakerIcon);
        mandarinSpeakerIcon.setOnClickListener(v -> {
            // Get the Mandarin translation and speak it
            String mandarinTextToSpeak = translationsMap.get(filteredFavoritesList.get(groupPosition)).get(1);
            if (context instanceof FavoritesActivity) {
                ((FavoritesActivity) context).speakText(mandarinTextToSpeak);
            }
        });

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        return null;  // No child views, as translations are displayed directly in the group view
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;  // No child selection needed
    }

    public void filterList(String query) {
        if (query.isEmpty()) {
            filteredFavoritesList = favoritesList;  // Return to the full list if query is empty
        } else {
            List<String> filteredList = new ArrayList<>();

            for (String favorite : favoritesList) {
                if (favorite.toLowerCase().contains(query.toLowerCase()) ||
                        translationsMap.get(favorite).get(0).toLowerCase().contains(query.toLowerCase()) ||  // Check Spanish translation
                        translationsMap.get(favorite).get(1).toLowerCase().contains(query.toLowerCase())) {  // Check Mandarin translation
                    filteredList.add(favorite);
                }
            }
            filteredFavoritesList = filteredList;  // Set filtered list
        }
        notifyDataSetChanged();  // Notify the adapter to refresh the view
    }

    private CharSequence formatTranslationText(String language, String translation) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        // Add bold language label
        SpannableString languageSpan = new SpannableString(language + ": ");
        languageSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, languageSpan.length(), 0);
        builder.append(languageSpan);

        // Add translation in normal text
        builder.append(translation);

        // Add speaker icon
        builder.append("");

        return builder;
    }
}

