package com.IT4A.langhub;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class TranslationResponse {

    @SerializedName("data")
    private TranslationData data;

    public TranslationData getData() {
        return data;
    }

    public static class TranslationData {
        @SerializedName("translations")
        private List<Translation> translations;

        public List<Translation> getTranslations() {
            return translations;
        }
    }

    public static class Translation {
        @SerializedName("translatedText")
        private String translatedText;

        public String getTranslatedText() {
            return translatedText;
        }
    }
}
