package com.apps.navai;

import static java.lang.Math.*;
import java.nio.BufferOverflowException;
import java.util.Arrays;

public class CircularBuffer {
    private static final int MAX_SIZE = 0x8000;
    private final int[] data;
    private final double[] freqs;
    private final double[] trigCache;

    private final int N;
    private final int LOGN;
    private final int MASK;
    private int writePtr;

    public CircularBuffer(int N) {
        if(N > MAX_SIZE)
            throw new BufferOverflowException();
        final int NBSL = N<<1;
        this.N = N;
        LOGN = Integer.numberOfTrailingZeros(N);
        data = new int[NBSL];
        MASK = NBSL-1;
        freqs = new double[NBSL];
        trigCache = new double[N];

        final int NBS = N >>> 1;
        for(int i = 0; i< NBS; ++i) {
            int IBS = i << 1;
            trigCache[IBS] = cos(i*PI/NBS);
            trigCache[1+IBS] = -sqrt(1-pow(trigCache[IBS],2));
        }
    }

    public void write(int val) {
        data[writePtr++ & MASK] = val;
        data[writePtr++ & MASK] = 0;
    }

    public double dominantFreq() {
        fft(0, 0);
        System.out.println(Arrays.toString(freqs));
        int IBS;
        final int LEN = 1+(N >>> 1);
        int idx = 1;
        double max = pow(freqs[2],2)+pow(freqs[3],2);

        for(int i = 2; i<LEN; ++i) {
            IBS = i<<1;
            double curr = pow(freqs[IBS],2) + pow(freqs[IBS+1],2);
            if(curr > max) {
                max = curr;
                idx = i;
            }
        }

        int OBS = 32-LOGN;
        return (double) N/(idx << OBS >> OBS);
    }

    private void fft(int start, int lvl) {
        int n = N >>> lvl;
        int skip = 1 << lvl;

        int rev = reverse(lvl, start);

        if (n == 1) {
            int RBS = rev << 1; int SBS = start << 1;
            freqs[RBS] = data[(writePtr + SBS) & MASK];
            freqs[1+RBS] = data[(1 + writePtr + SBS) & MASK];
            return;
        }

        fft(start, lvl+1);
        fft(start+skip, lvl+1);

        int REVBS = rev << (LOGN-lvl);
        int AGGBS = REVBS + (n >>> 1);

        for (int k = 0; k < n >>> 1; ++k) {
            int pos = k << lvl+1;
            writePlusMinus(trigCache[pos], trigCache[1+pos],
                    (k+AGGBS) << 1, (k+REVBS) << 1);
        }
    }

    // inline this hopefully?
    private final int reverse(int len, int bits) {
        bits = (bits & 0x5555) << 1 | (bits & 0xAAAA) >>> 1;
        bits = (bits & 0x3333) << 2 | (bits & 0xCCCC) >>> 2;
        bits = (bits & 0x0F0F) << 4 | (bits & 0xF0F0) >>> 4;
        bits = (bits & 0x00FF) << 8 | (bits & 0xFF00) >>> 8;
        return bits >>> 16-len;
    }

    /**
     * Does the +/- computation needed for the FFT in place.
     * Operations:
     *
     * 1. Multiply the complex number defined by <param>reScl</param> and
     * <param>imScl</param> against the one located in the output array at
     * <param> locMult.
     * 2. Add this result against the complex number located at <param> locAdd
     * </param> and store it in locMult. Then subtract and store in locAdd.
     *
     * @param reScl the real component of the original unit cpx (complex)
     * @param imScl the imaginary component of the original unit cpx
     * @param locMult the location in the array of the cpx to be multiplied to
     * @param locAdd the location in the array of the cpx to be added to
     */
    private final void writePlusMinus(double reScl, double imScl,
                                      int locMult, int locAdd) {
        double multRe = freqs[locMult]; double multIm = freqs[locMult+1];
        double temp = multRe;
        multRe = reScl*multRe-imScl*multIm;
        multIm = imScl*temp+reScl*multIm;
        // now we have result of the multiplication
        freqs[locAdd] += multRe;
        freqs[locAdd+1] += multIm;
        freqs[locMult] = freqs[locAdd]-(multRe*2);
        freqs[locMult+1] = freqs[locAdd+1]-(multIm*2);
    }
}