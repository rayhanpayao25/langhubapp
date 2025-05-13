package com.IT4A.langhub;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.*;

import java.lang.reflect.Field;
import java.util.*;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;

public class QuestionsActivity extends AppCompatActivity {

    private EditText questionEditText, newChoiceEditText, fixedAnswerEditText;
    private RadioGroup choicesRadioGroup;
    private Button saveQuestionButton, addChoiceButton;
    private Spinner difficultySpinner, languageSpinner, questionTypeSpinner, drawableResourceSpinner;
    private RecyclerView questionsRecyclerView;
    private FirebaseFirestore db;
    private ArrayList<String> choicesList;
    private String correctAnswer;
    private QuestionsAdapter questionsAdapter;
    private List<Question> questionsList;
    private View multipleChoiceSection;
    private View fixedAnswerLayout;
    private EditText imgUrlEditText, imgNameEditText, drawableSearchEditText;
    private List<String> drawableResourceNames;
    private List<String> filteredDrawableResourceNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_questions);

        db = FirebaseFirestore.getInstance();
        choicesList = new ArrayList<>();
        questionsList = new ArrayList<>();

        // Initialize UI components
        questionEditText = findViewById(R.id.questionEditText);
        newChoiceEditText = findViewById(R.id.newChoiceEditText);
        fixedAnswerEditText = findViewById(R.id.fixedAnswerEditText);
        choicesRadioGroup = findViewById(R.id.choicesRadioGroup);
        difficultySpinner = findViewById(R.id.difficultySpinner);
        languageSpinner = findViewById(R.id.languageSpinner);
        questionTypeSpinner = findViewById(R.id.questionTypeSpinner);
        drawableResourceSpinner = findViewById(R.id.drawableResourceSpinner);
        saveQuestionButton = findViewById(R.id.saveQuestionButton);
        addChoiceButton = findViewById(R.id.addChoiceButton);
        questionsRecyclerView = findViewById(R.id.questionsRecyclerView);
        imgUrlEditText = findViewById(R.id.imgUrlEditText);
        imgNameEditText = findViewById(R.id.imgNameEditText);
        multipleChoiceSection = findViewById(R.id.multipleChoiceSection);
        fixedAnswerLayout = findViewById(R.id.fixedAnswerLayout);
        drawableSearchEditText = findViewById(R.id.drawableSearchEditText);

        // Setup spinners
        setupSpinners();
        loadDrawableResources();

        // Setup RecyclerView
        questionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        questionsAdapter = new QuestionsAdapter(questionsList, this::deleteQuestion, this::openEditDialog);
        questionsRecyclerView.setAdapter(questionsAdapter);

        // Setup button click listeners
        saveQuestionButton.setOnClickListener(v -> saveQuestion());
        addChoiceButton.setOnClickListener(v -> addChoice());

        // Setup spinner listeners
        questionTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateUIForQuestionType();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadQuestions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadQuestions();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Setup search functionality for drawable resources
        drawableSearchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDrawableResources(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Load initial questions
        loadQuestions();
    }

    private void filterDrawableResources(String query) {
        filteredDrawableResourceNames = new ArrayList<>();
        filteredDrawableResourceNames.add("None");

        if (TextUtils.isEmpty(query)) {
            filteredDrawableResourceNames.addAll(drawableResourceNames.subList(1, drawableResourceNames.size()));
        } else {
            for (int i = 1; i < drawableResourceNames.size(); i++) {
                if (drawableResourceNames.get(i).toLowerCase().contains(query.toLowerCase())) {
                    filteredDrawableResourceNames.add(drawableResourceNames.get(i));
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filteredDrawableResourceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drawableResourceSpinner.setAdapter(adapter);

        // Auto-select the first matching item if there are any matches
        if (!TextUtils.isEmpty(query) && filteredDrawableResourceNames.size() > 1) {
            // Select the first match (index 1 since "None" is at index 0)
            drawableResourceSpinner.setSelection(1);
            // Update the imgNameEditText with the selected drawable
            imgNameEditText.setText(filteredDrawableResourceNames.get(1));
            imgUrlEditText.setVisibility(View.GONE); // Hide URL field when drawable auto-selected
        }
    }

    private void loadDrawableResources() {
        drawableResourceNames = new ArrayList<>();
        filteredDrawableResourceNames = new ArrayList<>();

        // Add a "None" option
        drawableResourceNames.add("None");
        filteredDrawableResourceNames.add("None");

        try {
            // Get all drawable resource names
            Field[] fields = R.drawable.class.getFields();
            for (Field field : fields) {
                try {
                    // Skip system resources that start with certain prefixes
                    String name = field.getName();
                    if (!name.startsWith("abc_") && !name.startsWith("notification_") &&
                            !name.startsWith("btn_") && !name.startsWith("ic_")) {

                        // Get the resource ID
                        int resourceId = field.getInt(null);

                        try {
                            // Try to load it as a BitmapDrawable directly
                            Drawable drawable = ContextCompat.getDrawable(this, resourceId);

                            if (drawable instanceof BitmapDrawable) {
                                // It's a bitmap drawable (PNG, JPG, etc.)
                                drawableResourceNames.add(name);
                                filteredDrawableResourceNames.add(name);
                            } else {
                                // It's likely an XML-based drawable
                                Log.d("QuestionsActivity", "Skipping XML drawable: " + name);
                            }
                        } catch (Exception e) {
                            Log.e("QuestionsActivity", "Error checking drawable type: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.e("QuestionsActivity", "Error getting drawable name: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("QuestionsActivity", "Error accessing R.drawable: " + e.getMessage());
            // Add some default drawable names in case reflection fails
            drawableResourceNames.add("hat");
            drawableResourceNames.add("computer");
            drawableResourceNames.add("phone");
            filteredDrawableResourceNames.addAll(drawableResourceNames);
        }

        // Create adapter for spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filteredDrawableResourceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        drawableResourceSpinner.setAdapter(adapter);

        // Set listener to update imgNameEditText when selection changes
        drawableResourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("None")) {
                    imgNameEditText.setText(selected);
                    imgUrlEditText.setVisibility(View.GONE); // Hide URL field when drawable selected
                } else {
                    imgUrlEditText.setVisibility(View.VISIBLE); // Show URL field when None selected
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void openEditDialog(Question question) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_question, null);
        builder.setView(dialogView);

        // Find views in the dialog
        EditText editQuestion = dialogView.findViewById(R.id.edit_question);
        EditText editImgUrl = dialogView.findViewById(R.id.edit_img_url);
        EditText editImgName = dialogView.findViewById(R.id.edit_img_name);
        EditText editDrawableSearch = dialogView.findViewById(R.id.edit_drawable_search);
        Spinner editDrawableSpinner = dialogView.findViewById(R.id.edit_drawable_spinner);
        RadioGroup editChoicesGroup = dialogView.findViewById(R.id.edit_choices_group);
        EditText editNewChoice = dialogView.findViewById(R.id.edit_new_choice);
        Button editAddChoice = dialogView.findViewById(R.id.edit_add_choice);
        EditText editFixedAnswer = dialogView.findViewById(R.id.edit_fixed_answer);
        Spinner editDifficulty = dialogView.findViewById(R.id.edit_difficulty);
        Spinner editLanguage = dialogView.findViewById(R.id.edit_language);
        Spinner editQuestionType = dialogView.findViewById(R.id.edit_question_type);
        Button saveEditButton = dialogView.findViewById(R.id.save_edit_button);

        // Set current values
        editQuestion.setText(question.getQuestion());
        editImgUrl.setText(question.getImgUrl());
        editImgName.setText(question.getImgName());

        // Set initial visibility based on whether imgName has a value
        if (!TextUtils.isEmpty(question.getImgName())) {
            editImgName.setVisibility(View.GONE);
            editImgUrl.setVisibility(View.GONE); // Hide URL field if drawable is selected
        } else {
            editImgUrl.setVisibility(View.VISIBLE); // Show URL field if no drawable is selected
        }

        // Setup spinners
        setupEditSpinners(editDifficulty, editLanguage, editQuestionType, question);

        // Setup drawable spinner with all drawable resources
        List<String> dialogDrawableNames = new ArrayList<>(drawableResourceNames);
        ArrayAdapter<String> drawableAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, dialogDrawableNames);
        drawableAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editDrawableSpinner.setAdapter(drawableAdapter);

        // Set current drawable selection if it exists
        if (!TextUtils.isEmpty(question.getImgName())) {
            int position = dialogDrawableNames.indexOf(question.getImgName());
            if (position >= 0) {
                editDrawableSpinner.setSelection(position);
            }
        }

        // Setup drawable search functionality
        editDrawableSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterDialogDrawableResources(s.toString(), editDrawableSpinner, dialogDrawableNames, editImgName);
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Set drawable spinner listener
        editDrawableSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if (!selected.equals("None")) {
                    editImgName.setText(selected);
                    editImgName.setVisibility(View.GONE);
                    editImgUrl.setVisibility(View.GONE); // Hide URL field when drawable selected
                } else {
                    editImgName.setText("");
                    editImgUrl.setVisibility(View.VISIBLE); // Show URL field when None selected
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Clear choices group
        editChoicesGroup.removeAllViews();

        // Setup UI based on question type
        if (question.getQuestionType().equals("Multiple Choice")) {
            editFixedAnswer.setVisibility(View.GONE);
            editChoicesGroup.setVisibility(View.VISIBLE);
            editAddChoice.setVisibility(View.VISIBLE);
            editNewChoice.setVisibility(View.VISIBLE);

            // Add existing choices
            for (String choice : question.getChoices()) {
                addChoiceToRadioGroup(choice, editChoicesGroup);
            }

            // Check the correct answer
            for (int i = 0; i < editChoicesGroup.getChildCount(); i++) {
                View view = editChoicesGroup.getChildAt(i);
                if (view instanceof LinearLayout) {
                    for (int j = 0; j < ((LinearLayout) view).getChildCount(); j++) {
                        View child = ((LinearLayout) view).getChildAt(j);
                        if (child instanceof RadioButton) {
                            RadioButton rb = (RadioButton) child;
                            if (rb.getText().toString().equals(question.getCorrectAnswer())) {
                                rb.setChecked(true);
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            editChoicesGroup.setVisibility(View.GONE);
            editAddChoice.setVisibility(View.GONE);
            editNewChoice.setVisibility(View.GONE);
            editFixedAnswer.setVisibility(View.VISIBLE);
            editFixedAnswer.setText(question.getCorrectAnswer());
        }

        // Create and show dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Setup question type change listener
        editQuestionType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String type = parent.getItemAtPosition(position).toString();
                updateEditDialogUI(type, editChoicesGroup, editAddChoice, editNewChoice, editFixedAnswer);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup add choice button
        editAddChoice.setOnClickListener(v -> {
            String choice = editNewChoice.getText().toString().trim();
            if (!choice.isEmpty()) {
                addChoiceToRadioGroup(choice, editChoicesGroup);
                editNewChoice.setText("");
            }
        });

        // Setup save button
        saveEditButton.setOnClickListener(v -> {
            // Get updated values
            String updatedQuestion = editQuestion.getText().toString().trim();
            String updatedImgUrl = editImgUrl.getText().toString().trim();
            String updatedImgName = editImgName.getText().toString().trim();
            String updatedType = editQuestionType.getSelectedItem().toString();
            String updatedDifficulty = editDifficulty.getSelectedItem().toString();
            String updatedLanguage = editLanguage.getSelectedItem().toString();
            String updatedAnswer = "";
            List<String> choices = new ArrayList<>();

            // Create updates map
            Map<String, Object> updates = new HashMap<>();
            updates.put("question", updatedQuestion);
            updates.put("difficulty", updatedDifficulty);
            updates.put("language", updatedLanguage);
            updates.put("questionType", updatedType);
            updates.put("imgUrl", updatedImgUrl);
            updates.put("imgName", updatedImgName);

            // Handle different question types
            if (updatedType.equals("Multiple Choice")) {
                boolean hasSelectedAnswer = false;

                // Collect choices and find selected answer
                for (int i = 0; i < editChoicesGroup.getChildCount(); i++) {
                    View view = editChoicesGroup.getChildAt(i);
                    if (view instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout) view;
                        for (int j = 0; j < layout.getChildCount(); j++) {
                            View childView = layout.getChildAt(j);
                            if (childView instanceof RadioButton) {
                                RadioButton rb = (RadioButton) childView;
                                String choiceText = rb.getText().toString();
                                choices.add(choiceText);
                                if (rb.isChecked()) {
                                    updatedAnswer = choiceText;
                                    hasSelectedAnswer = true;
                                }
                            }
                        }
                    }
                }

                // Validate
                if (choices.isEmpty()) {
                    Toast.makeText(QuestionsActivity.this, "Please add at least one choice", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!hasSelectedAnswer) {
                    Toast.makeText(QuestionsActivity.this, "Please select a correct answer", Toast.LENGTH_SHORT).show();
                    return;
                }

                updates.put("choices", choices);
            } else {
                // Handle fixed answer
                updatedAnswer = editFixedAnswer.getText().toString().trim();
                if (updatedAnswer.isEmpty()) {
                    Toast.makeText(QuestionsActivity.this, "Please enter an answer", Toast.LENGTH_SHORT).show();
                    return;
                }
                updates.put("choices", Collections.emptyList());
            }

            updates.put("correctAnswer", updatedAnswer);

            // Save to Firestore
            db.collection("questions").document(question.getId())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(QuestionsActivity.this, "Question updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadQuestions();
                    })
                    .addOnFailureListener(e -> Toast.makeText(QuestionsActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // Updated method to filter drawable resources in the dialog and auto-select the first match
    private void filterDialogDrawableResources(String query, Spinner spinner, List<String> allDrawables, EditText imgNameEditText) {
        List<String> filteredList = new ArrayList<>();
        filteredList.add("None");

        if (TextUtils.isEmpty(query)) {
            // If query is empty, add all drawables except the first one (which is "None")
            if (allDrawables.size() > 1) {
                filteredList.addAll(allDrawables.subList(1, allDrawables.size()));
            }
        } else {
            // Add drawables that match the query
            for (int i = 1; i < allDrawables.size(); i++) {
                if (allDrawables.get(i).toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(allDrawables.get(i));
                }
            }
        }

        // Update spinner with filtered list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, filteredList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Auto-select the first matching item if there are any matches
        if (!TextUtils.isEmpty(query) && filteredList.size() > 1) {
            // Select the first match (index 1 since "None" is at index 0)
            spinner.setSelection(1);
            // Update the imgNameEditText with the selected drawable
            imgNameEditText.setText(filteredList.get(1));
            imgNameEditText.setVisibility(View.VISIBLE);

            // Find and hide the imgUrl EditText
            View dialogView = (View) spinner.getParent();
            EditText editImgUrl = dialogView.findViewById(R.id.edit_img_url);
            if (editImgUrl != null) {
                editImgUrl.setVisibility(View.GONE);
            }
        }

        // Log for debugging
        Log.d("QuestionsActivity", "Filtered drawable resources: " + filteredList.size() + " items");
    }

    private void setupEditSpinners(Spinner difficultySpinner, Spinner languageSpinner, Spinner questionTypeSpinner, Question question) {
        // Setup difficulty spinner
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty_array, android.R.layout.simple_spinner_item);
        difficultyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);
        difficultySpinner.setSelection(difficultyAdapter.getPosition(question.getDifficulty()));

        // Setup language spinner
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages, android.R.layout.simple_spinner_item);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);
        languageSpinner.setSelection(languageAdapter.getPosition(question.getLanguage()));

        // Setup question type spinner
        ArrayAdapter<CharSequence> questionTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.question_types, android.R.layout.simple_spinner_item);
        questionTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        questionTypeSpinner.setAdapter(questionTypeAdapter);
        questionTypeSpinner.setSelection(questionTypeAdapter.getPosition(question.getQuestionType()));
    }

    private void updateEditDialogUI(String type, RadioGroup choicesGroup, Button addChoiceBtn, EditText newChoiceEdit, EditText fixedAnswer) {
        if (type.equals("Multiple Choice")) {
            choicesGroup.setVisibility(View.VISIBLE);
            addChoiceBtn.setVisibility(View.VISIBLE);
            newChoiceEdit.setVisibility(View.VISIBLE);
            fixedAnswer.setVisibility(View.GONE);
        } else {
            choicesGroup.setVisibility(View.GONE);
            addChoiceBtn.setVisibility(View.GONE);
            newChoiceEdit.setVisibility(View.GONE);
            fixedAnswer.setVisibility(View.VISIBLE);
        }
    }

    private void addChoiceToRadioGroup(String choice, RadioGroup group) {
        LinearLayout choiceLayout = new LinearLayout(this);
        choiceLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        choiceLayout.setOrientation(LinearLayout.HORIZONTAL);

        RadioButton rb = new RadioButton(this);
        rb.setText(choice);
        rb.setId(View.generateViewId());
        rb.setPadding(16, 16, 16, 16);
        rb.setTextSize(16);
        rb.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // Add the click listener to handle mutual exclusivity
        rb.setOnClickListener(view -> {
            try {
                // First uncheck all radio buttons in the group
                for (int i = 0; i < group.getChildCount(); i++) {
                    View childView = group.getChildAt(i);
                    if (childView instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout) childView;
                        for (int j = 0; j < layout.getChildCount(); j++) {
                            View innerView = layout.getChildAt(j);
                            if (innerView instanceof RadioButton && innerView != rb) {
                                ((RadioButton) innerView).setChecked(false);
                            }
                        }
                    }
                }

                // Ensure the clicked radio button is checked
                rb.setChecked(true);
            } catch (Exception e) {
                Log.e("QuestionsActivity", "Error in radio button click: " + e.getMessage());
                // Don't crash the app, just show a toast
                Toast.makeText(QuestionsActivity.this, "Error selecting choice", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setBackgroundResource(android.R.color.transparent);
        deleteButton.setPadding(8, 8, 8, 8);

        deleteButton.setOnClickListener(v -> {
            group.removeView(choiceLayout);
            Toast.makeText(this, "Choice removed", Toast.LENGTH_SHORT).show();
        });

        choiceLayout.addView(rb);
        choiceLayout.addView(deleteButton);

        group.addView(choiceLayout);
    }

    private void setupSpinners() {
        // Setup difficulty spinner
        ArrayAdapter<CharSequence> difficultyAdapter = ArrayAdapter.createFromResource(this,
                R.array.difficulty_array, R.layout.spinner_item);
        difficultyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        difficultySpinner.setAdapter(difficultyAdapter);

        // Setup language spinner
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(this,
                R.array.languages, R.layout.spinner_item);
        languageAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        languageSpinner.setAdapter(languageAdapter);

        // Setup question type spinner
        ArrayAdapter<CharSequence> questionTypeAdapter = ArrayAdapter.createFromResource(this,
                R.array.question_types, R.layout.spinner_item);
        questionTypeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        questionTypeSpinner.setAdapter(questionTypeAdapter);
    }

    private void updateUIForQuestionType() {
        String selectedType = questionTypeSpinner.getSelectedItem().toString();

        if (selectedType.equals("Multiple Choice")) {
            multipleChoiceSection.setVisibility(View.VISIBLE);
            fixedAnswerLayout.setVisibility(View.GONE);
        } else {
            multipleChoiceSection.setVisibility(View.GONE);
            fixedAnswerLayout.setVisibility(View.VISIBLE);
        }
    }

    private void saveQuestion() {
        // Get input values
        String question = questionEditText.getText().toString().trim();
        String difficulty = difficultySpinner.getSelectedItem().toString();
        String language = languageSpinner.getSelectedItem().toString();
        String questionType = questionTypeSpinner.getSelectedItem().toString();
        String imgUrl = imgUrlEditText.getText().toString().trim();
        String imgName = imgNameEditText.getText().toString().trim();

        // Validate question
        if (TextUtils.isEmpty(question)) {
            Toast.makeText(this, "Please enter a question", Toast.LENGTH_SHORT).show();
            return;
        }

        correctAnswer = "";

        // Handle different question types
        if (questionType.equals("Multiple Choice")) {
            boolean hasSelectedAnswer = false;

            // Find selected answer
            for (int i = 0; i < choicesRadioGroup.getChildCount(); i++) {
                View view = choicesRadioGroup.getChildAt(i);
                if (view instanceof LinearLayout) {
                    LinearLayout layout = (LinearLayout) view;
                    for (int j = 0; j < layout.getChildCount(); j++) {
                        View childView = layout.getChildAt(j);
                        if (childView instanceof RadioButton) {
                            RadioButton rb = (RadioButton) childView;
                            if (rb.isChecked()) {
                                correctAnswer = rb.getText().toString();
                                hasSelectedAnswer = true;
                                break;
                            }
                        }
                    }
                }
                if (hasSelectedAnswer) break;
            }

            // Validate
            if (!hasSelectedAnswer) {
                Toast.makeText(this, "Please select a correct answer", Toast.LENGTH_SHORT).show();
                return;
            }

            if (choicesList.isEmpty()) {
                Toast.makeText(this, "Please add at least one choice", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            // Handle fixed answer
            correctAnswer = fixedAnswerEditText.getText().toString().trim();
            if (TextUtils.isEmpty(correctAnswer)) {
                Toast.makeText(this, "Please provide an answer", Toast.LENGTH_SHORT).show();
                return;
            }
            choicesList.clear();
        }

        // Create and save question
        Question newQuestion = new Question(question, choicesList, correctAnswer, difficulty, language, questionType, "", imgUrl, imgName);
        db.collection("questions")
                .add(newQuestion)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Question saved successfully", Toast.LENGTH_SHORT).show();
                    clearForm();
                    loadQuestions();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error saving question", Toast.LENGTH_SHORT).show());
    }

    private void clearForm() {
        questionEditText.setText("");
        imgUrlEditText.setText("");
        imgNameEditText.setText("");
        drawableResourceSpinner.setSelection(0); // Reset to "None"
        choicesRadioGroup.removeAllViews();
        choicesList.clear();
        fixedAnswerEditText.setText("");
    }

    private void addChoice() {
        String newChoice = newChoiceEditText.getText().toString().trim();
        if (TextUtils.isEmpty(newChoice)) {
            Toast.makeText(this, "Please enter a choice", Toast.LENGTH_SHORT).show();
            return;
        }

        choicesList.add(newChoice);

        LinearLayout choiceLayout = new LinearLayout(this);
        choiceLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        choiceLayout.setOrientation(LinearLayout.HORIZONTAL);

        RadioButton radioButton = new RadioButton(this);
        radioButton.setText(newChoice);
        radioButton.setId(View.generateViewId());
        radioButton.setPadding(16, 16, 16, 16);
        radioButton.setTextSize(16);
        radioButton.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        // Add click listener to ensure only one can be selected
        radioButton.setOnClickListener(v -> {
            try {
                // Uncheck all other radio buttons in the group
                for (int i = 0; i < choicesRadioGroup.getChildCount(); i++) {
                    View childView = choicesRadioGroup.getChildAt(i);
                    if (childView instanceof LinearLayout) {
                        LinearLayout layout = (LinearLayout) childView;
                        for (int j = 0; j < layout.getChildCount(); j++) {
                            View innerView = layout.getChildAt(j);
                            if (innerView instanceof RadioButton && innerView != radioButton) {
                                ((RadioButton) innerView).setChecked(false);
                            }
                        }
                    }
                }
                // Ensure this radio button is checked
                radioButton.setChecked(true);
            } catch (Exception e) {
                Log.e("QuestionsActivity", "Error in radio button click: " + e.getMessage());
                Toast.makeText(this, "Error selecting choice", Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton deleteButton = new ImageButton(this);
        deleteButton.setImageResource(android.R.drawable.ic_menu_delete);
        deleteButton.setBackgroundResource(android.R.color.transparent);
        deleteButton.setPadding(8, 8, 8, 8);

        deleteButton.setOnClickListener(v -> {
            choicesList.remove(newChoice);
            choicesRadioGroup.removeView(choiceLayout);
            Toast.makeText(this, "Choice removed", Toast.LENGTH_SHORT).show();
        });

        choiceLayout.addView(radioButton);
        choiceLayout.addView(deleteButton);

        choicesRadioGroup.addView(choiceLayout);
        newChoiceEditText.setText("");
    }

    private void loadQuestions() {
        String selectedDifficulty = difficultySpinner.getSelectedItem().toString();
        String selectedLanguage = languageSpinner.getSelectedItem().toString();

        db.collection("questions")
                .whereEqualTo("difficulty", selectedDifficulty)
                .whereEqualTo("language", selectedLanguage)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        questionsList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Question question = document.toObject(Question.class);
                            question.setId(document.getId());
                            questionsList.add(question);
                        }
                        questionsAdapter.notifyDataSetChanged();
                    }
                });
    }

    private void deleteQuestion(String questionId) {
        db.collection("questions").document(questionId).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Question deleted", Toast.LENGTH_SHORT).show();
                    loadQuestions();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to delete question", Toast.LENGTH_SHORT).show());
    }
}

