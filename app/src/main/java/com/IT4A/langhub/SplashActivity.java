package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the layout for the splash screen


        // Set the background image immediately


        // Delay transition to HomePageActivity
        new Handler().postDelayed(() -> {
            // Start HomePageActivity after the delay
            Intent intent = new Intent(SplashActivity.this, HomepageActivity.class);
            startActivity(intent);
            finish(); // End SplashActivity so the user can't go back to it
        }, 50); // 2000 ms = 2 seconds delay
    }
}
