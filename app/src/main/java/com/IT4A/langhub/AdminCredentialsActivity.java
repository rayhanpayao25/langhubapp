package com.IT4A.langhub;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AdminCredentialsActivity extends AppCompatActivity {

    private EditText adminUsername, newAdminPassword;
    private Button saveButton;
    private FirebaseFirestore db;
    private String adminDocId = "EiooaCD9xymlSI1qCB8X"; // Document ID for admin credentials

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.admin_credentials_activity);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        adminUsername = findViewById(R.id.adminUsername);
        newAdminPassword = findViewById(R.id.newAdminPassword);
        saveButton = findViewById(R.id.saveButton);

        // Set initial values (fetch current admin credentials from Firestore)
        fetchAdminCredentials();

        saveButton.setOnClickListener(v -> updateAdminCredentials());
    }

    // Fetch current admin credentials from Firestore
    private void fetchAdminCredentials() {
        db.collection("admin").document(adminDocId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        String username = task.getResult().getString("username");
                        adminUsername.setText(username);
                    } else {
                        Toast.makeText(this, "Failed to load admin credentials", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Update admin credentials in Firestore (without requiring old password)
    private void updateAdminCredentials() {
        String enteredUsername = adminUsername.getText().toString().trim();
        String enteredNewPassword = newAdminPassword.getText().toString().trim();

        if (enteredUsername.isEmpty() || enteredNewPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hash the new password
        String hashedNewPassword = hashPassword(enteredNewPassword);

        // Update Firestore with the new password
        db.collection("admin").document(adminDocId)
                .update("username", enteredUsername, "password", hashedNewPassword)
                .addOnCompleteListener(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        Toast.makeText(this, "Admin credentials updated successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update credentials", Toast.LENGTH_SHORT).show();
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
            throw new RuntimeException("Error hashing password", e);
        }
    }
}