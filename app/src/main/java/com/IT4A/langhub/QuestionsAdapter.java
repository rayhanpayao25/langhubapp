package com.IT4A.langhub;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class QuestionsAdapter extends RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder> {

    private List<Question> questionsList;
    private OnDeleteClickListener deleteListener;
    private OnEditClickListener editClickListener;

    public interface OnDeleteClickListener {
        void onDeleteClick(String questionId);
    }

    public interface OnEditClickListener {
        void onEditClick(Question question);
    }

    public QuestionsAdapter(List<Question> questionsList, OnDeleteClickListener deleteListener, OnEditClickListener editClickListener) {
        this.questionsList = questionsList;
        this.deleteListener = deleteListener;
        this.editClickListener = editClickListener;
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questionsList.get(position);
        holder.questionTextView.setText(question.getQuestion());
        holder.questionTypeTextView.setText(question.getQuestionType());
        holder.difficultyTextView.setText(question.getDifficulty());

        int difficultyColor;
        switch (question.getDifficulty().toLowerCase()) {
            case "easy":
                difficultyColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.easy_color);
                break;
            case "normal":
                difficultyColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.medium_color);
                break;
            case "hard":
                difficultyColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.hard_color);
                break;
            default:
                difficultyColor = Color.GRAY;
                break;
        }

        holder.difficultyTextView.setBackgroundTintList(ColorStateList.valueOf(difficultyColor));

        // UPDATED IMAGE LOADING LOGIC
        String imgName = question.getImgName();
        String imageUrl = question.getImgUrl();

        if (!TextUtils.isEmpty(imgName)) {
            // First try to load from drawable resources
            int resourceId = holder.itemView.getContext().getResources().getIdentifier(
                    imgName, "drawable", holder.itemView.getContext().getPackageName());

            if (resourceId != 0) {
                // Resource exists in drawable, load it
                holder.questionImageView.setImageResource(resourceId);
                holder.questionImageView.setVisibility(View.VISIBLE);
            } else {
                // If not in drawable, try to load from assets
                try {
                    InputStream is = holder.itemView.getContext().getAssets().open("images/" + imgName);
                    Bitmap bitmap = BitmapFactory.decodeStream(is);
                    holder.questionImageView.setImageBitmap(bitmap);
                    holder.questionImageView.setVisibility(View.VISIBLE);
                    is.close();
                } catch (IOException e) {
                    Log.e("QuestionsAdapter", "Error loading image: " + e.getMessage());

                    // If asset loading fails and we have a URL, try that as fallback
                    if (!TextUtils.isEmpty(imageUrl)) {
                        Glide.with(holder.itemView.getContext())
                                .load(imageUrl)
                                .placeholder(android.R.drawable.ic_menu_report_image)
                                .into(holder.questionImageView);
                        holder.questionImageView.setVisibility(View.VISIBLE);
                    } else {
                        holder.questionImageView.setVisibility(View.GONE);
                    }
                }
            }
        } else if (!TextUtils.isEmpty(imageUrl)) {
            // Load from URL using Glide
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .into(holder.questionImageView);
            holder.questionImageView.setVisibility(View.VISIBLE);
        } else {
            holder.questionImageView.setVisibility(View.GONE);
        }

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClick(question.getId());
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (editClickListener != null) {
                editClickListener.onEditClick(question);
            }
        });
    }
    @Override
    public int getItemCount() {
        return questionsList.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView questionTextView, questionTypeTextView, difficultyTextView;
        ImageView questionImageView;
        ImageView deleteButton;
        ImageView editButton;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTextView = itemView.findViewById(R.id.questionTextView);
            questionTypeTextView = itemView.findViewById(R.id.questionTypeTextView);
            difficultyTextView = itemView.findViewById(R.id.difficultyTextView);
            questionImageView = itemView.findViewById(R.id.questionImageView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
}