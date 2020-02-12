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
    private int X;
    private int Y;

    static {
        XY_FILTER = "string1&4349039";
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(XY_FILTER)) {
                double Xloc = intent.getDoubleExtra("X", 0D);
                double Yloc = intent.getDoubleExtra("Y", 0D);
                Xloc += X; Yloc += Y;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(XY_FILTER));
    }
}
