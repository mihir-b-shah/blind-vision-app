package com.example.myfirstapp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.content.LocalBroadcastManager;

import androidx.annotation.Nullable;

import java.util.Locale;

import static com.example.myfirstapp.MainActivity.INT_1;

public class Speak extends Service implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private String message;
    private int id;

    public Speak() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        tts = new TextToSpeech(this, this);
        tts.setSpeechRate((float) 0.9);
        tts.setPitch((float) 1.1);
        message = intent.getStringExtra(MainActivity.STRING_1);
        id = intent.getIntExtra(INT_1,-1);
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.ENGLISH);
            speak();
        }
    }

    public void speak() {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "first");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onError(String s) {}
            @Override public void onDone(String s) {
                Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
                out.putExtra(INT_1, id);
                System.out.println("Id: " + id);
                System.out.println("Broadcast sent!");
                LocalBroadcastManager.getInstance(getApplicationContext())
                        .sendBroadcast(out);
                tts.shutdown();
                Speak.this.stopSelf();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
