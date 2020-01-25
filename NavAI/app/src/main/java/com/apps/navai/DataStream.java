package com.apps.navai;

import static java.lang.Math.*;

public class DataStream {
    private double threshold;

    private double noiseMean1;
    private double noiseMeanSQ1;
    private double noiseSD1;
    private int noiseCount1;

    private double noiseMean2;
    private double noiseMeanSQ2;
    private double noiseSD2;
    private int noiseCount2;

    private boolean real;

    private final SpeedListener listener;

    private static final double RSQRT_2 = 1/sqrt(2);
    private static final double EPSILON = 1e-5;

    private final DblStack stack1;
    private final DblStack stack2;
    private final ByteArray queue;
    private final CircularBuffer buffer1;
    private final CircularBuffer buffer2;

    private int recentStack;

    private class DblStack {
        private final double[] stack;
        private int pos;

        public DblStack(int capacity) {
            stack = new double[capacity];
        }

        public void push(double val) {
            stack[pos++] = val;
        }

        public void pop() {
            --pos;
        }

        public double top() {
            return stack[pos-1];
        }

        public double extract() {
            return stack[--pos];
        }

        public int size() {
            return pos;
        }

        public void setSize(int size) {pos = size;}

        public void clear() {
            pos = 0;
        }

        public double at(int i) {
            return stack[i];
        }

        public void set(int idx, double val) {stack[idx] = val;}

        public void move(int src, int dest) {
            stack[dest] = stack[src];
        }

        public boolean isNoise() {
            return stack[2] != -1 && abs(stack[4] - stack[2]) <= threshold;
        }

        public boolean corner() {
            if(abs(stack[2]+1)<EPSILON) {
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
        stack1 = new DblStack(5);
        stack1.push(0); stack1.push(-1);
        stack2 = new DblStack(5);
        stack2.push(0); stack2.push(-1);
        queue = new ByteArray(3);
        buffer1 = new CircularBuffer(64);
        buffer2 = new CircularBuffer(64);
        recentStack = 0;
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
            } else if((recentStack&2) == 0){
                stack1.push(res);
                if(real) {
                    pushVal(0);
                } else {
                    pushTrain(0);
                }
                ++recentStack;
            } else {
                stack2.push(res);
                if(real) {
                    pushVal(1);
                } else {
                    pushTrain(1);
                }
                ++recentStack;
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
            } else if((recentStack&2) == 0){
                stack1.push(res);
                if(real) {
                    pushVal(0);
                } else {
                    pushTrain(0);
                }
                ++recentStack;
            } else {
                stack2.push(res);
                if(real) {
                    pushVal(1);
                } else {
                    pushTrain(1);
                }
                ++recentStack;
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

    private void pushTrain(int id) {
        DblStack stack = id == 0 ? stack1: stack2;
        CircularBuffer buffer = id == 0 ? buffer1: buffer2;
        if((stack.size()&1) == 0) {
            buffer.write(stack.top());
            double val = abs(stack.top()-
                    (stack.at(1)==-1 ? stack.top() : stack.at(1)));
            stack.move(3, 1);
            stack.move(2, 0);
            stack.pop();
            stack.pop();
            if(id == 0) {
                noiseMean1 += val;
                noiseMeanSQ1 += val * val;
                ++noiseCount1;
            } else {
                noiseMean2 += val;
                noiseMeanSQ2 += val * val;
                ++noiseCount2;
            }
        }
    }

    private void noiseDone() {
        buffer1.setDominantFreq();
        buffer1.buildKernel();
        buffer2.setDominantFreq();
        buffer2.buildKernel();

        noiseMean1 /= noiseCount1-1;
        noiseMeanSQ1 /= noiseCount1-1;
        noiseSD1 = sqrt(noiseMeanSQ1-noiseMean1*noiseMean1);
        threshold = genThreshold();
        threshold *= noiseSD1;

        noiseMean2 /= noiseCount2-1;
        noiseMeanSQ2 /= noiseCount2-1;
        noiseSD2 = sqrt(noiseMeanSQ2-noiseMean2*noiseMean2);
        threshold *= noiseSD2;

        double temp = stack1.at(1);
        stack1.move(0, 1);
        stack1.set(0, temp);
        stack1.set(2, -1); // set up for {mean, time, mean}
        stack1.setSize(3); // should be init off by 1
        temp = stack2.at(1);
        stack2.move(0, 1);
        stack2.set(0, temp);
        stack2.set(2, -1); // set up for {mean, time, mean}
        stack2.setSize(3); // should be init off by 1
    }

    private void pushVal(int id) {
        DblStack stack = id == 0 ? stack1: stack2;
        CircularBuffer buffer = id == 0 ? buffer1: buffer2;

        boolean noise;
        if((stack.size() != stack.capacity())) {
            return;
        } else {
            stack.set(4, buffer.lsqFilter());
            if(!(noise = stack.isNoise()) && stack.corner()) {
                stack.move(2, 0);
                stack.move(4, 2);
                listener.speedChanged(id, (stack.at(3)-stack.at(1))/1000d);
                stack.move(3, 1);
            } else if(!noise) {
                stack.move(4, 2);
            }
        }

        buffer.write(stack.top());
        stack.pop();
        stack.pop();
    }

    @FunctionalInterface
    interface SpeedListener {
        void speedChanged(int id, double time);
    }
}