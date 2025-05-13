package com.IT4A.langhub;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view (if needed)
        setContentView(R.layout.activity_main);

        findViewById(android.R.id.content).setBackgroundResource(R.drawable.bgphone);

        // Start HomePageActivity directly
        Intent intent = new Intent(MainActivity.this, HomepageActivity.class);
        startActivity(intent);
        finish(); // Optional: Finish MainActivity so the user cannot go back to it
    }
}
