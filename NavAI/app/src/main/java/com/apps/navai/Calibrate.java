package com.apps.navai;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.Arrays;

import static com.apps.navai.MainActivity.INT_1;

public class Calibrate extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor[] sensors;
    private float[][] data;
    private boolean set;
    private int ctr;
    private int id;

    public static final float DIR_NOTEXIST = -100f;

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
        sensors[0] = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
            case Sensor.TYPE_GRAVITY:
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
        if(!rotationOK) {
            return;
        }

        if(ctr > getNumSamples()) {
            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            mSensorManager.unregisterListener(this, sensors[0]);
            mSensorManager.unregisterListener(this, sensors[1]);

            System.out.println("Got the rotation matrix: " + Arrays.toString(rotationMatrix));
            out.putExtra("rot-mat", rotationMatrix);
            out.putExtra(INT_1, id);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            stopSelf();
        }
    }

    public float mgn(float[] v) {
        return (float) Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
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
