package com.IT4A.langhub;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class userActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        ImageView searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            // Proceed to SearchPage Activity directly
            Intent intent = new Intent(userActivity.this, com.IT4A.langhub.SearchPage.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });

        // Help button functionality
        ImageView helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(view -> {
            Intent intent = new Intent(userActivity.this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView favoritesButton = findViewById(R.id.favorite_button);
        favoritesButton.setOnClickListener(view -> {
            Intent intent = new Intent(userActivity.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView homeButton = findViewById(R.id.home_button);
        homeButton.setOnClickListener(view -> {
            Intent intent = new Intent(userActivity.this, HomepageActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Add this in your onCreate method
        ImageView toolbarUserIcon = findViewById(R.id.admin);
        toolbarUserIcon.setOnClickListener(view -> {
            // Handle user icon click, perhaps navigate to user profile
            Intent intent = new Intent(userActivity.this, AdminActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Contact Us functionality
        Button callButton = findViewById(R.id.call_button);
        callButton.setOnClickListener(view -> {
            // Make a phone call
            String phoneNumber = "+639123456789"; // Replace with your actual phone number
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        });

        Button emailButton = findViewById(R.id.email_button);
        emailButton.setOnClickListener(view -> {
            // Send an email
            String email = "langhub@example.com"; // Replace with your actual email
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry from LangHub App");

            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(userActivity.this, "No email client installed", Toast.LENGTH_SHORT).show();
            }
        });


    }
    @Override
    public void onBackPressed() {
        // Navigate to device home screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);
    }
}

