package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AdminActivity extends AppCompatActivity {

    private TextInputEditText adminUsername, adminPassword;
    private MaterialButton loginButton;
    private TextView forgotPasswordText;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        adminUsername = findViewById(R.id.adminUsername);
        adminPassword = findViewById(R.id.adminPassword);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // Set up login button click listener
        loginButton.setOnClickListener(view -> authenticateAdmin());

        // Set up forgot password click listener
        forgotPasswordText.setOnClickListener(view -> {
            Toast.makeText(this, "Please contact Langhub Admin", Toast.LENGTH_SHORT).show();
        });
    }

    private void authenticateAdmin() {
        String enteredUsername = adminUsername.getText().toString().trim();
        String enteredPassword = adminPassword.getText().toString().trim();

        if (enteredUsername.isEmpty() || enteredPassword.isEmpty()) {
            Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        loginButton.setEnabled(false);
        loginButton.setText("Signing in...");

        // Hash the entered password
        String hashedEnteredPassword = hashPassword(enteredPassword);

        db.collection("admin").document("EiooaCD9xymlSI1qCB8X")
                .get()
                .addOnCompleteListener(task -> {
                    // Reset button state
                    loginButton.setEnabled(true);
                    loginButton.setText("Sign In");

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String correctPassword = document.getString("password");
                            if (hashedEnteredPassword.equals(correctPassword)) {
                                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(AdminActivity.this, AdminPanelActivity.class));
                                finish(); // Close login screen
                            } else {
                                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show();
                                adminPassword.setText("");
                            }
                        } else {
                            Toast.makeText(this, "Admin not found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("Firestore", "Error getting admin data", task.getException());
                        Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Hashing the password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password" , e);
        }
    }
}

