package com.apps.navai;

import android.app.IntentService;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.SizeF;

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

    private class FloatPair {
        private final float x;
        private final float y;

        public FloatPair(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }
        public float getY() {
            return y;
        }
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

    public float calcDist(Calibrate.DirVector first, Calibrate.DirVector second) {
        return 0.0f;
    }

    public float getVerticalAngle(Annotation annot) {
        float VERTICAL_SIZE = 1920; // update
        float vertPixels = annot.getRect().exactCenterY();
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            CameraCharacteristics props = manager.getCameraCharacteristics(getNormCamera(manager));
            float[] focals = props.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            float focLength = focals[0];
            SizeF size = props.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
            float vertHalfView = 2*(float) Math.atan(size.getHeight()/focLength);
            float pixelDist = 0.5f-vertPixels/(VERTICAL_SIZE/2);
            return (float) Math.atan(pixelDist*Math.tan(vertHalfView));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return Float.NaN;
    }

    public FloatPair correct(float x, float y) {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            float xCorr,yCorr;
            float[] K = manager.getCameraCharacteristics(getNormCamera(manager))
                    .get(CameraCharacteristics.LENS_RADIAL_DISTORTION);
            if(K == null) return null;
            float r = (float) Math.hypot(x-0.5,y-0.5);
            float f1 = 1f;
            float rpow = 1; final float rsq = r*r;
            for(int i = 0; i<3; ++i) {
                rpow *= r*r;
                f1 += K[i]*rpow;
            }
            float f2 = 2*x*y;
            xCorr = x*f1+K[3]*f2+K[4]*(rsq+2*x*x);
            yCorr = y*f1+K[4]*f2+K[3]*(rsq+2*y*y);
            return new FloatPair(xCorr, yCorr);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getNormCamera(CameraManager cManager) {
        try {
            for(final String cameraId : cManager.getCameraIdList()){
                CameraCharacteristics ch = cManager.getCameraCharacteristics(cameraId);
                Integer val = ch.get(CameraCharacteristics.LENS_FACING);
                if(val != null && val == CameraCharacteristics.LENS_FACING_BACK) return cameraId;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void converge() {
        final int size = session.size();
        String[] buffer = new String[size+1];
        buffer[0] = keyword;
        for(int i = 0; i<size; ++i) {
            buffer[i+1] = session.getAnnotation(i).getDescription();
        }
        float[] scores = genScores(buffer);
        convergeScores(scores);
    }

    private void convergeScores(float[] f) {
        PriorityQueue<Pair> pq = new PriorityQueue<>();
        for(int i = 0; i<f.length; ++i) {
            pq.offer(new Pair(session.getAnnotation(i), f[i]));
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
            Intent out = new Intent(MainActivity.SERVICE_RESPONSE);
            session.setAnnotation(result);
            out.putExtra("session", session);
            out.putExtra(INT_1, id);
            LocalBroadcastManager.getInstance(getApplicationContext())
                    .sendBroadcast(out);
            Converge.this.stopSelf();
        }
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
        ByteBuffer buffer = new ByteBuffer();
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