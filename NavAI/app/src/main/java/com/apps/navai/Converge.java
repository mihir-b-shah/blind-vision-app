package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.PriorityQueue;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.STRING_1;
import static com.apps.navai.MainActivity.STRING_2;

public class Converge extends IntentService {

    private Session session;
    private String keyword;
    private static final int NUM_ANNOT = 1;
    private int id;

    public Converge() {
        super("Converge");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        id = intent.getIntExtra(INT_1,-1);
        keyword = intent.getStringExtra(STRING_1);
        session = (Session) intent.getSerializableExtra(STRING_2);
        converge();
    }

    private class Pair implements Comparable<Pair> {
        final Annotation sess;
        final float conf;

        Pair(Annotation s, float c) {
            sess = s;
            conf = c;
        }

        @Override
        public int compareTo(Pair other) {
            return Float.compare(other.conf, conf);
        }
    }

    public void converge() {
        int size = session.sizeOne();
        String[] buffer = new String[size+1];
        buffer[0] = keyword;
        for(int i = 0; i<size; ++i) {
            buffer[i+1] = session.getAnnotationFirst(i).getDescription();
        }
        float[] scores = genScores(buffer);
        Annotation[] a1 = convergeScores(0, scores);
        for(int i = 0; i<size; ++i) {
            buffer[i+1] = session.getAnnotationSecond(i).getDescription();
        }
        scores = genScores(buffer);
        Annotation[] a2 = convergeScores(1, scores);
        done(a1, a2);
    }

    private Annotation[] convergeScores(int id, float[] f) {
        PriorityQueue<Pair> pq = new PriorityQueue<>();
        for(int i = 0; i<f.length; ++i) {
            pq.offer(new Pair(id == 0 ? session.getAnnotationFirst(i) :
                    session.getAnnotationSecond(i), f[i]));
        }
        Annotation[] result = new Annotation[NUM_ANNOT];
        if(NUM_ANNOT > pq.size()) {
            Converge.this.stopSelf();
            System.err.println("NUM_ANNOT too big. Fatal exception occurred.");

        } else {
            for (int i = 0; i < NUM_ANNOT; ++i) {
                Annotation a = pq.poll().sess;
                a.setMatch(f[i]);
                result[i] = a;
            }
        }
        return result;
    }

    private void done(Annotation[] a1, Annotation[] a2) {
        Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
        session.setAnnotationsOne(a1);
        session.setAnnotationsTwo(a2);
        out.putExtra("session", session);
        out.putExtra(INT_1, id);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(out);
        Converge.this.stopSelf();
    }

    private float[] genScores(String[] buffer) {
        URL url;
        HttpURLConnection con;
        try {
            url = new URL(getString(R.string.myapi));
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            OutputStream os = con.getOutputStream();
            String str = String.format("{\"w1\":\"%s\", \"w2\":\"%s\"}", arrayString(buffer),
                    buffer[0]);
            byte[] write = str.getBytes(StandardCharsets.UTF_8);
            os.write(write);
            os.flush();
            os.close();
            float[] result = null;
            con.connect();
            if(con.getResponseCode() == 200) {
                InputStream instr = con.getInputStream();
                result = readFloatArray(buffer.length-1, instr);
            } else {
                System.err.printf("%d, Request did not go through correctly.%n",
                        con.getResponseCode());
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String arrayString(String[] strings) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(int i = 1; i<strings.length; ++i) {
            sb.append(String.format("%s, ", strings[i].trim().toLowerCase()));
        }
        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.append(']');
        return sb.toString();
    }

    private float[] readFloatArray(int N, InputStream instr) throws IOException {
        byte buf;
        float[] floats = new float[N];
        int ctr = 0;
        ByteArray buffer = new ByteArray();
        outer: while(true) {
            switch (buf = (byte) instr.read()) {
                case '[':
                case ' ': break;
                case ',': floats[ctr++] = Float.parseFloat(new String(buffer.getBuffer()));
                    buffer.clear(); break;
                case ']': floats[ctr] = Float.parseFloat(new String(buffer.getBuffer()));
                    buffer.clear(); break outer;
                default: buffer.add(buf);
            }
        }
        return floats;
    }
}