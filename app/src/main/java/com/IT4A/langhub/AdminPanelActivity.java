package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

public class AdminPanelActivity extends AppCompatActivity {

    private MaterialButton questionsButton, manageCredentialsButton;
    private TextView adminNameText, adminRoleText;
    private Toolbar topAppBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);

        // Set up the toolbar
        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        // Initialize UI components
        questionsButton = findViewById(R.id.questionsButton);
        manageCredentialsButton = findViewById(R.id.manageCredentialsButton);
        adminNameText = findViewById(R.id.adminNameText);
        adminRoleText = findViewById(R.id.adminRoleText);

        // You can set admin name and role dynamically if needed
        // adminNameText.setText("John Doe");
        // adminRoleText.setText("Content Administrator");

        // Logout Button: Clears session and redirects to login screen
        // Questions Button: Redirects to the Questions Activity
        questionsButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, com.IT4A.langhub.QuestionsActivity.class));
        });

        // Manage Credentials Button: Redirects to the Admin Credentials Activity
        manageCredentialsButton.setOnClickListener(v -> {
            startActivity(new Intent(AdminPanelActivity.this, AdminCredentialsActivity.class));
        });
    }

    @Override
    public void onBackPressed() {
        // Show Toast message when the user presses the back button
        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();
        super.onBackPressed();  // Call the default back pressed behavior
    }
}
