package com.IT4A.langhub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;

import java.util.ArrayList;
import java.util.Locale;

public class OcrActivity extends AppCompatActivity {

    private Spinner optionsSpinner;
    private Button captureButton;
    private Button translateButton;
    private TextView recognizedText;
    private SpeechRecognizer speechRecognizer;
    private TextView translatedText;
    private Uri imageUri;
    private TextToSpeech textToSpeech;
    private String extractedText = "";

    private CropImageView cropImageView; // CropImageView for cropping the image

    private ArrayList<String> optionsList;

    // Registering the ActivityResultLauncher for crop image result using CropImageContract
    private final ActivityResultLauncher<CropImageContractOptions> cropImageLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result.isSuccessful()) {
                    // Get the cropped image URI
                    Uri croppedUri = result.getUriContent();
                    cropImageView.setImageUriAsync(croppedUri); // Set the cropped image to the CropImageView

                    // Process the cropped image for OCR
                    processImage(croppedUri);
                } else {
                    Exception error = result.getError();
                    Toast.makeText(this, "Cropping failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

        // Initialize views
        captureButton = findViewById(R.id.captureButton);
        recognizedText = findViewById(R.id.recognizedText);
        translatedText = findViewById(R.id.translatedText);
        translateButton = findViewById(R.id.translateButton);
        optionsSpinner = findViewById(R.id.optionsSpinner);
        cropImageView = findViewById(R.id.cropImageView);

        // Set up Toolbar with back button
        Toolbar appBar = findViewById(R.id.appBar);
        // Ensure this matches your XML ID
        setSupportActionBar(appBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setTitle("OCR"); // Set title
        }

        // Handle back button press
        appBar.setNavigationOnClickListener(view -> onBackPressed());

        // Set up options for the Spinner
        optionsList = new ArrayList<>();
        optionsList.add("Translate to");
        optionsList.add("English");
        optionsList.add("Chinese");
        optionsList.add("Spanish");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, optionsList);
        optionsSpinner.setAdapter(adapter);

        // Capture button for taking a picture
        captureButton.setOnClickListener(view -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            // Create content values object to store image
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "Captured Image");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, 100); // Launch camera capture
            }
        });

        // Handle reset button click


        View resetButton = findViewById(R.id.resetButton);

        resetButton.setOnClickListener(view -> {
            // Log the reset action
            Log.d("OCR", "Reset button clicked");

            // Reset recognized text
            recognizedText.setText("Recognized Text will appear here");

            // Reset translated text
            translatedText.setText("Translation will appear here");



            // Reset the Spinner to default value
            optionsSpinner.setSelection(0);  // Assuming "Translate to" is at position 0

            // Reset the CropImageView
            cropImageView.setImageUriAsync(null);  // Clears the cropped image
        });




        // Translate button functionality
        translateButton.setOnClickListener(view -> {
            String selectedLanguage = optionsSpinner.getSelectedItem().toString();
            String textToTranslate = recognizedText.getText().toString();

            if (textToTranslate.isEmpty()) {
                Toast.makeText(OcrActivity.this, "Please capture text first!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedLanguage.equals("Translate to")) {
                Toast.makeText(OcrActivity.this, "Please select a language", Toast.LENGTH_SHORT).show();
                return;
            }

            String targetLanguage = "en"; // Default is English
            if (selectedLanguage.equals("Chinese")) {
                targetLanguage = "zh";
            } else if (selectedLanguage.equals("Spanish")) {
                targetLanguage = "es";
            }

            TranslateHelper.translateText(textToTranslate, targetLanguage, new TranslateHelper.TranslateCallback() {
                @Override
                public void onTranslateSuccess(String translatedTextResult) {
                    translatedText.setText(translatedTextResult); // Display translated text
                }

                @Override
                public void onTranslateFailure(String error) {
                    Toast.makeText(OcrActivity.this, "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            }, OcrActivity.this);
        });

        ImageView speakerIcon = findViewById(R.id.speakerIcon);
        speakerIcon.setOnClickListener(view -> {
            String textToRead = translatedText.getText().toString();

            if (!textToRead.isEmpty()) {
                // Get the selected language from optionsSpinner
                String selectedOption = optionsSpinner.getSelectedItem().toString();

                // Set language for TTS based on selected language
                if (selectedOption.equals("Chinese")) {
                    int langResult = textToSpeech.setLanguage(Locale.CHINA);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(OcrActivity.this, "Chinese language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (selectedOption.equals("Spanish")) {
                    int langResult = textToSpeech.setLanguage(new Locale("es", "ES"));
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(OcrActivity.this, "Spanish language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else if (selectedOption.equals("English")) {
                    int langResult = textToSpeech.setLanguage(Locale.US);
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(OcrActivity.this, "English language not supported", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    Toast.makeText(OcrActivity.this, "Please select a valid language", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Read the translated text aloud
                textToSpeech.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                Toast.makeText(OcrActivity.this, "No text to speak", Toast.LENGTH_SHORT).show();
            }
        });


        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.SUCCESS) {
                    Toast.makeText(OcrActivity.this, "TTS initialization failed", Toast.LENGTH_SHORT).show();
                    speakerIcon.setEnabled(false);
                }

            }

        });

        ImageView copyIcon = findViewById(R.id.copyIcon); // Assuming you've added this ImageView to your layout
        copyIcon.setOnClickListener(view -> {
            String textToCopy = translatedText.getText().toString();

            if (!textToCopy.isEmpty()) {
                // Copy text to the clipboard
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Translated Text", textToCopy);
                clipboard.setPrimaryClip(clip);

                // Provide feedback to the user
                Toast.makeText(OcrActivity.this, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(OcrActivity.this, "No text to copy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getChineseCharactersOnly(String text) {
        StringBuilder chineseText = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (Character.toString(character).matches("[\\u4e00-\\u9fa5]+")) {
                chineseText.append(character);
            }
        }
        return chineseText.toString();
    }



    // Handling image capture result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            if (imageUri != null) {
                // Create a CropImageOptions object (you can customize the options as needed)
                CropImageOptions options = new CropImageOptions();
                // You can set any additional options, e.g. aspect ratio, guidelines, etc.

                // Launch cropping activity using the launcher
                CropImageContractOptions cropOptions = new CropImageContractOptions(imageUri, options);
                cropImageLauncher.launch(cropOptions); // Launch cropping activity using the launcher
            } else {
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Process the captured or cropped image for OCR
    private void processImage(Uri imageUri) {
        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);

            // Use TextRecognizer with Chinese or other language options
            TextRecognizer recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            // Add language-specific options (optional, based on the selected language)
            // For example, if Chinese is selected:
            if (optionsSpinner.getSelectedItem().toString().equals("Chinese")) {
                recognizer = TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
            }

            recognizer.process(image)
                    .addOnSuccessListener(text -> {
                        extractedText = text.getText();
                        recognizedText.setText(extractedText); // Display recognized text
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "OCR failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to process image.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

}
