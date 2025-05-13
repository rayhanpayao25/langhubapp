package com.IT4A.langhub;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> originalSearchList;
    private List<String> filteredSearchList;
    private Map<String, List<String>> translationsMap;
    private ExpandableListView expandableListView;

    public SearchExpandableListAdapter(Context context, List<String> searchList, Map<String, List<String>> translationsMap, ExpandableListView expandableListView) {
        this.context = context;
        this.originalSearchList = new ArrayList<>(searchList);
        this.filteredSearchList = new ArrayList<>(searchList);
        this.expandableListView = expandableListView;
        this.translationsMap = translationsMap;
    }




    @Override
    public int getGroupCount() {
        return filteredSearchList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 0;  // No children to display as we are handling translations inside the group
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filteredSearchList.get(groupPosition);
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
        title.setText(filteredSearchList.get(groupPosition));

        // Set Spanish and Mandarin translations (only display translated text)
        TextView spanishText = convertView.findViewById(R.id.spanishTranslation);
        TextView mandarinText = convertView.findViewById(R.id.mandarinTranslation);
        LinearLayout expandedContent = convertView.findViewById(R.id.expandedContent);

        // Get translations (Spanish and Mandarin)
        String spanishTranslation = translationsMap.get(filteredSearchList.get(groupPosition)).get(0).replace("Spanish: ", "");
        String mandarinTranslation = translationsMap.get(filteredSearchList.get(groupPosition)).get(1).replace("Mandarin: ", "");

        // Set the translated text
        spanishText.setText("Spanish: " + spanishTranslation);
        mandarinText.setText("Mandarin: " + mandarinTranslation);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            spanishText.setText(Html.fromHtml("<b>Spanish: </b>" + spanishTranslation, Html.FROM_HTML_MODE_LEGACY));
            mandarinText.setText(Html.fromHtml("<b>Mandarin: </b>" + mandarinTranslation, Html.FROM_HTML_MODE_LEGACY));
        } else {
            spanishText.setText(Html.fromHtml("<b>Spanish: </b>" + spanishTranslation));
            mandarinText.setText(Html.fromHtml("<b>Mandarin: </b>" + mandarinTranslation));
        }


        if (isExpanded) {
            title.setTextColor(ContextCompat.getColor(context, android.R.color.white));
        } else {
            title.setTextColor(ContextCompat.getColor(context, R.color.gray_text));
        }

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

        // Speaker icon click listener (Spanish)
        ImageView spanishSpeakerIcon = convertView.findViewById(R.id.spanishSpeakerIcon);
        spanishSpeakerIcon.setOnClickListener(v -> {
            // Get the Spanish translation and speak it
            String spanishTextToSpeak = translationsMap.get(filteredSearchList.get(groupPosition)).get(0);
            if (context instanceof SearchPage) {
                ((SearchPage) context).speakText(spanishTextToSpeak);
            }
        });


        // Speaker icon click listener (Mandarin)
        ImageView mandarinSpeakerIcon = convertView.findViewById(R.id.mandarinSpeakerIcon);
        mandarinSpeakerIcon.setOnClickListener(v -> {
            // Get the Mandarin translation and speak it
            String mandarinTextToSpeak = translationsMap.get(filteredSearchList.get(groupPosition)).get(1);
            if (context instanceof SearchPage) {
                ((SearchPage) context).speakText(mandarinTextToSpeak);
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

    public List<String> filterList(String text) {
        filteredSearchList.clear();
        if (text.isEmpty()) {
            filteredSearchList.addAll(originalSearchList);
        } else {
            text = text.toLowerCase();
            for (String item : originalSearchList) {
                if (item.toLowerCase().contains(text) ||
                        translationsMap.get(item).get(0).toLowerCase().contains(text) || // Spanish
                        translationsMap.get(item).get(1).toLowerCase().contains(text)) { // Mandarin
                    filteredSearchList.add(item);
                }
            }
        }
        // Collapse all groups after filtering
        collapseAllGroups();
        notifyDataSetChanged();
        return filteredSearchList;
    }

    private void collapseAllGroups() {
        if (expandableListView != null) {
            for (int i = 0; i < getGroupCount(); i++) {
                expandableListView.collapseGroup(i);
            }
        }
    }
}
