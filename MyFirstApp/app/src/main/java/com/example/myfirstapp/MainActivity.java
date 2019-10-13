/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.Vertex;

import java.util.List;

/*
1. Should manage the main sequence UI.
 */

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private Annotation[] annotations;
    private String spkText;
    private String adjectives;
    private String mCurrentPhotoPath;
    private boolean first = true;

    /**
     * Creates the application and launches the audio intro.
     *
     * @param st the data needed to construct the application.
     */
    @Override
    protected void onCreate(Bundle st) {
        super.onCreate(st);
        setContentView(R.layout.activity_main);
        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(EXTRA_MESSAGE, "Hello welcome to my assisted navigation app.");
        mCurrentPhotoPath = st == null ? null : st.getString("photo-path");
        spkText = st == null ? null : st.getString("spk-text");
        first = st == null;
        if(first) startActivityForResult(start, 0);
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("photo-path", mCurrentPhotoPath);
        b.putString("spk-text", spkText);
    }

    /**
     * Manages the data flow of the activities being launched.
     * <p>
     * List of current request codes:
     * <p>
     * 0: SPEAK_START
     * 1: SPEECH_RECOGNIZER
     *
     * @param requestCode the code used by the startActivityForResult()
     * @param resultCode  the code determining whether the activity executed correctly.
     * @param data        the output of the activity.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 0:
                    Intent start = new Intent(getApplicationContext(), WordInput.class);
                    start.putExtra("question", "What are you looking for?");
                    first = false;
                    startActivityForResult(start, 1);
                    break;
                case 1:
                    spkText = data.getStringExtra("spk-text");
                    Intent photo = new Intent(getApplicationContext(), Photo.class);
                    startActivityForResult(photo, 2);
                    break;
                case 2:
                    mCurrentPhotoPath = data.getStringExtra("photo-path");
                    Intent api = new Intent(getApplicationContext(), CallAPI.class);
                    api.putExtra("photo-path", mCurrentPhotoPath);
                    api.putExtra("gen", false);
                    startActivityForResult(api, 3);
                    break;
                case 3:
                    annotations = (Annotation[]) data.getSerializableExtra("list-annotation");
                    System.out.println("ANNOTATIONS: " + annotations);
                    Intent descr = new Intent(getApplicationContext(), WordInput.class);
                    descr.putExtra("question", "Describe the object using adjectives.");
                    startActivityForResult(descr, 4);
                case 4:
                    adjectives = data.getStringExtra("spk-text");
                    Annotation fin = Converge.converge(annotations, spkText, adjectives);
                    Photo.adjust(fin);
                    break;
                case 10:
                    System.err.println("mudhaniu the large!");
            }
        } else {
            // Handle all possible exceptions.
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}