package com.example.myfirstapp;

import static java.lang.Math.*;

public class DataStream {
    private int slope; // last nonzero slope
    private int curr;
    private int timeLast;

    private float threshold;
    private float zError;

    private float noiseMean;
    private float noiseMeanSQ;
    private float noiseSD;
    private int noiseCount;

    private final SpeedListener listener;

    private static final float PI_SQRT = (float) sqrt(2*PI);

    public DataStream(float thr, SpeedListener listener) {
        curr = -1;
        threshold = thr;
        this.listener = listener;
    }

    private float erfIntegral(float upper) {
        return 0.5f + erfIntegralHelper(abs(upper));
    }

    // this erf is on (0,1) instead of (-1,1)
    private float erfIntegralHelper(float upper) {
        float sum = 0;
        float term = 1;
        for(int i = 1; term>zError; ++i) {
            term *= upper*upper/(4*i)*(1-2/(2f*i+1));
            sum += term;
        }
        return sum;
    }

    private float genThreshold() {
        // uses binary search the answer to efficiently compute boundary point
        float lower = 0f;
        float upper = 1024f;
        float mid = 0f;
        float res;

        while(upper-lower>zError) {
            mid = (lower+upper)/2;
            res = erfIntegral(mid);
            if(res > threshold) {
                upper = mid;
            } else {
                lower = mid;
            }
        }

        return mid;
    }

    public static int parseBytes(final byte[] bytes, int LIM) {
        int iter = 0;
        int val = 0;
        while(iter < bytes.length && bytes[iter] != 0) {
            val *= 10;
            val += bytes[iter]-'0';
        }
        return val;
    }

    public void pushTrain(int time, final byte[] bytes, int LIM) {
        int val = parseBytes(bytes, LIM);
        noiseMean += val;
        noiseMeanSQ += val*val;
        ++noiseCount;
        timeLast = time;
    }

    public void noiseDone() {
        noiseMean /= noiseCount;
        noiseMeanSQ /= noiseCount;
        noiseSD = (float) sqrt(noiseMeanSQ-noiseMean*noiseMean);
        zError = threshold/noiseSD*PI_SQRT;
        threshold = genThreshold();
    }

    public void pushVal(int time, final byte[] bytes, int LIM) {
        int val = parseBytes(bytes, LIM);
        if(!isNoise(val)) return;
        else {
            if (signum(val - curr) == -signum(slope)) {
                int diff = time-timeLast;
                timeLast = time;
                listener.speedChanged((float) PI/diff);
            }
            int aux = val-curr;
            slope = aux == 0 ? slope : aux;
            curr = val;
        }
    }

    private boolean isNoise(int val) {
        return val-curr < threshold;
    }

    @FunctionalInterface
    public interface SpeedListener {
        void speedChanged(float omega);
    }
}
