package com.example.myfirstapp;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.*;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.*;

import static java.lang.Math.*;

public class Navigate extends AppCompatActivity implements SensorEventListener {

    public static int WIDTH;
    public static int HEIGHT;
    public static int X;
    private boolean calibr;
    private float dir_init;
    public static int Y;
    public static float[] init_mat;
    private SensorManager mSensorManager;
    private Sensor[] sensors;
    private float[][] data;
    private float[] orientationValues;
    private float[] xyz_vect;
    private Display mDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_navigate);
        xyz_vect = new float[3];
        WIDTH = getIntent().getIntExtra("W", -1);
        HEIGHT = getIntent().getIntExtra("H", -1);
        X = getIntent().getIntExtra("X", -1);
        Y = getIntent().getIntExtra("Y", -1);
        init_mat = getIntent().getFloatArrayExtra("rot-mat");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = new Sensor[2];
        sensors[0] = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensors[1] = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        data = new float[2][];

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                data[0] = sensorEvent.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                data[1] = sensorEvent.values.clone();
                break;
            default:
                return;
        }

        if(data[0] == null || data[1] == null) {
            return;
        }

        float[] rotationMatrix = new float[9];
        float[] inclination = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                inclination, data[0], data[1]);

        float[] rotationMatrixAdjusted = new float[9];
        switch (mDisplay.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted);
                break;
        }

        orientationValues = new float[3];
        if (rotationOK)
            SensorManager.getOrientation(rotationMatrixAdjusted,
                    orientationValues);

        float azimuth = orientationValues[0];
        float pitch = orientationValues[1];
        float roll = orientationValues[2];

        if(!calibr) {
            dir_init = azimuth;
            calibr = true;
        }

        System.out.printf("<%.2f, %.2f, %.2f>%n",
                azimuth, pitch, roll);
        /*
        gen_xyz_vect(xyz_vect, azimuth, pitch, roll);
        System.out.printf("<%.2f, %.2f, %.2f>%n",
                xyz_vect[0], xyz_vect[1], xyz_vect[2]); */
    }

    public void gen_xyz_vect(float[] fill, float a, float p, float r) {
        a -= dir_init;
        fill[0] = (float) (cos(a)*sin(r)-sin(a)*sin(p)*cos(r));
        fill[1] = (float) (-sin(a)*sin(r)-cos(a)*sin(p)*cos(r));
        fill[2] = (float) (cos(p)*cos(r));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

}
