package com.IT4A.langhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UserProfileActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private EditText userNameEditText;
    private ImageView profileImageView;
    private Button saveButton;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        userNameEditText = findViewById(R.id.user_name);
        profileImageView = findViewById(R.id.profile_image);
        saveButton = findViewById(R.id.save_button);

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        gson = new Gson();

        // Load saved username
        String savedName = sharedPreferences.getString("userName", "");
        userNameEditText.setText(savedName);

        // Load saved profile image
        String savedImage = sharedPreferences.getString("profileImage", null);
        if (savedImage != null) {
            byte[] imageBytes = android.util.Base64.decode(savedImage, android.util.Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            profileImageView.setImageBitmap(bitmap);
        }

        // Let user pick a new profile picture
        profileImageView.setOnClickListener(v -> openGallery());

        // Save button: store username and image
        saveButton.setOnClickListener(v -> {
            saveUserData();
            restartApp();
        });

        // Save username on text change
        userNameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Update username in SharedPreferences as it changes
                sharedPreferences.edit().putString("userName", s.toString()).apply();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                profileImageView.setImageBitmap(bitmap);
                saveImage(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImage(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        String encodedImage = android.util.Base64.encodeToString(byteArrayOutputStream.toByteArray(), android.util.Base64.DEFAULT);

        // Replace the old profile image in SharedPreferences with the new one
        sharedPreferences.edit().putString("profileImage", encodedImage).apply();
    }

    private void saveUserData() {
        String userName = userNameEditText.getText().toString();

        // Replace the old username in SharedPreferences with the new one
        sharedPreferences.edit().putString("userName", userName).apply();

        // If a new profile image is selected, save it
        if (profileImageView.getDrawable() != null) {
            Bitmap bitmap = ((BitmapDrawable) profileImageView.getDrawable()).getBitmap();
            saveImage(bitmap);
        }
    }

    private void restartApp() {
        // Restart the current activity to see the changes
        Intent intent = getIntent();
        finish();
        startActivity(intent);

        // Show a Toast message indicating the changes are saved
        Toast.makeText(this, "Profile updated! Please restart the app to see changes.", Toast.LENGTH_SHORT).show();
    }

    // Optional: You can clear all preferences if needed
    private void clearAllPreferences() {
        sharedPreferences.edit().clear().apply();
    }
}
