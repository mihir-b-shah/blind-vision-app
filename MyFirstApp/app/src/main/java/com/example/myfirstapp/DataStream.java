package com.example.myfirstapp;

import android.util.Log;
import java.io.IOException;
import static java.lang.Math.*;

public class DataStream {
    private float threshold;

    private float noiseMean;
    private float noiseMeanSQ;
    private float noiseSD;
    private int noiseCount;
    private boolean real;

    private final SpeedListener listener;
    private static final float PI_SQRT = (float) sqrt(2*PI);
    private final IntStack stack;

    private class IntStack {
        private int[] stack;
        private int pos;

        public IntStack(int capacity) {
            stack = new int[capacity];
        }

        public void push(int val) {
            stack[pos++] = val;
        }

        public void pop() {
            --pos;
        }

        public int top() {
            return stack[pos-1];
        }

        public int extract() {
            return stack[--pos];
        }

        public int size() {
            return pos;
        }

        public void setSize(int size) {pos = size;}

        public void clear() {
            pos = 0;
        }

        public int at(int i) {
            return stack[i];
        }

        public void set(int idx, int val) {stack[idx] = val;}

        public void move(int src, int dest) {
            stack[dest] = stack[src];
        }

        public byte compare(int src, int dest) {
            return (byte) (Integer.compare(stack[src], stack[dest]));
        }

        public boolean isNoise() {
            return abs(stack[4]-stack[2]) <= threshold;
        }

        public boolean corner() {
            return signum(stack[4]-stack[2]) == -signum(stack[2]-stack[0]);
        }

        public int capacity() {
            return stack.length;
        }
    }

    public DataStream(float thr, SpeedListener listener) {
        threshold = thr;
        this.listener = listener;
        stack = new IntStack(5);
        stack.push(0);
    }

    private float erfIntegral(float upper) {
        return 0.5f + erfIntegralHelper(abs(upper));
    }

    // this erf is on (0,1) instead of (-1,1)
    private float erfIntegralHelper(float upper) {
        float sum = 0;
        float term = 1;
        for(int i = 1; term>threshold; ++i) {
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

        while(upper-lower>threshold) {
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

    private static int parseBytes(final byte[] bytes, int OFFSET, int LIM) throws IOException {
        if(BuildConfig.DEBUG && LIM != 3) {
            throw new IOException("Byte length not 3.");
        }
        if(bytes[OFFSET]==-1) {
            return -1;
        } return bytes[OFFSET] + (bytes[OFFSET+1] << 7) + (bytes[OFFSET+2] << 14);
    }

    public void enqueue(final byte[] buffer, int size) throws IOException {
        Log.v("Method", String.format("%d,%d",buffer.length,size));
        for(int i = 0; i<size; i+=3) {
            int res = parseBytes(buffer, i, 3);
            Log.v("Received", Integer.toString(res));
            /*
            if(res == -1) {
                real = true;
                noiseDone();
            } else {
                stack.push(res);
            }

        }
        if(real) {
            pushVal();
        } else {
            pushTrain(); */
        }
    }

    private void pushTrain() {
        if((stack.size()&2) != 0) {
            int val = stack.extract();
            stack.move(stack.size()-1, 0);
            stack.pop();
            noiseMean += val;
            noiseMeanSQ += val * val;
            ++noiseCount;
        }
    }

    private void noiseDone() {
        noiseMean /= noiseCount;
        noiseMeanSQ /= noiseCount;
        noiseSD = (float) sqrt(noiseMeanSQ-noiseMean*noiseMean);
        threshold /= noiseSD*PI_SQRT;
        threshold = genThreshold();
        stack.move(0,1);
        stack.set(0, (int) noiseMean);
        stack.set(2, (int) noiseMean+1); // set up for {mean, time, mean}
    }

    private void pushVal() {
        if((stack.size() != stack.capacity())) {
        } else if(stack.isNoise() || !stack.corner()) {
            stack.pop();
            stack.pop();
        } else {
            stack.move(2,0);
            stack.move(4,2);
            listener.speedChanged((float) PI, (stack.at(3)-stack.at(1))/1000);
            stack.move(1,3);
            stack.pop();
            stack.pop();
        }
    }

    @FunctionalInterface
    public interface SpeedListener {
        void speedChanged(float theta, float time);
    }
}
