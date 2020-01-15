package com.apps.navai;

import static java.lang.Math.*;
import java.nio.BufferOverflowException;

/* Currently since I'm using the naive matrix solve,
64 or 128 is the max value that can be used with filtering for this buffer. */

public class CircularBuffer {
    private static final int MAX_SIZE = 0x8000;
    private static final double EPS = 1e-7;

    private final double[] data;
    private final double[] freqs;
    private final double[] trigCache;
    private final double[] kernel;

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

        // build the kernel
        kernel = new double[N-1];
    }

    private static void naiveSolve(double[] mat, double[] res) {
        final int SIZE = res.length;
        outer: for(int i = 0; i<SIZE; ++i) {
            for(int j = 0; j<i; ++j) {
                int pos = i-1;
                if(abs(mat[SIZE*i+j]) > EPS) {
                    while(abs(mat[SIZE*pos+j]) < EPS) {
                        --pos;
                    }
                    double mult = mat[SIZE*i+j]/mat[SIZE*pos+j];
                    for(int k = 0; k<SIZE; ++k) {
                        mat[SIZE*i+k] -= mult*mat[SIZE*pos+k];
                    }
                    res[i] -= mult*res[pos];
                }
            }
            if(abs(mat[i*(SIZE+1)]-1d) > EPS) {
                double mult = 1/mat[(SIZE+1)*i];
                for(int c = 0; c<SIZE; ++c) {
                    mat[SIZE*i+c] *= mult;
                }
                res[i] *= mult;
            }
        }

        for(int i = SIZE-2; i>=0; --i) {
            for(int j = i+1; j<SIZE; ++j) {
                double mult = mat[SIZE*i+j]/mat[(SIZE+1)*j];
                for(int k = 0; k<SIZE; ++k) {
                    mat[SIZE*i+k] -= mult*mat[SIZE*j+k];
                }
                res[i] -= mult*res[j];
            }
        }
    }

    private double lsqFilter() {
        final int LIM = N >>> 1;
        double sum = 0;
        final int START = writePtr-1;
        for(int i = 0; i<LIM; ++i) {
            sum += data[START-(i >> 1) & MASK]*kernel[i];
        }
        return sum;
    }

    private double[] buildAvgMatrix(double[] row) {
        final int SIZE = 1+(row.length >>> 1);
        final double[] mat = new double[SIZE*SIZE];

        for(int i = 0; i<SIZE; ++i) {
            for(int j = 0; j<SIZE; ++j) {
                mat[SIZE*i+j] = (row[i+j]+row[abs(i-j)])/2;
            }
        }

        return mat;
    }


    // please inline
    private final double sinc(double x) {
        if(x == 0) {
            return 1;
        } else return sin(PI*x)/(PI*x);
    }

    public void write(int val) {
        data[writePtr++ & MASK] = val;
        data[writePtr++ & MASK] = 0d;
    }

    public void writeFilter(int val) {
        data[writePtr++ & MASK] = val;
        data[writePtr++ & MASK] = 0d;
        data[writePtr-2] = lsqFilter();
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

    public void buildKernel() {
        final int M = kernel.length >>> 1;
        final int K = 1000; // weight on stopband
        final double freqPass = domIndex*PI/(N >>> 1);
        /* this val for freqStop is really bad, i'll fix it
           right now just 0.5 more or the nyquist-0.1*/
        final double freqStop = min(0.5+freqPass,PI-0.1);

        final double[] row = new double[N-1];
        row[0] = freqPass+K*(1-freqStop);
        for(int i = 1; i<N-1; ++i) {
            row[i] = freqPass*sinc(freqPass*i)-K*freqStop*sinc(freqStop*i);
        }

        final double[] coeffs = new double[M+1];
        for(int i = 0; i<coeffs.length; ++i) {
            coeffs[i] = freqPass*sinc(freqPass*i);
        }

        naiveSolve(buildAvgMatrix(row), coeffs);

        int ctr = 0;
        for(int i = M; i>0; --i) {
            kernel[ctr++] = -coeffs[i]/2;
        }
        kernel[ctr++] = -coeffs[0];
        for(int i = 1; i<M+1; ++i) {
            kernel[ctr++] = -coeffs[i]/2;
        }
        kernel[0] += 1;
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