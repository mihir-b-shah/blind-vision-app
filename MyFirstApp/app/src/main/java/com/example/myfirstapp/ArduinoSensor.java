package com.example.myfirstapp;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import static com.example.myfirstapp.MainActivity.INT_1;

public class ArduinoSensor extends IntentService implements DataStream.SpeedListener {

    private Physicaloid phy;
    private byte[] buf;
    private static final byte[] error = "Error encountered".getBytes();
    private int id;
    private boolean real;
    private DataStream stream;
    private int timeBuffer;
    private static final byte STOP_VAL = -1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable toast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), new String(buf), Toast.LENGTH_SHORT).show();
        }
    };

    public ArduinoSensor() {
        super("ArduinoSensor");
        stream = new DataStream(0.99f, this); // set
        buf = new byte[15]; // shouldn't cause problems
        buf[0] = STOP_VAL;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        id = intent.getIntExtra(INT_1, -1);
        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            phy.addReadListener(new ReadLisener() {
                @Override
                public void onRead(int i) {
                    boolean forget = buf[0] < 0;
                    phy.read(buf, i);

                    if(forget && buf[0] != 'D') {
                        timeBuffer = DataStream.parseBytes(buf, i);
                        return;
                    }
                    if(buf[0] == 'D') {
                        real = true;
                        stream.noiseDone();
                        return;
                    }
                    if(real) {
                        stream.pushVal(timeBuffer, buf, i);
                    } else {
                        stream.pushTrain(timeBuffer, buf, i);
                    }
                    buf[0] = STOP_VAL;
                    //handler.post(toast);
                    }
                });
        } //buf = error; handler.post(toast);
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
        Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
        out.putExtra(INT_1, id);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(out);
        stopSelf();
    }

    @Override
    public void speedChanged(float omega) {
        // message the user!
    }
}
