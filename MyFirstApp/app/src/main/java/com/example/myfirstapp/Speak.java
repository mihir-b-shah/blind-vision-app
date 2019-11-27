package com.example.myfirstapp;

/**
 * Speaks some given text.
 */

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import java.util.Locale;

public class Speak extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private String message;

    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak);

        tts = new TextToSpeech(this, this);
        tts.setSpeechRate((float) 0.9);
        tts.setPitch((float) 1.1);

        String message = getIntent().getStringExtra(MainActivity.EXTRA_MESSAGE);
        this.message = message;

    }

    /**
     * Initializes the text to speech service.
     *
     * @param i code for intialization of the service.
     */
    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.ENGLISH);
            speak(message);
        }
    }

    /**
     * Speaks the text given.
     * @param message the message to be spoken.
     */
    public void speak(String message) {

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "I'll bet i can fit my whole foot in your mouth!");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onError(String s) {}
            @Override public void onDone(String s) {
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tts.shutdown();
    }
}
