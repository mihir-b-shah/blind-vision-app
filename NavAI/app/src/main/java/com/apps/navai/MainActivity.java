/**
 * Manages the main UI thread of the application and launches all the sub-activities.
 */

package com.apps.navai;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.SizeF;

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
    public static final String VECTOR_1;
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
        VECTOR_1 = "vector1_0430440";
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
    private PhotoUtils.PolarVector vect;

    @SuppressLint("DefaultLocale")
    private String genMsg(PhotoUtils.PolarVector vect) {
        int angle = (int) (6*vect.getDir()/Math.PI);
        if(angle < 0) {
            if (angle == 0) {
                return "Straight forward:";
            } else {
                return String.format("%d o'clock", angle);
            }
        } else {
            if (angle == 0) {
                return "Straight forward:";
            } else {
                return String.format("%d o'clock", 12 - angle);
            }
        }
    }

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
                        session.sortFirst((a1,a2)->a1.getRTag()-a2.getRTag());
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
                        session2.sortSecond((a1,a2)->a1.getRTag()-a2.getRTag());
                        next = new Intent(getApplicationContext(), SpellCheck.class);
                        String[] input = session.getDescrArray(0);
                        System.out.println("INPUT ARRAY 0: " + Arrays.toString(input));
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
                        System.out.println("INPUT ARRAY 0: " + Arrays.toString(input));
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
                        Annotation frame1 = session.getAnnotationFirst(0);
                        Annotation frame2 = session.getAnnotationSecond(0);

                        CameraManager ref = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                        vect = PhotoUtils.calcTrajectory(ref, focusDist1,
                                focusDist2, frame1, frame2, rotMat, rotMat2);
                        System.out.println(vect.getMgn() + " " + vect.getDir());
                        next = new Intent(getApplicationContext(), Speak.class);
                        next.putExtra(STRING_1, genMsg(vect));
                        next.putExtra(INT_1, 11);
                        startService(next);
                    case 11:
                        next = new Intent(getApplicationContext(), Navigate.class);
                        next.putExtra(VECTOR_1, vect);
                        startActivityForResult(next, 12);
                    case 12:
                        System.out.println("We're done!");
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
        vect = (PhotoUtils.PolarVector) (st == null ? null : st.getSerializable("vector"));

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(SERVICE_RESPONSE));

        Intent arduino = new Intent(getApplicationContext(), ArduinoInterface.class);
        arduino.putExtra(INT_1, 13);
        if(first) startService(arduino);

        Intent start = new Intent(getApplicationContext(), Speak.class);
        start.putExtra(STRING_1, "Hello welcome to my assisted navigation app. What " +
                "are you looking for?");
        start.putExtra(INT_1, 0);
        if(first) startService(start);

        /*
        Annotation[] a1s = new Annotation[3];
        a1s[0] = new Annotation('o', "book\\tbox\\tblue", 0.67f, new Rect(
                0,0,0,0));
        double[] dat1 = new double[3];
        dat1[0] = 0.93; dat1[1] = 0.31; dat1[2] = 0.17;
        a1s[0].extra = dat1;

        a1s[1] = new Annotation('t', "Hacker\\tnull", 0.89f, new Rect(
                0,0,0,0));
        a1s[2] = new Annotation('t', "delight\\tnull", 0.94f, new Rect(
                0,0,0,0));
        Annotation[] a2s = new Annotation[4];
        a2s[0] = new Annotation('o', "book\\tblack", 0.37f, new Rect(
                0,0,0,0));
        double[] dat2 = new double[3];
        dat2[0] = 0.87; dat1[1] = 0.23; dat1[2] = 0.09;
        a2s[0].extra = dat2;

        a2s[1] = new Annotation('t', "Hacker\\tnull", 0.75f, new Rect(
                0,0,0,0));
        a2s[2] = new Annotation('t', "delight\\tnull", 0.51f, new Rect(
                0,0,0,0));
        a2s[3] = new Annotation('t', "Warren\\tnull", 0.44f, new Rect(
                0,0,0,0));

        Session s = new Session(a1s, a2s, "", "", "", "");
        Intent start = new Intent(getApplicationContext(), Converge.class);
        start.putExtra(STRING_2, s);
        start.putExtra(STRING_1, "hacker");
        start.putExtra(INT_1, 159);
        startService(start);
        */
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
        b.putSerializable("vector", vect);
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
                case 11:
                    System.out.println("Program done!");
                    break;
                default:
                    System.err.println("Code not recognized.");
                    break;
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}