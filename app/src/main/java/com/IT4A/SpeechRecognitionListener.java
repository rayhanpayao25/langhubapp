package com.IT4A.langhub;

import android.speech.RecognitionListener;
import android.os.Bundle;

public class SpeechRecognitionListener implements RecognitionListener {

    @Override
    public void onReadyForSpeech(Bundle params) {
        // Implement logic here
    }

    @Override
    public void onBeginningOfSpeech() {
        // Implement logic here
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        // Implement logic here
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        // Implement logic here
    }

    @Override
    public void onEndOfSpeech() {
        // Implement logic here
    }

    @Override
    public void onError(int error) {
        // Implement logic here
    }

    @Override
    public void onResults(Bundle results) {
        // Implement logic here
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        // Implement logic here
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        // Implement logic here
    }
}
