package com.example.myfirstapp;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

public class ArduinoSensor extends AppCompatActivity {

    private TextView tv;
    private Physicaloid phy;
    private Handler hand = new Handler();
    private String curr;
    private byte[] buf;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_sensor);
        tv = (TextView) findViewById(R.id.textView);
        phy = new Physicaloid(this);
        phy.setBaudrate(9600);
        if(phy.open()) {
            phy.addReadListener(new ReadLisener() {
                @Override
                public void onRead(int i) {
                    buf = new byte[i];
                    phy.read(buf, i);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvAppend(new String(buf));
                        }
                    });
                }
            });
        } else Toast.makeText(this, "Cannot open!", Toast.LENGTH_LONG).show();
    }

    private void tvAppend(String s) {
        curr = s;
        hand.post(new Runnable() {
            @Override
            public void run() {
                tv.setText(curr);
            }
        });
    }

    private void close() {
        if(phy.close())
            phy.clearReadListener();
    }

}
