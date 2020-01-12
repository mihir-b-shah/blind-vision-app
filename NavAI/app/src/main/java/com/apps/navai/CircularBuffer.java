package com.apps.navai;

import static java.lang.Math.*;
import java.nio.BufferOverflowException;

// contains signal processing methods.
public class CircularBuffer {
    private static final int MAX_SIZE = 0x8000;
    private final double[] data;
    private final double[] freqs;
    private final double[] trigCache;
    private final double[] cpxCache;

    private final int N;
    private final int LOGN;
    private final int MASK;

    private int writePtr;
    private int domIndex;

    public CircularBuffer(int N) {
        if(N > MAX_SIZE)
            throw new BufferOverflowException();
        final int NBSL = N<<1;
        this.N = N;
        LOGN = Integer.numberOfTrailingZeros(N);
        data = new double[NBSL];
        MASK = NBSL-1;
        freqs = new double[NBSL];
        trigCache = new double[N];

        final int NBS = N >>> 1;
        for(int i = 0; i< NBS; ++i) {
            final int IBS = i << 1;
            trigCache[IBS] = cos(i*PI/NBS);
            trigCache[1+IBS] = -sqrt(1-pow(trigCache[IBS],2));
        }

        // Store W and W^3 for each N.
        cpxCache = new double[NBSL];
        final double BASE = PI/NBS;
        int pos;
        for(int i = 1; i<N; ++i) {
            pos = i << 1;
            cpxCache[pos] = cos(i*BASE);
            cpxCache[pos+1] = sin(i*BASE);
        }
    }

    public void write(int val) {
        data[writePtr++ & MASK] = val;
        data[writePtr++ & MASK] = 0d;
    }

    public void writeFilter(int val) {
        double fval = filter(val);
        data[writePtr++ & MASK] = fval;
        data[writePtr++ & MASK] = 0d;
    }

    public void setDominantFreq() {
        fft(0, 0);
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
        domIndex = idx << OBS >> OBS;
    }

    /**
     * Called after fft() and before respective write() call.
     *
     * @param newVal the new signal value.
     * @return the filtered value.
     */
    private double filter(int newVal) {
        // perform the dft on the new values
        for(int i = 0; i<N; ++i) {
            int pos = i << 1;
            freqs[pos] += newVal - data[writePtr & MASK];
            double temp = freqs[pos];
            freqs[pos] = cpxCache[pos]*freqs[pos]-cpxCache[pos+1]*freqs[pos+1];
            freqs[pos+1] = cpxCache[pos+1]*temp+cpxCache[pos]*freqs[pos+1];
        }

        double real = 0;
        for(int i = domIndex+1; i<N; ++i) {
            int pos = i << 1;
            real += cpxCache[pos]*freqs[pos+1]-cpxCache[pos+1]*freqs[pos];
        }
        System.out.printf("%d->%.3f%n", newVal, real/N);
        return real/N;
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