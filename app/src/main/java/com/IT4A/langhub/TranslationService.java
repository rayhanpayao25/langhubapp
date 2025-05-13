package com.IT4A.langhub;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class TranslationService {

    private static final String API_URL = "https://translation.googleapis.com/language/translate/v2";
    private static final String API_KEY = "AIzaSyD2f7nNg0At5w6PYwjJgi0qybEboFr26EQ"; // Your API key here

    public static String translate(String text, String targetLanguage) {
        OkHttpClient client = new OkHttpClient();

        // Construct the API request URL with parameters
        String url = String.format("%s?key=%s&q=%s&target=%s", API_URL, API_KEY, text, targetLanguage);

        // Make the request
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();
                // Parse the JSON response to extract the translated text
                return parseTranslation(responseBody);
            } else {
                return "Error: Unable to translate";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Error: Network problem";
        }
    }

    private static String parseTranslation(String jsonResponse) {
        // Example of parsing the JSON response using simple substring manipulation
        // You may want to use a JSON parsing library like Gson or Moshi for better accuracy.
        int start = jsonResponse.indexOf("\"translatedText\":\"") + 18;
        int end = jsonResponse.indexOf("\"}", start);
        return jsonResponse.substring(start, end);
    }
}
