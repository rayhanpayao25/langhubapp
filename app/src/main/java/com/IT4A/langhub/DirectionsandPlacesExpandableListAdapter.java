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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectionsandPlacesExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private List<String> directionsandPlacesList;
    private Map<String, List<String>> translationsMap;
    private List<String> filtereddirectionsandPlacesList;

    public DirectionsandPlacesExpandableListAdapter(Context context, List<String> directionsandPlacesList, Map<String, List<String>> translationsMap) {
        this.context = context;
        this.directionsandPlacesList = directionsandPlacesList;
        this.translationsMap = translationsMap;
        this.filtereddirectionsandPlacesList = directionsandPlacesList; // Start with the full list
    }

    @Override
    public int getGroupCount() {
        return filtereddirectionsandPlacesList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 0;  // No children to display as we are handling translations inside the group
    }

    @Override
    public Object getGroup(int groupPosition) {
        return filtereddirectionsandPlacesList.get(groupPosition);
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
            convertView = inflater.inflate(R.layout.group_item, parent, false);
        }


        LinearLayout titleLayout = (LinearLayout) convertView.findViewById(R.id.groupTitle).getParent();

        // Set group title (e.g., "Hello")
        TextView title = convertView.findViewById(R.id.groupTitle);
        title.setText(filtereddirectionsandPlacesList.get(groupPosition));

        // Set Spanish and Mandarin translations (only display translated text)
        TextView spanishText = convertView.findViewById(R.id.spanishTranslation);
        TextView mandarinText = convertView.findViewById(R.id.mandarinTranslation);
        LinearLayout expandedContent = convertView.findViewById(R.id.expandedContent);

        // Get translations (Spanish and Mandarin)
        String spanishTranslation = translationsMap.get(filtereddirectionsandPlacesList.get(groupPosition)).get(0).replace("Spanish: ", "");
        String mandarinTranslation = translationsMap.get(filtereddirectionsandPlacesList.get(groupPosition)).get(1).replace("Mandarin: ", "");

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
            titleLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_background_expanded)); // Apply the expanded background
        } else {
            expandedContent.setVisibility(View.GONE); // Hide the expanded content
            titleLayout.setBackground(ContextCompat.getDrawable(context, R.drawable.rounded_background)); // Apply the default background
        }

        // Speaker icon click listener (Spanish)
        ImageView spanishSpeakerIcon = convertView.findViewById(R.id.spanishSpeakerIcon);
        spanishSpeakerIcon.setOnClickListener(v -> {
            // Get the Spanish translation and speak it
            String spanishTextToSpeak = translationsMap.get(filtereddirectionsandPlacesList.get(groupPosition)).get(0);
            if (context instanceof DirectionsandPlaces) {
                ((DirectionsandPlaces) context).speakText(spanishTextToSpeak);
            }
        });

        // Speaker icon click listener (Mandarin)
        ImageView mandarinSpeakerIcon = convertView.findViewById(R.id.mandarinSpeakerIcon);
        mandarinSpeakerIcon.setOnClickListener(v -> {
            // Get the Mandarin translation and speak it
            String mandarinTextToSpeak = translationsMap.get(filtereddirectionsandPlacesList.get(groupPosition)).get(1);
            if (context instanceof DirectionsandPlaces) {
                ((DirectionsandPlaces) context).speakText(mandarinTextToSpeak);
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
            filtereddirectionsandPlacesList = directionsandPlacesList;  // Return to the full list if query is empty
        } else {
            List<String> filteredList = new ArrayList<>();

            for (String directionsandPlaces : directionsandPlacesList) {
                if (directionsandPlaces.toLowerCase().contains(query.toLowerCase()) ||
                        translationsMap.get(directionsandPlaces).get(0).toLowerCase().contains(query.toLowerCase()) ||  // Check Spanish translation
                        translationsMap.get(directionsandPlaces).get(1).toLowerCase().contains(query.toLowerCase())) {  // Check Mandarin translation
                    filteredList.add(directionsandPlaces);
                }
            }
            filtereddirectionsandPlacesList = filteredList;  // Set filtered list
        }
        notifyDataSetChanged();  // Notify the adapter to refresh the view
    }
}

