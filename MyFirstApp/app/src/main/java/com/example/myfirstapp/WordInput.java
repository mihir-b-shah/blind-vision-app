package com.example.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;

import static com.example.myfirstapp.MainActivity.EXTRA_MESSAGE;

/*
This activity should:
1. Read in the user's input through audio.
2. Utilizes dialogflow to create a more conversational interaction. <later>...
 */

public class WordInput extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_input);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(EXTRA_MESSAGE, getIntent().getStringExtra("question"));
        startActivityForResult(start, 0);
    }

    /**
     * List of codes:
     *
     * 0: intro speak
     * 1: speech input
     *
     * @param requestCode
     * @param resultCode
     * @param intent
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case 0:
                    Intent in = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(in, 1);
                    break;
                case 1:
                    Intent out = new Intent();
                    out.putExtra("spk-text", intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0));
                    setResult(Activity.RESULT_OK, out);
                    finish();
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }
}