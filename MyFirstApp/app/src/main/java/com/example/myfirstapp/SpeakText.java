package com.example.myfirstapp;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class SpeakText implements TextToSpeech.OnInitListener {

    private class BoolWrap {
        boolean b;
        BoolWrap(boolean b) {
            this.b = b;
        }
    }

    private final TextToSpeech tts;
    private static final float SPEECH_RATE = 0.9f;
    private static final float PITCH = 1.1f;
    private static int ctr;
    private final BoolWrap done;

    public SpeakText(Context cxt) {
        tts = new TextToSpeech(cxt, this);
        tts.setSpeechRate(SPEECH_RATE);
        tts.setPitch(PITCH);
        System.out.println("Line 28");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String s) {}
            @Override public void onError(String s) {}
            @Override public void onDone(String s) {
                synchronized (done) {
                    done.notify();
                }
            }
        });
        System.out.println("Line 38");
        done = new BoolWrap(false);
    }

    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.ENGLISH);
        }
        System.out.println("About to send notification!");
        synchronized (done) {
            done.notify();
        }
    }

    public void speak(String message) {
        synchronized(done) {
            try {
                done.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, Integer.toString(ctr++));
        // wait for done, then return
        synchronized(done) {
            try {
                done.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void shutTTS() {
        tts.shutdown();
    }
}
