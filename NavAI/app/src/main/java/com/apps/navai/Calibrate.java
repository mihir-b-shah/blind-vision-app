package com.apps.navai;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;

import static com.apps.navai.MainActivity.FLOAT_1;
import static com.apps.navai.MainActivity.INT_1;
import static java.lang.Math.*;

public class Calibrate extends Service implements SensorEventListener {

    public static final float DIR_NOTEXIST = -100f;
    private SensorManager mSensorManager;
    private Sensor[] sensors;
    private float[][] data;
    private boolean set;
    private float initDir;
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
        initDir = intent.getFloatExtra(FLOAT_1,DIR_NOTEXIST);
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
            mSensorManager.unregisterListener(this, sensors[0]);
            mSensorManager.unregisterListener(this, sensors[1]);

            if(initDir == DIR_NOTEXIST) {
                out.putExtra("currdir", orientationValues[0]);
            } else {
                out.putExtra("vector", new DirVector(orientationValues, initDir));
            }
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

    public class DirVector implements Serializable {
        private float x,y,z;

        public DirVector() {}
        public DirVector(float[] APR, float a0) {
            update(APR, a0);
        }

        public float getX() {return x;}
        public float getY() {return y;}
        public float getZ() {return z;}

        public void update(float[] APR, float a0) {
            x = (float) (cos(APR[0]-a0)*sin(APR[2])-sin(APR[0]-a0)*sin(APR[1])*cos(APR[2]));
            y = (float) (-sin(APR[0]-a0)*sin(APR[2])-cos(APR[0]-a0)*sin(APR[1])*cos(APR[2]));
            z = (float) (cos(APR[1])*cos(APR[2]));
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.writeFloat(x);
            oos.writeFloat(y);
            oos.writeFloat(z);
        }

        private void readObject(ObjectInputStream ois) throws IOException {
            x = ois.readFloat();
            y = ois.readFloat();
            z = ois.readFloat();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
