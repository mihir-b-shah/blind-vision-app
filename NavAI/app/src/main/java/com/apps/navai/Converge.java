package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Rect;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import static com.apps.navai.MainActivity.INT_1;
import static com.apps.navai.MainActivity.STRING_1;
import static com.apps.navai.MainActivity.STRING_2;

public class Converge extends IntentService {

    private Session session;
    private String keyword;
    private int id;

    private int numObjScores;
    private int numTxtScores;
    private int numObjects;

    private static final int EPS_X = CustomCamera.CAMERA_WIDTH/4;
    private static final int EPS_Y = CustomCamera.CAMERA_HEIGHT/4;

    public Converge() {
        super("Converge");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        id = intent.getIntExtra(INT_1,-1);
        keyword = intent.getStringExtra(STRING_1);
        session = (Session) intent.getSerializableExtra(STRING_2);
        System.out.println("Convergence input:");
        session.display();
        converge();
    }

    public void converge() {
        session.sortReg((a1,a2)->a1.getRTag()-a2.getRTag());
        float[] scores = genScores(session, 0);
        convergeScores(0, scores);
        scores = genScores(session, 1);
        convergeScores(1, scores);
        identifyPair();
        done();
    }

    private void identifyPair() {
        System.out.println("FOR REAL:");
        session.display();
        URL url;
        HttpURLConnection con;
        try {
            url = new URL(getString(R.string.myapi)+"matrix");
            con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            OutputStream os = con.getOutputStream();

            String str = String.format("{\"one\":\"%s\", \"two\":\"%s\"}",
                    formatDescriptions(0), formatDescriptions(1));

            byte[] write = str.getBytes(StandardCharsets.UTF_8);
            os.write(write);
            os.flush();
            os.close();
            final float[] result;
            con.connect();
            if(con.getResponseCode() == 200) {
                InputStream instr = con.getInputStream();
                result = readFloatArray(session.sizeOne()*session.sizeTwo(), instr);
            } else {
                System.err.printf("%d, Request did not go through correctly.%n",
                        con.getResponseCode());
                con.disconnect();
                return;
            }
            con.disconnect();

            final int sizeOne = session.sizeOne();
            final int sizeTwo = session.sizeTwo();
            long[] data = new long[sizeOne*sizeTwo];

            for(int i = 0; i<sizeOne; ++i) {
                for(int j = 0; j<sizeTwo; ++j) {
                    data[i*sizeTwo+j] = (i << 12) + j;
                }
            }

            Comparator<Long> comp = (v1, v2)->{
                final int idx11 = (int) (v1 >>> 12);
                final int idx12 = (int) (v1 & 0xfff);
                final int idx21 = (int) (v2 >>> 12);
                final int idx22 = (int) (v2 & 0xfff);

                final Annotation a11 = session.getAnnotationFirst(idx11);
                final Annotation a12 = session.getAnnotationSecond(idx12);
                final Annotation a21 = session.getAnnotationFirst(idx21);
                final Annotation a22 = session.getAnnotationSecond(idx22);

                final float sc1 = result[idx11*session.sizeTwo()+idx12]
                        *a11.getConfidence()*a12.getConfidence();
                final float sc2 = result[idx21*session.sizeTwo()+idx22]
                        *a21.getConfidence()*a22.getConfidence();

                return Float.compare(sc2,sc1);
            };

            Long[] boxedData = Arrays.stream(data).boxed().toArray(Long[]::new);
            Arrays.sort(boxedData, comp);
            data = Arrays.stream(boxedData).mapToLong(x->x).toArray();
            Annotation a1 = null;
            Annotation a2 = null;

            int ptr = 0;
            while(ptr < data.length) {
                final int idx1 = (int) (data[ptr] >>> 12);
                final int idx2 = (int) (data[ptr] & 0xfff);

                final Annotation an1 = session.getAnnotationFirst(idx1);
                final Annotation an2 = session.getAnnotationSecond(idx2);

                if(enoughOverlap(an1.getRect(), an2.getRect())) {
                    a1 = an1;
                    a2 = an2;
                    break;
                }

                ++ptr;
            }

            System.out.println("CONVERGED ANNOTATIONS: " + a1 + " " + a2);
            Annotation[] annot1 = {a1};
            Annotation[] annot2 = {a2};
            session.setAnnotationsOne(annot1);
            session.setAnnotationsTwo(annot2);

        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private final boolean enoughOverlap(Rect r1, Rect r2) {
        return !(r1.right < r2.left && r2.left-r1.right>EPS_X || r1.left < r2.right
                && r2.right-r1.left>EPS_X) && !(r1.bottom < r2.top && r2.top-r1.bottom>EPS_Y ||
                r1.top < r2.bottom && r2.bottom-r1.top>EPS_Y);
    }

    private String formatDescriptions(int id) {
        StringBuilder sb = new StringBuilder();
        boolean run = false;
        if(id == 0) {
            final int size = session.sizeOne();
            for(int i = 0; i<size; ++i) {
                run = true;
                sb.append(session.getAnnotationFirst(i).getDescription());
                sb.append('\\'); sb.append('t');
            }
        } else {
            final int size = session.sizeTwo();
            for(int i = 0; i<size; ++i) {
                run = true;
                sb.append(session.getAnnotationSecond(i).getDescription());
                sb.append('\\'); sb.append('t');
            }
        }
        return run ? sb.substring(0, sb.length()-2).trim() : sb.toString();
    }

    private void convergeScores(int id, float[] f) {
        // map the floats to the annotations.
        int ptr = 0;
        for(int i = 0; i<numObjects; ++i) {
            Annotation annotation = id == 0 ?
                    session.getAnnotationFirst(i) : session.getAnnotationSecond(i);
            int ct = annotation.getExtraCount();
            annotation.dotScores(f, ptr, ptr+ct);
            ptr += ct;
        }

        System.out.println("AFTER DOT SCORES!");
        session.display();

        for(int i = numObjects; i<(id == 0 ? session.sizeOne() : session.sizeTwo()); ++i) {
            Annotation annotation = id == 0 ?
                    session.getAnnotationFirst(i) : session.getAnnotationSecond(i);
            if(Math.abs(f[ptr+1] - 1_000_000_000) < 0.01 || f[ptr] >= f[ptr+1]) {
                annotation.multConfDecide(f[ptr], true);
            } else {
                annotation.multConfDecide(f[ptr+1], false);
            }
            ptr += 2;
        }

        System.out.println("SESSION DONE!");
        session.display();
    }

    private void done() {
        Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
        out.putExtra("session", session);
        out.putExtra(INT_1, id);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(out);
        Converge.this.stopSelf();
    }

    private float[] genScores(Session session, int id) {
        numObjScores = 0;
        numTxtScores = 0;
        numObjects = 0;

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
                    numObjScores += session.getAnnotationSecond(ptr).getExtraCount();
                    ++ptr;
                }

            }
            numObjects = ptr;
            String str = String.format("{\"qs\":\"%s\", \"obj\":\"%s\", \"txt\":\"%s\"}",
                    keyword.toLowerCase(), objString(session, ptr, id), txtString(session, ptr, id));
            byte[] write = str.getBytes(StandardCharsets.UTF_8);
            os.write(write);
            os.flush();
            os.close();
            float[] result = null;
            con.connect();
            if(con.getResponseCode() == 200) {
                InputStream instr = con.getInputStream();
                result = readFloatArray(numTxtScores+numObjScores, instr);
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
            return sb.substring(0, sb.length()-2).trim();
        } else {
            sb.append(stop); sb.append('\\'); sb.append('t');
            for (int i = 0; i<stop; ++i) {
                Annotation a = session.getAnnotationSecond(i);
                numObjScores += a.getExtraCount();
                sb.append(a.getExtraCount()); sb.append('\\'); sb.append('t');
                sb.append(a.getDescription().toLowerCase().trim()); sb.append('\\'); sb.append('t');
            }
            return sb.substring(0, sb.length()-2).trim();
        }
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
                String corr = s.substring(tabIdx+2).toLowerCase().trim();
                System.out.printf("TEXT CORR: orig: %s, corr: %s%n", orig, corr);

                if(orig.indexOf(' ') == -1 && corr.indexOf(' ') != -1) {
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
                numTxtScores += 2;
                sb.append('\\'); sb.append('t');
            }
            SpellCheck.freeDict();
        }
        return sb.substring(0, sb.length()-2).trim();
    }

    private float[] readFloatArray(int N, InputStream instr) throws IOException {
        System.out.println("N: " + N);
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