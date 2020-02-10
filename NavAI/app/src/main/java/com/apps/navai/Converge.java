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

    private int numObjScores;
    private int numTxtScores;

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
        float[] scores = genScores(session, 0);
        Annotation[] a1 = convergeScores(0, scores);
        scores = genScores(session, 1);
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

    private float[] genScores(Session session, int id) {
        URL url;
        HttpURLConnection con;
        try {
            url = new URL(getString(R.string.myapi));
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            OutputStream os = con.getOutputStream();
            int ptr = 0;
            if(id == 0) {
                while(ptr < session.sizeOne() &&
                        session.getAnnotationFirst(ptr).getRTag()==Annotation.OBJECT_TAG) {
                    ++ptr;
                }
            } else {
                while(ptr < session.sizeTwo() &&
                        session.getAnnotationSecond(ptr).getRTag()==Annotation.OBJECT_TAG) {
                    ++ptr;
                }
            }
            String str = String.format("{\"qs\":\"%s\", \"obj\":\"%s\", \"txt\":\"%s\"}",
                    keyword, objString(session, ptr, id), txtString(session, ptr, id));
            byte[] write = str.getBytes(StandardCharsets.UTF_8);
            os.write(write);
            os.flush();
            os.close();
            float[] result = null;
            con.connect();
            if(con.getResponseCode() == 200) {
                InputStream instr = con.getInputStream();
                result = readFloatArray(, instr);
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

    // each session object a description tab-delimited.
    private String objString(Session session, int stop, int id) {
        StringBuilder sb = new StringBuilder();
        if(id == 0) {
            sb.append(stop); sb.append('\n');
            for (int i = 0; i<stop; ++i) {
                Annotation a = session.getAnnotationFirst(i);
                numObjScores += a.getExtraCount();
                sb.append(a.getExtraCount()); sb.append('\n');
                sb.append(a.getDescription()); sb.append('\n');
            }
        } else {
            sb.append(stop); sb.append('\n');
            for (int i = 0; i<stop; ++i) {
                Annotation a = session.getAnnotationSecond(i);
                numTxtScores
                sb.append(a.getDescription()); sb.append('\n');
            }
        }
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    private String txtString(Session session, int start, int id) {
        StringBuilder sb = new StringBuilder();
        if(id == 0) {
            sb.append(session.sizeOne() - start);
            sb.append('\t');
            for (int i = start; i<session.sizeOne(); ++i) {
                sb.append(session.getAnnotationFirst(i).getDescription());
                sb.append('\t');
            }
        } else {
            sb.append(session.sizeTwo() - start);
            sb.append('\t');
            for (int i = start; i<session.sizeTwo(); ++i) {
                sb.append(session.getAnnotationSecond(i).getDescription());
                sb.append('\t');
            }
        }
        sb.deleteCharAt(sb.length()-1);
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