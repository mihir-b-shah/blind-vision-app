package com.apps.navai;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import androidx.appcompat.app.AppCompatActivity;

import static com.apps.navai.MainActivity.STRING_1;

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

        Intent in = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        in.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        startActivityForResult(in, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(resultCode == Activity.RESULT_OK) {
            switch(requestCode) {
                case 0:
                    Intent out = new Intent();
                    out.putExtra(STRING_1, intent.getStringArrayListExtra(
                            RecognizerIntent.EXTRA_RESULTS).get(0));
                    setResult(Activity.RESULT_OK, out);
                    finish();
                    break;
                default:
                    System.err.println("Code not recognized.");
            }
        }
    }
}