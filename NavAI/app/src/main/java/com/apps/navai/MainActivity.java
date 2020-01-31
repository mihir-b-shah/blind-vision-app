/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.apps.navai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;

/*
1. Should manage the main sequence UI.
 */

public class MainActivity extends AppCompatActivity {

    public static final String STRING_1;
    public static final String STRING_2;
    public static final String STRING_3;
    public static final String SERVICE_RESPONSE;
    public static final String INT_1;
    public static final String INT_2;
    public static final String FLOAT_1;

    public static final int CAMERA_CALL_COUNT;

    static {
        STRING_1 = "string1_8941930";
        STRING_2 = "string2_1210210";
        STRING_3 = "string3_6795899";
        SERVICE_RESPONSE = "stringG1_32932093";
        INT_1 = "int1_4390403";
        INT_2 = "int2_1201021";
        FLOAT_1 = "float1_7864059";
        CAMERA_CALL_COUNT = 2;
    }

    private Session session;
    private Session session2;
    private String spkText;
    private String mCurrentPhotoPath;
    private String photoPath2;
    private AudioManager audioManager;
    private boolean first = true;
    private float[] rotMat;
    private float[] rotMat2;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        @SuppressWarnings("deprecation")
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(SERVICE_RESPONSE)) {
                int code = intent.getIntExtra(INT_1, -1);
                Intent next;
                switch (code) {
                    case 0:
                        System.out.println("Arrived at case 0");
                        next = new Intent(getApplicationContext(), WordInput.class);
                        first = false;
                        startActivityForResult(next, 1);
                        break;
                    case 3:
                        System.out.println("Arrived at case 3");
                        rotMat = intent.getFloatArrayExtra("rot-mat");
                        next = new Intent(getApplicationContext(), CustomCamera.class);
                        startActivityForResult(next, 4);
                        break;
                    case 5:
                        System.out.println("Arrived at case 5");
                        rotMat2 = intent.getFloatArrayExtra("rot-mat");
                        next = new Intent(getApplicationContext(), CallAPI.class);
                        next.putExtra(STRING_1, mCurrentPhotoPath);
                        next.putExtra(STRING_2, "READFILE");
                        next.putExtra(STRING_3, (String) null);
                        next.putExtra(INT_1, 6);
                        next.putExtra(INT_2, 0);
                        startService(next);
                        break;
                    case 6:
                        System.out.println("Arrived at case 6");
                        session = (Session) intent.getSerializableExtra("session");
                        next = new Intent(getApplicationContext(), CallAPI.class);
                        next.putExtra(STRING_1, photoPath2);
                        next.putExtra(STRING_2, "READFILE_TWO");
                        next.putExtra(STRING_3, (String) null);
                        next.putExtra(INT_1, 7);
                        next.putExtra(INT_2, 1);
                        startService(next);
                        break;
                    case 7:
                        System.out.println("Arrived at case 7");
                        session2 = (Session) intent.getSerializableExtra("session");
                        next = new Intent(getApplicationContext(), Converge.class);
                        next.putExtra(STRING_2, Session.combine(session, session2));
                        next.putExtra(STRING_1, spkText);
                        next.putExtra(INT_1, 8);
                        startService(next);
                        break;
                    case 8:
                        session = (Session) intent.getSerializableExtra("session");
                        break;
                        // DONE
                    default:
                        System.err.println("Error code: " + code);
                        System.err.println("Process id not recognized.");
                        break;
                }
            }
        }
    };

    /**
     * Creates the application and launches the audio intro.
     *
     * @param st the data needed to construct the application.
     */
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle st) {
        super.onCreate(st);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // deprecated but simple
        if(audioManager.isWiredHeadsetOn()) {
            audioManager.setMode(AudioManager.MODE_RINGTONE | AudioManager.MODE_IN_CALL);
            audioManager.setSpeakerphoneOn(true);
        }

        mCurrentPhotoPath = st == null ? null : st.getString("photo-path");
        spkText = st == null ? null : st.getString("spk-text");
        first = st == null;
        rotMat = st == null ? null : st.getFloatArray("rot-mat-1");
        rotMat2 = st == null ? null : st.getFloatArray("rot-mat-2");
        photoPath2 = st == null ? null : st.getString("photo-path-2");

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));

        /*
        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(STRING_1, "Hello welcome to my assisted navigation app. What " +
                "are you looking for?");
        start.putExtra(INT_1, 0);
        if(first) startService(start);
        */

        String[] output = StringUtils.correct(this, "hello\tphysica\thackr");
        System.out.println(Arrays.toString(output));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(audioManager.getMode() != AudioManager.MODE_NORMAL ||
                audioManager.isWiredHeadsetOn()) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
            audioManager.setSpeakerphoneOn(false);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("photo-path", mCurrentPhotoPath);
        b.putString("spk-text", spkText);
        b.putString("photo-path-2", photoPath2);
        b.putFloatArray("rot-mat-1", rotMat);
        b.putFloatArray("rot-mat-2", rotMat2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent next;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                    System.out.println("Arrived at case 1");
                    spkText = data.getStringExtra(STRING_1);
                    next = new Intent(getApplicationContext(), CustomCamera.class);
                    startActivityForResult(next, 2);
                    break;
                case 2:
                    System.out.println("Arrived at case 2");
                    mCurrentPhotoPath = data.getStringExtra("photo-path");
                    next = new Intent(getApplicationContext(), Calibrate.class);
                    next.putExtra(INT_1, 3);
                    startService(next);
                    break;
                case 4:
                    System.out.println("Arrived at case 4");
                    photoPath2 = data.getStringExtra("photo-path");
                    next = new Intent(getApplicationContext(), Calibrate.class);
                    next.putExtra(INT_1, 5);
                    startService(next);
                    break;
                default:
                    System.err.println("Code not recognized.");
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}