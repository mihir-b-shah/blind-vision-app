package com.apps.navai;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.physicaloid.lib.Physicaloid;

/**
 * Interface to interact with the hardware.
 */
public class ArduinoSensor extends AppCompatActivity implements DataStream.SpeedListener {

    private Physicaloid phy;
    private ByteArray buf;
    private DataStream stream;

    public ArduinoSensor() {
        stream = new DataStream(0.99f, this); // set
        buf = new ByteArray(12); // grows by expand()
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            phy.addReadListener(i -> {
                buf.expand(i);
                phy.read(buf.getBuffer(), i);
                Log.v("Buffer val", buf.limString(i));
                stream.enqueue(buf.getBuffer(), i);
            });
        }
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
    }

    @Override
    public void speedChanged(double theta, double time) {
        Log.v("Speed chg", String.format("%.3f, %.3f", theta, time));
    }
}