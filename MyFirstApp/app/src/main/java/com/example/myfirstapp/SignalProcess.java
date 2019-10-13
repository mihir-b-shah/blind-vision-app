package com.example.myfirstapp;

import java.util.ArrayList;
import org.apache.commons.math3.transform.FastFourierTransformer;

/*
Possible techniques:
1. filtering
2. thresholds
3. bayesian classifier
4. kNN
5. Fourier transform
6. Neural network
 */

public class SignalProcess {
    private ArrayList<Double> time;
    private ArrayList<Double> signal;

    public SignalProcess() {
        time = new ArrayList<>();
        signal = new ArrayList<>();
    }

    public void addData(double t, double s) {
        time.add(t); signal.add(s);
    }


}
