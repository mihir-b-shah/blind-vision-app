package com.example.myfirstapp;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.PriorityQueue;

public class ConvgAnnot extends AppCompatActivity {

    private Session session;
    private String keyword;
    private static final int NUM_ANNOT = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_convg_annot);

        keyword = getIntent().getStringExtra("query");
        session = (Session) getIntent().getSerializableExtra("session");
        converge(session, keyword);
    }

    private class Pair implements Comparable<Pair> {
        final Annotation sess;
        final float conf;

        public Pair(Annotation s, float c) {
            sess = s;
            conf = c;
        }

        @Override
        public int compareTo(Pair other) {
            return Float.compare(other.conf, conf);
        }
    }

    public Annotation converge(Session session, String keyword) {
        final int size = session.size();
        String[] buffer = new String[size+1];
        buffer[0] = keyword;
        for(int i = 0; i<size;) {
            buffer[i+1] = session.get_annotation(i++).d;
        }
        new TwinwordCall().execute(buffer);
        return null;
    }

    private class TwinwordCall extends AsyncTask<String, Void, float[]> {

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
            ByteBuffer buffer = new ByteBuffer();
            outer: while(true) {
                switch (buf = (byte) instr.read()) {
                    case '[':
                    case ' ': break;
                    case ',': floats[ctr++] = Float.parseFloat(new String(buffer.getBuffer()));
                        buffer.clear(); break;
                    case ']': floats[ctr++] = Float.parseFloat(new String(buffer.getBuffer()));
                        buffer.clear(); break outer;
                    default: buffer.add(buf);
                }
            }
            return floats;
        }

        @Override
        protected float[] doInBackground(String... strings) {
            URL url;
            HttpURLConnection con;
            try {
                url = new URL("http://mudhaniu.pythonanywhere.com");
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                OutputStream os = con.getOutputStream();
                String str = String.format("{\"w1\":\"%s\", \"w2\":\"%s\"}", arrayString(strings),
                        strings[0]);
                System.out.println(str);
                byte[] write = str.getBytes(StandardCharsets.UTF_8);
                os.write(write);
                os.flush();
                os.close();
                float[] result = null;
                con.connect();
                if(con.getResponseCode() == 200) {
                    InputStream instr = con.getInputStream();
                    result = readFloatArray(strings.length-1, instr);
                    System.out.println(Arrays.toString(result));
                } else {
                    System.out.println(con.getResponseCode());
                    System.out.println("Request did not go through correctly.");
                }
                return result;
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(float[] f) {
            PriorityQueue<Pair> pq = new PriorityQueue<>();
            for(int i = 0; i<f.length; ++i) {
                pq.offer(new Pair(session.get_annotation(i), f[i]));
            }
            Annotation[] result = new Annotation[NUM_ANNOT];
            if(NUM_ANNOT > pq.size()) {
                System.err.println("NUM_ANNOT too big. Fatal exception occurred.");
                ConvgAnnot.this.setResult(Activity.RESULT_CANCELED);
                ConvgAnnot.this.finish();
            }

            for(int i = 0; i<NUM_ANNOT; ++i) {
                Annotation a = pq.poll().sess;
                a.match = f[i];
                result[i] = a;
            }
            session.setAnnotation(result);
            ConvgAnnot.this.setResult(Activity.RESULT_OK);
            ConvgAnnot.this.finish();
        }
    }
}
