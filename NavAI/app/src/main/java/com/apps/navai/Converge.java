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
import java.util.Arrays;
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
    private int numObjects;

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

    public void converge() {
        session.sort((a1,a2)->a1.getRTag()-a2.getRTag());
        float[] scores = genScores(session, 0);
        Annotation[] a1 = convergeScores(0, scores);
        scores = genScores(session, 1);
        Annotation[] a2 = convergeScores(1, scores);
        done(a1, a2);
    }

    private Annotation[] convergeScores(int id, float[] f) {
        // map the floats to the annotations.
        int ptr = 0;
        for(int i = 0; i<numObjects; ++i) {
            Annotation annotation = id == 0 ?
                    session.getAnnotationFirst(i) : session.getAnnotationSecond(i);
            int ct = annotation.getExtraCount();
            annotation.dotScores(f, ptr, ptr+ct);
            ptr += ct;
        }

        for(int i = numObjects; i<(id == 0 ? session.sizeOne() : session.sizeTwo()); ++i) {
            Annotation annotation = id == 0 ?
                    session.getAnnotationFirst(i) : session.getAnnotationSecond(i);
            if(Math.abs(f[ptr+1] - 1_000_000_000) < 0.01) {
                annotation.multConf(f[ptr]);
            } else {
                annotation.multConf(Math.max(f[ptr], f[ptr+1]));
            }
            ptr += 2;
        }

        PriorityQueue<Annotation> pq = new PriorityQueue<>();
        System.out.println(Arrays.toString(f));
        if(id == 0) {
            final int size = session.sizeOne();
            for(int i = 0; i<session.sizeOne(); ++i) {
                pq.offer(session.getAnnotationFirst(i));
            }
        } else {
            final int size = session.sizeTwo();
            for(int i = 0; i<session.sizeTwo(); ++i) {
                pq.offer(session.getAnnotationSecond(i));
            }
        }
        
        Annotation[] result = new Annotation[NUM_ANNOT];
        if(NUM_ANNOT > pq.size()) {
            Converge.this.stopSelf();
            System.err.println("NUM_ANNOT too big. Fatal exception occurred.");

        } else {
            for (int i = 0; i < NUM_ANNOT; ++i) {
                result[i] = pq.poll();
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
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            OutputStream os = con.getOutputStream();
            int ptr = 0;

            if(id == 0) {
                while(ptr < session.sizeOne() &&
                        session.getAnnotationFirst(ptr).getRTag()==Annotation.OBJECT_TAG) {
                    numObjScores += session.getAnnotationFirst(ptr).getExtraCount();
                    ++ptr;
                }
            } else {
                while(ptr < session.sizeTwo() &&
                        session.getAnnotationSecond(ptr).getRTag()==Annotation.OBJECT_TAG) {
                    numObjScores += session.getAnnotationFirst(ptr).getExtraCount();
                    ++ptr;
                }

            }
            numObjects = ptr;
            String str = String.format("{\"qs\":\"%s\", \"obj\":\"%s\", \"txt\":\"%s\"}",
                    keyword, objString(session, ptr, id), txtString(session, ptr, id));
            /*
            String str = "{\"qs\":\"candidate\", \"obj\":\"3\\t3\\tpolitics\\tflag\\tdemocrat\", " +
                    "\"txt\":\"2\\tnext door\\tnull\\tcandidate\\tnull\"}"; */
            System.out.println(str);
            byte[] write = str.getBytes(StandardCharsets.UTF_8);
            os.write(write);
            os.flush();
            os.close();
            float[] result = null;
            con.connect();
            if(con.getResponseCode() == 200) {
                InputStream instr = con.getInputStream();
                result = readFloatArray(numTxtScores+numObjScores, instr);
                System.out.println(Arrays.toString(result));
            } else {
                System.err.printf("%d, Request did not go through correctly.%n",
                        con.getResponseCode());
            }
            con.disconnect();
            return result;
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // each session object a description tab-delimited.
    private String objString(Session session, int stop, int id) {
        System.out.println("STOP VAL: " + stop);
        StringBuilder sb = new StringBuilder();
        if(id == 0) {
            sb.append(numObjScores); sb.append('\\'); sb.append('t');
            for (int i = 0; i<stop; ++i) {
                Annotation a = session.getAnnotationFirst(i);
                if(a.getRTag() != Annotation.OBJECT_TAG) {
                    --numObjects;
                    continue;
                }

                sb.append(a.getExtraCount()); sb.append('\\'); sb.append('t');
                sb.append(a.getDescription().toLowerCase().trim()); sb.append('\\'); sb.append('t');
            }
        } else {
            sb.append(stop); sb.append('\\'); sb.append('t');
            for (int i = 0; i<stop; ++i) {
                Annotation a = session.getAnnotationSecond(i);
                numObjScores += a.getExtraCount();
                sb.append(a.getExtraCount()); sb.append('\\'); sb.append('t');
                sb.append(a.getDescription().toLowerCase().trim()); sb.append('\\'); sb.append('t');
            }
        }

        return sb.toString().trim();
    }

    private String txtString(Session session, int start, int id) {
        StringBuilder sb = new StringBuilder();
        if(id == 0) {
            sb.append(session.sizeOne() - start);
            sb.append('\\'); sb.append('t');
            SpellCheck.loadDict(this);
            for (int i = start; i<session.sizeOne(); ++i) {
                String s = session.getAnnotationFirst(i).getDescription();
                final int tabIdx = s.indexOf("\\t");
                String orig = s.substring(0, tabIdx).toLowerCase().trim();
                String corr = s.substring(tabIdx).toLowerCase().trim();
                if(orig.indexOf(' ') != -1 && corr.indexOf(' ') == -1) {
                    s = String.format("%s\\t%s", orig, corr = SpellCheck.findSpaces(corr));
                    String res;
                    if((res = SpellCheck.condenseSpaces(corr)).equals(corr)) {
                        s = String.format("%s\\t%s", orig, res);
                    }
                }
                sb.append(s.toLowerCase().trim());
                numTxtScores += 2;
                sb.append('\\'); sb.append('t');
            }
            SpellCheck.freeDict();
        } else {
            sb.append(session.sizeTwo() - start);
            sb.append('\\'); sb.append('t');
            SpellCheck.loadDict(this);
            for (int i = start; i<session.sizeTwo(); ++i) {
                String s = session.getAnnotationSecond(i).getDescription();
                final int tabIdx = s.indexOf("\\t");
                String orig = s.substring(0, tabIdx).toLowerCase().trim();
                String corr = s.substring(tabIdx).toLowerCase().trim();
                if(orig.indexOf(' ') != -1 && corr.indexOf(' ') == -1) {
                    s = String.format("%s %s", orig, SpellCheck.findSpaces(corr));
                }
                sb.append(s);
                sb.append('\\'); sb.append('t');
            }
            SpellCheck.freeDict();
        }

        return sb.toString().trim();
    }

    private float[] readFloatArray(int N, InputStream instr) throws IOException {
        byte buf;
        float[] floats = new float[N];
        int ctr = 0;
        ByteArray buffer = new ByteArray();
        int level = 0;
        do {
            switch (buf = (byte) instr.read()) {
                case '[': ++level;
                case ' ': break;
                case ',':
                    if(buffer.isEmpty()) {
                        break;
                    }
                    String strBuf = new String(buffer.getBuffer());
                    floats[ctr] = Float.parseFloat(strBuf);
                    ++ctr;
                    buffer.clear();
                    break;
                case ']':
                    if(buffer.isEmpty()) {
                        --level;
                        break;
                    }
                    String strBuf2 = new String(buffer.getBuffer());
                    floats[ctr] = Float.parseFloat(strBuf2);
                    ++ctr;
                    --level;
                    buffer.clear();
                    break;
                default: buffer.add(buf);
            }
        } while(level > 0);
        return floats;
    }
}