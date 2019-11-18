/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.example.myfirstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;

/*
1. Should manage the main sequence UI.
 */

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    private Session session;
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
                    Intent quest = new Intent(getApplicationContext(), Speak.class);
                    quest.putExtra(EXTRA_MESSAGE, "Please calibrate the phone.");
                    startActivityForResult(quest, 2);
                    break;
                case 2:
                    Intent calibr = new Intent(getApplicationContext(), Calibrate.class);
                    startActivityForResult(calibr, 3);
                    break;
                case 3:
                    float[] vector = data.getFloatArrayExtra("vector");
                    Intent photo = new Intent(getApplicationContext(), Photo.class);
                    startActivityForResult(photo, 4);
                    break;
                case 4:
                    mCurrentPhotoPath = data.getStringExtra("photo-path");
                    Intent api = new Intent(getApplicationContext(), CallGAPI.class);
                    api.putExtra("photo-path", mCurrentPhotoPath);
                    api.putExtra("write-file", "READFILE");
                    api.putExtra("read-file", "READFILE");
                    startActivityForResult(api, 5);
                    break;
                case 5:
                    session = (Session) data.getSerializableExtra("list-annotation");
                    System.out.println("Annotations: " + session);
                    Intent convg = new Intent(getApplicationContext(), ConvgAnnot.class);
                    convg.putExtra("session", session);
                    convg.putExtra("query", spkText);
                    startActivityForResult(convg, 6);
                    break;
                case 6:
                    Annotation annot = session.get_annotation(0);
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}

/*
    Intent sp = new Intent(getApplicationContext(), SpellCheck.class);
        ArrayList<String> copy = new ArrayList<>();
        Annotation an;
        final int size = session.size();

        for(int i = 0; i<size; ++i) {
            if((an = session.get_annotation(i)).t.equals("t")) {
                copy.add(an.d);
            }
        }
        sp.putExtra("input_data", copy);
        startActivityForResult(sp, 4);
        break;
    case 4:
        String[] fixes = data.getStringArrayExtra("corrections");
        final int lim = fixes.length;
        int ctr = 0;
        for(int i = 0; i<lim; ++i) {
            if(session.get_annotation(i).t.equals("t")) {
                session.get_annotation(i).d = fixes[ctr++];
            }
        }
        System.out.println("Fixed annotations: " + session);
 */