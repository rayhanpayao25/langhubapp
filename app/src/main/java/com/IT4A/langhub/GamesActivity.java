package com.IT4A.langhub;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.widget.Toolbar;

public class GamesActivity extends AppCompatActivity {

    private AlertDialog historyDialog;
    private QuizDatabaseHelper dbHelper;
    private Spinner spinnerLanguage;
    private TextView historyTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_games);


        Toolbar toolbar = findViewById(R.id.appBar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle("Games");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        dbHelper = new QuizDatabaseHelper(this);

        Button usaButton = findViewById(R.id.usa);
        Button chinaButton = findViewById(R.id.china);
        Button spainButton = findViewById(R.id.spain);
        AppCompatImageButton historyButton = findViewById(R.id.Historybutton);

        usaButton.setOnClickListener(v -> startActivity(new android.content.Intent(GamesActivity.this, EnglishActivity.class)));
        chinaButton.setOnClickListener(v -> startActivity(new android.content.Intent(GamesActivity.this, ChineseActivity.class)));
        spainButton.setOnClickListener(v -> startActivity(new android.content.Intent(GamesActivity.this, SpanishActivity.class)));

        historyButton.setOnClickListener(v -> showHistoryDialog());
    }

    private void showHistoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quiz_history, null);
        spinnerLanguage = dialogView.findViewById(R.id.spinner_level);
        historyTextView = dialogView.findViewById(R.id.history_message);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerLanguage.setAdapter(adapter);

        spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = parent.getItemAtPosition(position).toString();
                updateHistoryMessage(selectedLanguage);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        historyDialog = builder.create();

        if (historyDialog.getWindow() != null) {
            historyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        historyDialog.show();

        MaterialButton deleteAllButton = dialogView.findViewById(R.id.btn_delete_all);
        MaterialButton closeButton = dialogView.findViewById(R.id.btn_close);

        deleteAllButton.setOnClickListener(v -> {
            String selectedLanguage = spinnerLanguage.getSelectedItem().toString();
            dbHelper.deleteHistoryByLanguage(selectedLanguage);
            updateHistoryMessage(selectedLanguage);
        });

        closeButton.setOnClickListener(v -> historyDialog.dismiss());

        updateHistoryMessage(spinnerLanguage.getSelectedItem().toString());
    }

    private void updateHistoryMessage(String selectedLanguage) {
        StringBuilder historyMessage = new StringBuilder();
        historyMessage.append("Your Current Score\n\n");
        historyMessage.append(String.format("%-10s %-20s %-20s %-15s\n", "Score", "Date", "     P/F", "Difficulty level" ));

        Cursor cursor = dbHelper.getTopQuizHistory(100);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String language = cursor.getString(cursor.getColumnIndex(QuizDatabaseHelper.COLUMN_NAME_LANGUAGE));
                if (selectedLanguage.equalsIgnoreCase(language)) {
                    @SuppressLint("Range") int score = cursor.getInt(cursor.getColumnIndex(QuizDatabaseHelper.COLUMN_NAME_SCORE));
                    @SuppressLint("Range") int totalQuestions = cursor.getInt(cursor.getColumnIndex(QuizDatabaseHelper.COLUMN_NAME_TOTAL_QUESTIONS));
                    @SuppressLint("Range") long timestamp = cursor.getLong(cursor.getColumnIndex(QuizDatabaseHelper.COLUMN_NAME_TIMESTAMP));
                    @SuppressLint("Range") String level = cursor.getString(cursor.getColumnIndex(QuizDatabaseHelper.COLUMN_NAME_LEVEL));

                    String formattedScore = String.format("%02d/%02d", score, totalQuestions);
                    String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
                    boolean passed = (score >= totalQuestions / 2);
                    String passedFailed = passed ? "P" : "F";

                    String line = String.format("%-10s %-20s %-20s %-15s\n", formattedScore, formattedDate,  passedFailed, level);
                    SpannableString spannableLine = new SpannableString(line);

                    int start = line.indexOf(passedFailed);
                    int end = start + passedFailed.length();
                    int color = passed ? Color.parseColor("#4CAF50") : Color.parseColor("#F44336");
                    spannableLine.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    historyMessage.append(spannableLine);
                }
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            historyMessage.append("No history data available.\n");
        }

        if (historyTextView != null) {
            historyTextView.setText(historyMessage);
        }
    }

    @Override
    public void onBackPressed() {
        if (historyDialog != null && historyDialog.isShowing()) {
            historyDialog.dismiss();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
