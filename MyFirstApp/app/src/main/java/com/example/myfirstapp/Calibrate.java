package com.example.myfirstapp;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import androidx.annotation.Nullable;

import java.util.Arrays;

import static com.example.myfirstapp.MainActivity.INT_1;

public class Calibrate extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor[] sensors;
    private float[][] data;
    private boolean set;
    private int ctr;
    private int id;

    public Calibrate() {}

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        id = intent.getIntExtra(INT_1, -1);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = new Sensor[2];
        sensors[0] = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensors[1] = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        data = new float[2][3];
        for(float[] dat: data) {
            Arrays.fill(dat, -1f);
        }
        if (sensors[0] != null)
            mSensorManager.registerListener(this, sensors[0],
                    SensorManager.SENSOR_DELAY_NORMAL);

        if (sensors[1] != null)
            mSensorManager.registerListener(this, sensors[1],
                    SensorManager.SENSOR_DELAY_NORMAL);
        System.out.println("id is set line 54!");
        return Service.START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
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

        System.out.println(getClass().toString() + " Sensor changed! Line 88.");

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

        float[] orientationValues = new float[3];
        if (rotationOK)
            SensorManager.getOrientation(rotationMatrix, orientationValues);

        if(ctr > getNumSamples()) {
            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            System.out.println("Got to stopself line 94");
            mSensorManager.unregisterListener(this, sensors[0]);
            mSensorManager.unregisterListener(this, sensors[1]);

            out.putExtra("vector", orientationValues);
            System.out.println("here is the id at 101: " + id);
            out.putExtra(INT_1, id);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            stopSelf();
        }
    }

    public int getNumSamples() {
        return 1;
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
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
