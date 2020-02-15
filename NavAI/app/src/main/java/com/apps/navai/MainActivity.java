/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.apps.navai;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.Arrays;

/*
1. Should manage the main sequence UI.
 */

public class MainActivity extends AppCompatActivity {

    public static final String STRING_1;
    public static final String STRING_2;
    public static final String STRING_3;
    public static final String STRING_ARRAY_1;
    public static final String SERVICE_RESPONSE;
    public static final String INT_1;
    public static final String INT_2;
    public static final String FLOAT_1;

    public static final int CAMERA_CALL_COUNT;

    static {
        STRING_1 = "string1_8941930";
        STRING_2 = "string2_1210210";
        STRING_3 = "string3_6795899";
        STRING_ARRAY_1 = "string_array1_54059495";
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
    private boolean first = true;
    private float[] rotMat;
    private float[] rotMat2;
    private float focusDist1;
    private float focusDist2;

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
                        System.out.println("ROTMAT: " + Arrays.toString(rotMat));
                        next = new Intent(getApplicationContext(), CustomCamera.class);
                        startActivityForResult(next, 4);
                        break;
                    case 5:
                        System.out.println("Arrived at case 5");
                        rotMat2 = intent.getFloatArrayExtra("rot-mat");
                        System.out.println("ROTMAT: " + Arrays.toString(rotMat2));
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
                        System.out.println(session);
                        next = new Intent(getApplicationContext(), CallAPI.class);
                        next.putExtra(STRING_1, photoPath2);
                        next.putExtra(STRING_2, "READFILE_TWO");
                        next.putExtra(STRING_3, (String) null);
                        next.putExtra(INT_1, 7);
                        next.putExtra(INT_2, 1);
                        startService(next);
                        break;
                    case 7:
                        session2 = (Session) intent.getSerializableExtra("session");
                        next = new Intent(getApplicationContext(), SpellCheck.class);
                        String[] input = session.getDescrArray(0);
                        next.putExtra(INT_1, 8);
                        next.putExtra(STRING_ARRAY_1, input);
                        startService(next);
                        break;
                    case 8:
                        ArrayList<String> output = intent.getStringArrayListExtra("output");
                        SpellCheck.FloatVector conf = (SpellCheck.FloatVector)
                                intent.getSerializableExtra("conf");
                        session.setOutput(0, output, conf);
                        next = new Intent(getApplicationContext(), SpellCheck.class);
                        input = session2.getDescrArray(1);
                        next.putExtra(INT_1, 9);
                        next.putExtra(STRING_ARRAY_1, input);
                        startService(next);
                        break;
                    case 9:
                        output = intent.getStringArrayListExtra("output");
                        conf = (SpellCheck.FloatVector)
                                intent.getSerializableExtra("conf");
                        session2.setOutput(1, output, conf);
                        Session combine = Session.combine(session, session2);
                        combine.display();
                        next = new Intent(getApplicationContext(), Converge.class);
                        next.putExtra(STRING_2, combine);
                        next.putExtra(STRING_1, spkText);
                        next.putExtra(INT_1, 10);
                        startService(next);
                        break;
                    case 10:
                        session = (Session) intent.getSerializableExtra("session");
                        break;
                    default:
                        System.err.println("Error code: " + code);
                        System.err.println("Process id not recognized.");
                        break;
                    // need to add the CalibrateHelp stuff in here.
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
        System.out.println("On create called.");
        super.onCreate(st);
        setContentView(R.layout.activity_main);

        mCurrentPhotoPath = st == null ? null : st.getString("photo-path");
        spkText = st == null ? null : st.getString("spk-text");
        first = st == null;
        rotMat = st == null ? null : st.getFloatArray("rot-mat-1");
        rotMat2 = st == null ? null : st.getFloatArray("rot-mat-2");
        photoPath2 = st == null ? null : st.getString("photo-path-2");
        focusDist1 = st == null ? -1f : st.getFloat("focus-dist-1", focusDist1);
        focusDist2 = st == null ? -1f : st.getFloat("focus-dist-2", focusDist2);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));

        /*
        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(STRING_1, "Hello welcome to my assisted navigation app. What " +
                "are you looking for?");
        start.putExtra(INT_1, 0);
        if(first) startService(start); */

        Annotation annot1 = new Annotation('t', "Big cat", 0.97f,
                new Rect(894, 457, 895, 459));
        Annotation annot2 = new Annotation('t', "Big cat", 0.71f,
                new Rect(889, 690, 891, 691));
        float[] rotMat1 = {0.07056068f, 0.045109104f, -0.996487f,
                           0.0020612627f, 0.99896824f, 0.045367382f,
                           0.99750537f, -0.0052551744f, 0.07039489f};
        float[] rotMat2 = {-0.09723142f, 0.004867336f, -0.9952499f,
                           -0.031096f, 0.99948496f, 0.007925986f,
                            0.9947759f, 0.031718943f, -0.09702999f};
        float fd1 = 0.5494943f;
        float fd2 = 0.5494943f;
        CameraManager ref = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        PhotoUtils.PolarVector vect = PhotoUtils.calcTrajectory(ref, fd1, fd2,
                annot1, annot2, rotMat1, rotMat2);

        /*
        for this test data, I should get approximately 0.5 m as distance and close to 0 degrees
        for the angle.
         */
        System.out.println(vect.getMgn() + " " + vect.getDir());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        b.putString("photo-path", mCurrentPhotoPath);
        b.putString("spk-text", spkText);
        b.putString("photo-path-2", photoPath2);
        b.putFloatArray("rot-mat-1", rotMat);
        b.putFloatArray("rot-mat-2", rotMat2);
        b.putFloat("focus-dist-1", focusDist1);
        b.putFloat("focus-dist-2", focusDist2);
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
                    focusDist1 = data.getFloatExtra("focus-distance", -1f);
                    System.out.println("FOCDIST1: " + focusDist1);
                    next = new Intent(getApplicationContext(), Calibrate.class);
                    next.putExtra(INT_1, 3);
                    startService(next);
                    break;
                case 4:
                    System.out.println("Arrived at case 4");
                    photoPath2 = data.getStringExtra("photo-path");
                    focusDist2 = data.getFloatExtra("focus-distance", -1f);
                    System.out.println("FOCDIST1: " + focusDist2);
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