
package com.apps.navai;

public class LongSet {
    private final long[] data;
    private final int MASK;
    private final int SHIFT;

    public LongSet(int N) {
        data = new long[N];
        MASK = N;
        SHIFT = Integer.numberOfTrailingZeros(N+1);
    }

    public void insert(final long val) {
        int iter;
        iter = (int) adjustIndex(val);
        int skip = 0;
        while ((data[iter]) != 0L) {
            iter += ++skip*skip;
            iter = (iter+1 >>> SHIFT) + (iter+1 & MASK) - 1;
        }
        data[iter] = val;
    }

    public boolean contains(final long val) {
        int iter = (int) adjustIndex(val);
        int skip = 0;
        while(data[iter] != 0L) {
            if(data[iter] == val) return true;
            iter += ++skip*skip;
            iter = (iter+1 >>> SHIFT) + (iter+1 & MASK) - 1;
        }
        return false;
    }

    private final long adjustIndex(long val) {
        long hash = val % data.length;
        return hash < 0 ? hash + data.length : hash;
    }
}