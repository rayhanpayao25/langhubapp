package com.IT4A.langhub;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class ConversationAdapter extends BaseAdapter {
    private Context context;
    private List<HashMap<String, String>> conversation;
    private LayoutInflater inflater;

    public ConversationAdapter(Context context, List<HashMap<String, String>> conversation) {
        this.context = context;
        this.conversation = conversation;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return conversation.size();
    }

    @Override
    public Object getItem(int position) {
        return conversation.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_conversation, parent, false);
            holder = new ViewHolder();
            holder.conversationText = convertView.findViewById(R.id.conversationText);
            holder.spanishText = convertView.findViewById(R.id.spanishText);
            holder.mandarinText = convertView.findViewById(R.id.mandarinText);
            holder.expandButton = convertView.findViewById(R.id.expandButton);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        final HashMap<String, String> conversationMap = conversation.get(position);
        holder.conversationText.setText(conversationMap.get("conversation"));
        holder.spanishText.setText(conversationMap.get("spanish"));
        holder.mandarinText.setText(conversationMap.get("mandarin"));

        // Get current state of expanded view
        boolean isExpanded = conversationMap.containsKey("isExpanded") && conversationMap.get("isExpanded").equals("true");
        holder.spanishText.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        holder.mandarinText.setVisibility(isExpanded ? View.VISIBLE : View.GONE);

        // Button to toggle visibility of translations
        holder.expandButton.setOnClickListener(v -> {
            boolean currentState = conversationMap.containsKey("isExpanded") && conversationMap.get("isExpanded").equals("true");
            // Toggle the expanded state
            conversationMap.put("isExpanded", currentState ? "false" : "true");
            notifyDataSetChanged();  // Update the list view after change
        });

        return convertView;
    }

    static class ViewHolder {
        TextView conversationText;
        TextView spanishText;
        TextView mandarinText;
        Button expandButton;
    }
}
