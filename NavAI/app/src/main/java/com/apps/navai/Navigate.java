package com.apps.navai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

public class Navigate extends AppCompatActivity {

    public static final String XY_FILTER;
    private double X;
    private double Y;

    private double tgtX;
    private double tgtY;

    static {
        XY_FILTER = "string1&4349039";
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(XY_FILTER)) {
                double Xloc = intent.getDoubleExtra("X", 0D);
                double Yloc = intent.getDoubleExtra("Y", 0D);
                X = Xloc; Y = Yloc;

                // current direction and remaining vector.

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        PhotoUtils.PolarVector pv = (PhotoUtils.PolarVector)
                getIntent().getSerializableExtra(MainActivity.VECTOR_1);
        tgtX = pv.getMgn()*Math.cos(pv.getDir());
        tgtY = pv.getMgn()*Math.sin(pv.getDir());

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(XY_FILTER));
        Intent intent = new Intent(ArduinoInterface.INPUT_FILTER);
        intent.putExtra(ArduinoInterface.MESSAGE_KEY, ArduinoInterface.MESSAGE_CHANGE);
        sendBroadcast(intent);
    }
}
