package com.IT4A.langhub;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import android.util.Base64;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import java.util.List;


public class HomepageActivity extends AppCompatActivity {

    private String currentUsername = null;
    private RecyclerView recyclerView;
    private EditText searchBox;
    private LinearLayout rectangleBox;
    private View whiteSquareBox;
    private GridAdapter adapter;
    private boolean isKeyboardVisible = false;
    private TextView helloText;
    private ImageView circleButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);
        
        helloText = findViewById(R.id.textHello); // "Hello" TextView
        circleButton = findViewById(R.id.circle_button); // Profile button

        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Load saved username
        String userName = sharedPreferences.getString("userName", "User");
        helloText.setText("Hello, " + userName + " 👋");

        // Load saved profile image (if available)
        String savedImage = sharedPreferences.getString("profileImage", null);
        if (savedImage != null) {
            byte[] imageBytes = Base64.decode(savedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            circleButton.setImageBitmap(getCircularBitmap(bitmap));
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.gridRecyclerView);
        searchBox = findViewById(R.id.searchBox);
        rectangleBox = findViewById(R.id.rectangleBox);
        whiteSquareBox = findViewById(R.id.white_square_box);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // 2 columns in the grid

        adapter = new GridAdapter();
        recyclerView.setAdapter(adapter);

        recyclerView.setVisibility(View.VISIBLE);

        // Search button functionality
        ImageView searchButton = findViewById(R.id.search_button);
        searchButton.setOnClickListener(v -> {
            // Proceed to SearchPage Activity directly
            Intent intent = new Intent(HomepageActivity.this, com.IT4A.langhub.SearchPage.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });

        // Help button functionality
        ImageView helpButton = findViewById(R.id.help_button);
        helpButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomepageActivity.this, HelpActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView favoritesButton = findViewById(R.id.favorite_button);
        favoritesButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomepageActivity.this, FavoritesActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        ImageView circleButton = findViewById(R.id.circle_button);
        circleButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomepageActivity.this, UserProfileActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0); // No animation
        });

        ImageView userButton = findViewById(R.id.user_button);
        userButton.setOnClickListener(view -> {
            Intent intent = new Intent(HomepageActivity.this, userActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);  // Disable transition animation
        });

        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Trigger the filter method of the adapter whenever the text changes
                adapter.filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

    }

    @Override
    public void onBackPressed() {
        // Create an intent to go to the phone's home screen
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(homeIntent);  // Start the home activity

        super.onBackPressed();
    }

    public Bitmap getCircularBitmap(Bitmap bitmap) {
        // Create a circular bitmap from the original image
        int width = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int height = width;
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    
        // Create a canvas to draw the circular image
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
    
        // Draw the circle
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint);
    
        // Draw the bitmap inside the circle
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
    
        return output;
    }
    

    // Adapter for the RecyclerView grid
    public class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridViewHolder> {
        private final String[] titles = {
                "Translate via Text", "Translate via Voice", "OCR",
                "Games", "Greetings", "Conversation", "Numbers", "Time and Date" , "Directions and Places" ,
                "Eating Out" , "Shopping" , "Color and Prints" ,

        };

        private final int[] images = {
                R.drawable.tvt,
                R.drawable.speech,
                R.drawable.scan_text,
                R.drawable.games,
                R.drawable.greetings,
                R.drawable.conversation,
                R.drawable.numbers,
                R.drawable.date,
                R.drawable.dap,
                R.drawable.eatingout,
                R.drawable.shopping,
                R.drawable.color,
                R.drawable.take_quiz,

        };

        private String[] filteredTitles = titles;

        @Override
        public GridViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(R.layout.grid_item, parent, false);
            return new GridViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(GridViewHolder holder, int position) {
            holder.title.setText(filteredTitles[position]);
            holder.image.setImageResource(filteredImages[position]);  // Use filteredImages instead of default images


            holder.itemView.setOnClickListener(v -> {
                switch (filteredTitles[position]) {
                    case "OCR":
                        startActivity(new Intent(HomepageActivity.this, OcrActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Translate via Text":
                        startActivity(new Intent(HomepageActivity.this, TranslateviatextActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Translate via Voice":
                        startActivity(new Intent(HomepageActivity.this, TranslateviavoiceActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Games":
                        startActivity(new Intent(HomepageActivity.this, GamesActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Greetings":
                        startActivity(new Intent(HomepageActivity.this, GreetingsActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Conversation":
                        startActivity(new Intent(HomepageActivity.this, ConversationActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Numbers":
                        startActivity(new Intent(HomepageActivity.this, NumbersActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Time and Date":
                        startActivity(new Intent(HomepageActivity.this, TimeanddateActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Directions and Places":
                        startActivity(new Intent(HomepageActivity.this, DirectionsandPlaces.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Eating Out":
                        startActivity(new Intent(HomepageActivity.this, EatingoutActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Shopping":
                        startActivity(new Intent(HomepageActivity.this, ShoppingActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;
                    case "Color and Prints":
                        startActivity(new Intent(HomepageActivity.this, ColorandPaintsActivity.class));
                        overridePendingTransition(0, 0);  // Disable transition animation
                        break;

                }
            });
        }

        @Override
        public int getItemCount() {
            return filteredTitles.length;
        }

        public class GridViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            ImageView image;

            public GridViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.grid_item_title);
                image = itemView.findViewById(R.id.grid_item_image);
            }
        }
        private int[] filteredImages = images;

        public void filter(String query) {
            if (query.isEmpty()) {
                // No filter, show all titles and images
                filteredTitles = titles;
                filteredImages = images;
            } else {
                List<String> filteredTitlesList = new ArrayList<>();
                List<Integer> filteredImagesList = new ArrayList<>();

                // Loop through each title and check if it matches the query
                for (int i = 0; i < titles.length; i++) {
                    if (titles[i].toLowerCase().contains(query.toLowerCase())) {
                        filteredTitlesList.add(titles[i]);
                        filteredImagesList.add(images[i]);
                    }
                }

                // Specifically check for OCR and Games
                if (query.toLowerCase().contains("ocr")) {
                    filteredTitlesList.clear();
                    filteredImagesList.clear();
                    filteredTitlesList.add("OCR");
                    filteredImagesList.add(R.drawable.scan_text);  // Set the image for OCR
                } else if (query.toLowerCase().contains("games")) {
                    filteredTitlesList.clear();
                    filteredImagesList.clear();
                    filteredTitlesList.add("Games");
                    filteredImagesList.add(R.drawable.games);  // Set the image for Games
                }

                // Convert the filtered lists to arrays
                filteredTitles = filteredTitlesList.toArray(new String[0]);
                filteredImages = new int[filteredImagesList.size()];
                for (int i = 0; i < filteredImagesList.size(); i++) {
                    filteredImages[i] = filteredImagesList.get(i);
                }
            }

            // Notify the adapter to update the grid with new titles and images
            notifyDataSetChanged();
        }





        private int[] filterImages(String query, String[] filteredTitles) {
            List<Integer> filteredImagesList = new ArrayList<>();
            for (String title : filteredTitles) {
                for (int i = 0; i < titles.length; i++) {
                    if (titles[i].equals(title)) {
                        filteredImagesList.add(images[i]);
                        break; // Prevent duplicate images in the list
                    }
                }
            }
            int[] filteredImagesArray = new int[filteredImagesList.size()];
            for (int i = 0; i < filteredImagesList.size(); i++) {
                filteredImagesArray[i] = filteredImagesList.get(i);
            }
            return filteredImagesArray;
        }

        private int[] filterImages(String query) {
            List<Integer> filteredImagesList = new ArrayList<>();
            for (int i = 0; i < titles.length; i++) {
                if (titles[i].toLowerCase().contains(query.toLowerCase())) {
                    filteredImagesList.add(images[i]);
                }
            }
            int[] filteredImagesArray = new int[filteredImagesList.size()];
            for (int i = 0; i < filteredImagesList.size(); i++) {
                filteredImagesArray[i] = filteredImagesList.get(i);
            }
            return filteredImagesArray;
        }

        private String[] filterTitles(String query) {
            List<String> filteredList = new ArrayList<>();
            if (query.equalsIgnoreCase("ocr")) {
                filteredList.add("OCR"); // Only show OCR if the search is 'OCR'
            } else if (query.equalsIgnoreCase("games")) {
                filteredList.add("Games"); // Only show Games if the search is 'Games'
            } else {
                // Default behavior: show all titles that contain the search query
                for (String title : titles) {
                    if (title.toLowerCase().contains(query.toLowerCase())) {
                        filteredList.add(title);
                    }
                }
            }
            Log.d("Search", "Filtered List: " + filteredList.toString());  // Debugging line
            return filteredList.toArray(new String[0]);
        }

        // Update grid visibility based on the filtered result
        private void updateGridVisibility() {
            if (filteredTitles.length == 1 && filteredTitles[0].equalsIgnoreCase("OCR")) {
                // If only OCR is left in the filtered titles, show it and hide others
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                // Show all grid items
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

}
