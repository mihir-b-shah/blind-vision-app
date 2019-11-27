package com.example.myfirstapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Arrays;

public class Calibrate extends AppCompatActivity implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor[] sensors;
    private static final int VOLUME = 85;
    private static final int DURATION = 5000;
    private static final int FREQ = 500;
    private static final int BEEP_LENGTH = 100;
    private static final int NUM_SAMPLES = 10;
    private float[][] data;
    private float[] orientationValues;
    private boolean set;
    private int ctr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_navigate);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = new Sensor[2];
        sensors[0] = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensors[1] = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        data = new float[2][3];
        for(float[] dat: data) {
            Arrays.fill(dat, -1f);
        }
    }

    /*
    Sensor data Adapted from https://github.com/google-developer-training/android-advanced/blob/master
    /TiltSpot/app/src/main/java/com/example/android/tiltspot/MainActivity.java
     */

    @Override
    protected void onStart() {
        super.onStart();

        if (sensors[0] != null)
            mSensorManager.registerListener(this, sensors[0],
                    SensorManager.SENSOR_DELAY_NORMAL);

        if (sensors[1] != null)
            mSensorManager.registerListener(this, sensors[1],
                    SensorManager.SENSOR_DELAY_NORMAL);

        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME);
        Thread beep = new Thread(){
            final int DIFF = FREQ-BEEP_LENGTH;
            @Override
            public void run() {
                for(int time = 0; time<DURATION; time+=FREQ){
                    tg.stopTone();
                    tg.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, BEEP_LENGTH);
                    try {
                        sleep(DIFF);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        beep.start();
        try {
            beep.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tg.release();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this, sensors[0]);
        mSensorManager.unregisterListener(this, sensors[1]);
    }

    private boolean nullCheck(float[] array) {
        for(int i = 0; i<array.length; ++i) {
            if(Float.compare(array[i], -1f) != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(sensorEvent.values, 0, data[0], 0, data[0].length);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(sensorEvent.values, 0, data[1], 0, data[1].length);
                break;
        }

        if(!set && (nullCheck(data[0]) || nullCheck(data[1]))) {
            return;
        } else {
            set = true;
        }

        ++ctr;
        float[] rotationMatrix = new float[9];
        float[] inclination = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                inclination, data[0], data[1]);

        orientationValues = new float[3];
        if (rotationOK)
            SensorManager.getOrientation(rotationMatrix, orientationValues);

        if(ctr > NUM_SAMPLES) {
            Intent out = new Intent();
            out.putExtra("vector", orientationValues);
            Calibrate.this.setResult(Activity.RESULT_OK, out);
            Calibrate.this.finish();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}
