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
    private Sensor sensor;
    private float[] data;
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
        sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        data = new float[5];
        Arrays.fill(data, -1f);

        if (sensor != null)
            mSensorManager.registerListener(this, sensor,
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
        if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(sensorEvent.values, 0, data, 0, data.length);
        }

        ++ctr;

        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, data);

        if(ctr > getNumSamples()) {
            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            mSensorManager.unregisterListener(this, sensor);

            System.out.println("Got the rotation matrix: " + Arrays.toString(rotationMatrix));
            out.putExtra("rot-mat", rotationMatrix);
            out.putExtra(INT_1, id);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            stopSelf();
        }
    }

    private final void matVectMult(float[] vect, float[] mat) {
        float[] newvect = new float[3];
        for(int i = 0; i<vect.length; ++i) {
            for(int j = 0; j<vect.length; ++j) {
                newvect[i] += mat[vect.length*i+j]*vect[j];
            }
        }
        System.arraycopy(newvect, 0, vect, 0, vect.length);
    }

    private final float dot(float[] v1, float[] v2) {
        return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
    }

    public int getNumSamples() {
        return 2;
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
