package com.apps.navai;

import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class CalibrateHelp extends Calibrate {

    private static final int VOLUME = 85;
    private static final int DURATION = 5000;
    private static final int FREQ = 500;
    private static final int BEEP_LENGTH = 100;

    public int onStartCommand(Intent intent, int flags, int startId) {
        int res = super.onStartCommand(intent, flags, startId);
        final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_SYSTEM, VOLUME);
        Thread beep = new Thread(){
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
        beep.start();
        try {
            beep.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tg.release();
        return res;
    }

    @Override
    public int getNumSamples() {
        return 2;
    }
}
