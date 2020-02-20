package com.apps.navai;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;

public class Navigate extends AppCompatActivity {

    public static final String XY_FILTER;
    private double X;
    private double Y;

    private double tgtX;
    private double tgtY;

    private static final int VOLUME = 85;
    private static final int DURATION = 150;
    private static final int FREQ = 40;
    private static final int BEEP_LENGTH = 10;

    static {
        XY_FILTER = "string1&4349039";
    }

    private Thread beep;
    private ToneGenerator tg;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(XY_FILTER)) {
                double Xloc = intent.getDoubleExtra("X", 0D);
                double Yloc = intent.getDoubleExtra("Y", 0D);
                double Xbear = intent.getDoubleExtra(ArduinoInterface.DIR_X, 0D);
                double Ybear = intent.getDoubleExtra(ArduinoInterface.DIR_Y, 1D);

                X = Xloc; Y = Yloc;
                double xComp = tgtX-X; double yComp = tgtY-Y;
                if(Math.sqrt(xComp*xComp+yComp*yComp)<0.5) {
                    Intent speak = new Intent(getApplicationContext(), Speak.class);
                    speak.putExtra(MainActivity.STRING_1,"You have arrived.");
                    speak.putExtra(MainActivity.INT_1, 0);
                    startService(speak);
                    return;
                }
                double dot = (xComp*Xbear+yComp*Ybear)/Math.sqrt(xComp*xComp+yComp*yComp);
                if(Math.abs(Math.acos(dot)) > Math.PI/6) {
                    beep.start();
                    try {
                        beep.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tg.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        PhotoUtils.PolarVector pv = (PhotoUtils.PolarVector)
                getIntent().getSerializableExtra(MainActivity.VECTOR_1);
        tgtX = pv.getMgn()*Math.cos(pv.getDir());
        tgtY = pv.getMgn()*Math.sin(pv.getDir());

        tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME);
        beep = new Thread(){
            final int DIFF = FREQ-BEEP_LENGTH;
            @Override
            public void run() {
                for(int time = 0; time<DURATION; time+=FREQ){
                    tg.stopTone();
                    tg.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP, BEEP_LENGTH);
                    try {
                        sleep(DIFF);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                receiver, new IntentFilter(XY_FILTER));
        Intent intent = new Intent(ArduinoInterface.INPUT_FILTER);
        intent.putExtra(ArduinoInterface.MESSAGE_KEY, ArduinoInterface.MESSAGE_CHANGE);
        sendBroadcast(intent);
    }
}
