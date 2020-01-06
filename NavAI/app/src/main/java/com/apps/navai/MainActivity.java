/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.apps.navai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;

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

    static {
        STRING_1 = "string1_8941930";
        STRING_2 = "string2_1210210";
        STRING_3 = "string3_6795899";
        SERVICE_RESPONSE = "stringG1_32932093";
        INT_1 = "int1_4390403";
        INT_2 = "int2_1201021";
        FLOAT_1 = "float1_7864059";
    }

    private Session session;
    private float[] calibrVect;
    private float initDir;
    private String spkText;
    private Annotation convgd;
    private String adjectives;
    private String mCurrentPhotoPath;
    private boolean first = true;
    private Calibrate.DirVector photoVect;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() != null && intent.getAction().equals(SERVICE_RESPONSE)) {
                int code = intent.getIntExtra(INT_1, -1);
                Intent next;
                switch (code) {
                    case 0:
                        next = new Intent(getApplicationContext(), WordInput.class);
                        first = false;
                        startActivityForResult(next, 1);
                        break;
                    case 2:
                        next = new Intent(getApplicationContext(), CalibrateHelp.class);
                        next.putExtra(INT_1, 3);
                        startService(next);
                        break;
                    case 3:
                        initDir = intent.getFloatExtra("currdir", Calibrate.DIR_NOTEXIST);
                        next = new Intent(getApplicationContext(), Photo.class);
                        next.putExtra(FLOAT_1, initDir);
                        startActivityForResult(next, 4);
                        break;
                    case 5:
                        System.out.println("Got out of CallAPI!");
                        session = (Session) intent.getSerializableExtra("session");
                        next = new Intent(getApplicationContext(), Converge.class);
                        next.putExtra(STRING_2, session);
                        next.putExtra(STRING_1, spkText);
                        next.putExtra(INT_1, 6);
                        startService(next);
                        break;
                    case 6:
                        session = (Session) intent.getSerializableExtra("session");
                        Annotation annot = session.getAnnotation(0);
                        boolean right;
                        if(annot.getRect() != null)
                            right = annot.getRect().centerX() > session.getImageWidth() >> 1;
                        else
                            right = false;
                        next = new Intent(getApplicationContext(), Speak.class);
                        next.putExtra(STRING_1, "The object found is to your " +
                                (right ? "right" : "left"));
                        next.putExtra(INT_1, 7);
                        startService(next);
                        break;
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
    @Override
    protected void onCreate(Bundle st) {
        super.onCreate(st);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        mCurrentPhotoPath = st == null ? null : st.getString("photo-path");
        spkText = st == null ? null : st.getString("spk-text");
        first = st == null;
        initDir = st == null ? Calibrate.DIR_NOTEXIST : st.getFloat("init-dir");

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));

        // below this change for testing
        /*
        Intent start = new Intent(getApplicationContext(), ArduinoSensor.class);
        startActivityForResult(start, 10);
        */

        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(STRING_1, "Hello welcome to my assisted navigation app. What " +
                "are you looking for?");
        start.putExtra(INT_1, 0);
        if(first) startService(start);
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("photo-path", mCurrentPhotoPath);
        b.putString("spk-text", spkText);
        b.putFloat("init-dir", initDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent next;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                    spkText = data.getStringExtra(STRING_1);
                    next = new Intent(getApplicationContext(), Speak.class);
                    next.putExtra(STRING_1, "Please calibrate the phone.");
                    next.putExtra(INT_1, 2);
                    startService(next);
                    break;
                case 4:
                    mCurrentPhotoPath = data.getStringExtra("photo-path");
                    photoVect = (Calibrate.DirVector) data.getSerializableExtra("vector");
                    next = new Intent(getApplicationContext(), CallAPI.class);
                    next.putExtra(STRING_1, mCurrentPhotoPath);
                    next.putExtra(STRING_2, "READFILE");
                    next.putExtra(STRING_3, "READFILE");
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