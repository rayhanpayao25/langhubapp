package com.IT4A.langhub;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TranslateHelper {

    private static final String TAG = "TranslateHelper";

    public interface TranslateCallback {
        void onTranslateSuccess(String translatedText);
        void onTranslateFailure(String error);
    }

    public static void translateText(String textToTranslate, String targetLanguage, TranslateCallback callback, Context context) {
        new TranslateAsyncTask(textToTranslate, targetLanguage, callback).execute();
    }

    private static class TranslateAsyncTask extends AsyncTask<Void, Void, String> {
        private String textToTranslate;
        private String targetLanguage;
        private TranslateCallback callback;
        private String error;

        public TranslateAsyncTask(String textToTranslate, String targetLanguage, TranslateCallback callback) {
            this.textToTranslate = textToTranslate;
            this.targetLanguage = targetLanguage;
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                // URL encode the text
                String encodedText = URLEncoder.encode(textToTranslate, "UTF-8");

                // Create the URL for the translation API
                // Note: This is using a free translation API. For production, you should use a proper API with authentication
                String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl="
                        + targetLanguage + "&dt=t&q=" + encodedText;

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONArray jsonArray = new JSONArray(response.toString());
                JSONArray translationArray = jsonArray.getJSONArray(0);

                StringBuilder translatedText = new StringBuilder();
                for (int i = 0; i < translationArray.length(); i++) {
                    JSONArray translationLineArray = translationArray.getJSONArray(i);
                    String translationLine = translationLineArray.getString(0);
                    translatedText.append(translationLine);
                }

                return translatedText.toString();

            } catch (UnsupportedEncodingException e) {
                error = "Encoding error: " + e.getMessage();
                Log.e(TAG, error, e);
            } catch (IOException e) {
                error = "Network error: " + e.getMessage();
                Log.e(TAG, error, e);
            } catch (JSONException e) {
                error = "JSON parsing error: " + e.getMessage();
                Log.e(TAG, error, e);
            } catch (Exception e) {
                error = "Translation error: " + e.getMessage();
                Log.e(TAG, error, e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(String translatedText) {
            if (translatedText != null) {
                callback.onTranslateSuccess(translatedText);
            } else {
                callback.onTranslateFailure(error != null ? error : "Translation failed");
            }
        }
    }
}