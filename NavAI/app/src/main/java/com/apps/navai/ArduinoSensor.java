package com.apps.navai;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class ArduinoSensor extends AppCompatActivity implements DataStream.SpeedListener {

    private Physicaloid phy;
    private ByteBuffer buf;
    private DataStream stream;

    public ArduinoSensor() {
        stream = new DataStream(0.99f, this); // set
        buf = new ByteBuffer(12); // grows by expand()
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            phy.addReadListener(new ReadLisener() {
                @Override
                public void onRead(int i) {
                    buf.expand(i);
                    phy.read(buf.getBuffer(), i);
                    Log.v("Buffer val", buf.limString(i));
                    stream.enqueue(buf.getBuffer(), i);
                }
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