package com.apps.navai;

import static java.lang.Math.*;

public class DataStream {
    private double threshold;

    private double noiseMean;
    private double noiseMeanSQ;
    private double noiseSD;
    private int noiseCount;
    private boolean real;

    private final SpeedListener listener;

    private static final double RSQRT_2 = 1/sqrt(2);
    private static final double EPSILON = 1e-5;
    private static final double SAMPLING_FREQ = 0.1;

    private final IntStack stack;
    private final ByteBuffer queue;
    private final CircularBuffer buffer;

    private class IntStack {
        private final int[] stack;
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

        public boolean isNoise() {
            return stack[2] == -1 ? false : abs(stack[4]-stack[2]) <= threshold;
        }

        public boolean corner() {
            if(stack[2] == -1) {
                return false;
            } else return signum(stack[4]-stack[2]) == -signum(stack[2]-stack[0]);
        }

        public int capacity() {
            return stack.length;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i<pos; ++i) {
                sb.append(stack[i]);
                sb.append(' ');
            }
            return sb.toString();
        }
    }

    public DataStream(double thr, SpeedListener listener) {
        threshold = thr;
        this.listener = listener;
        stack = new IntStack(5);
        stack.push(0); stack.push(-1);
        queue = new ByteBuffer(3);
        buffer = new CircularBuffer(64);
    }

    // pade approximant from https://math.stackexchange.com/questions/1312418/
    private double erfIntegral(double x) {
        x *= RSQRT_2;
        double sq = x*x;
        return (1+signum(x)*sqrt(1-exp(-(0.0167527*sq*sq+0.160257*sq+1.27324)/
                (0.0151778*sq*sq+0.155912*sq+1)*sq)))/2;
    }

    private double genThreshold() {
        // uses binary search the answer to efficiently compute boundary point
        double lower = 0f;
        double upper = 1024f;
        double mid = 0f;
        double res;
        while(upper-lower>EPSILON) {
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

    private int parseBytes(final byte[] bytes, int OFFSET) {
        if(bytes[OFFSET]==-1) {
            return -1;
        } return bytes[OFFSET] + (bytes[OFFSET+1] << 7) + (bytes[OFFSET+2] << 14);
    }

    private int parseQueueBytes(final byte[] bytes) {
        byte[] buf = queue.getBuffer();
        if(buf[0] == -1)
            return -1;
        final int lim = queue.size();
        switch(lim) {
            case 1:
                return buf[0] + (bytes[0] << 7) + (bytes[1] << 14);
            case 2:
                return buf[0] + (buf[1] << 7) + (bytes[0] << 14);
            default:
                return -5; // error;
        }
    }

    public void enqueue(final byte[] buffer, int size) {
        if(size + queue.size() < 3) {
            // queue the data and return
            for(int i = 0; i<size; ++i) {
                queue.add(buffer[i]);
            }
            return;
        }
        if(queue.size() != 0) {
            int res = parseQueueBytes(buffer);
            if(res == -1) {
                real = true;
                noiseDone();
            } else {
                stack.push(res);
            }
            if(real) {
                pushVal();
            } else {
                pushTrain();
            }
        }

        final int loopLen = size-3;
        final int loopStart = (3-queue.size())%3;
        // qs = 0, i = 0. qs = 1, i = 2, qs = 2, i = 1
        for(int i = loopStart; i<=loopLen; i+=3) {
            int res = parseBytes(buffer, i);
            if(res == -1) {
                real = !real; // in case of second switch
                noiseDone();
            } else {
                stack.push(res);
                if(real) {
                    pushVal();
                } else {
                    pushTrain();
                }
            }
        }
        // queue the remainder
        int remainder = (queue.size()+size)%3;
        if(remainder != 0) {
            queue.enqueue(buffer, size-remainder, remainder);
        } else {
            queue.contract(0);
        }
    }

    private void pushTrain() {
        if((stack.size()&1) == 0) {
            buffer.write(stack.top());
            int val = abs(stack.top()-
                    (stack.at(1)==-1 ? stack.top() : stack.at(1)));
            stack.move(3, 1);
            stack.move(2, 0);
            stack.pop();
            stack.pop();
            noiseMean += val;
            noiseMeanSQ += val * val;
            ++noiseCount;
        }
    }

    private void noiseDone() {
        double noiseFreq = buffer.dominantFreq()*SAMPLING_FREQ;
        System.out.println(noiseFreq);
        noiseMean /= noiseCount-1;
        noiseMeanSQ /= noiseCount-1;
        noiseSD = sqrt(noiseMeanSQ-noiseMean*noiseMean);
        threshold = genThreshold();
        threshold *= noiseSD;
        int temp = stack.at(1);
        stack.move(0, 1);
        stack.set(0, temp);
        stack.set(2, -1); // set up for {mean, time, mean}
        stack.setSize(3); // should be init off by 1
    }

    private void pushVal() {
        boolean noise;
        if((stack.size() != stack.capacity())) {
            return;
        } else if(!(noise = stack.isNoise()) && stack.corner()) {
            stack.move(2, 0);
            stack.move(4, 2);
            listener.speedChanged(PI, (stack.at(3)-stack.at(1))/1000d);
            stack.move(3, 1);
        } else if(!noise) {
            stack.move(4, 2);
        }

        stack.pop();
        stack.pop();
    }

    @FunctionalInterface
    public interface SpeedListener {
        void speedChanged(double theta, double time);
    }
}