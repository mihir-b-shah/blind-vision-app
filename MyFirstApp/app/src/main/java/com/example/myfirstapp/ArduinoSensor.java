package com.example.myfirstapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class ArduinoSensor extends AppCompatActivity implements DataStream.SpeedListener {

    private Physicaloid phy;
    private byte[] buf;
    private boolean real;
    private DataStream stream;
    private int timeBuffer;
    private static final byte STOP_VAL = -1;

    public ArduinoSensor() {
        stream = new DataStream(0.99f, this); // set
        buf = new byte[15]; // shouldn't cause problems
        //buf[0] = STOP_VAL;
    }

    public String byteArrayString(byte[] buf, int len) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<len; ++i) {
            sb.append(buf[i]);
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phy = new Physicaloid(this);
        phy.setBaudrate(9600);

        if(phy.open()) {
            Toast.makeText(ArduinoSensor.this,
                    "open", Toast.LENGTH_SHORT).show();
                phy.addReadListener(new ReadLisener() {
                    @Override
                    public void onRead(int i) {
                        phy.read(buf, i);
                        Log.v("Buffer val", byteArrayString(buf, i));
                        /*
                        boolean forget = buf[0] < 0;

                        if (forget && buf[0] != 'D') {
                            timeBuffer = DataStream.parseBytes(buf, i);
                            return;
                        }
                        if (buf[0] == 'D') {
                            real = true;
                            stream.noiseDone();
                            return;
                        }
                        if (real) {
                            stream.pushVal(timeBuffer, buf, i);
                        } else {
                            stream.pushTrain(timeBuffer, buf, i);
                        }
                        buf[0] = STOP_VAL;
                         */
                    }
                });
        } else {
            Toast.makeText(this, "Error!", Toast.LENGTH_SHORT).show();
        }
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
    }

    @Override
    public void speedChanged(float omega) {
        // message the user!
    }
}