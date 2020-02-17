package com.apps.navai;

import android.annotation.SuppressLint;
import android.graphics.Rect;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.text.FirebaseVisionText;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class Annotation implements Serializable,Comparable<Annotation> {

    private static final String NULL = "NULL";
    public static final char OBJECT_TAG = 'o';

    private String descr; // description
    private float conf; // confidence
    private Rect rect; // bounding polygon
    private char tag; // tag
    double[] extra;

    public Annotation(FirebaseVisionObject obj) {
        tag = 'o';
        conf = 1f;
        rect = obj.getBoundingBox();
    }

    // pls remove
    public double[] getExtra() {
        return extra;
    }

    public void genDescription(final FirebaseVisionImage image) {
        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                .getOnDeviceImageLabeler();
        Task<List<FirebaseVisionImageLabel>> task = labeler.processImage(image);
        while(!task.isComplete()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if(task.isCanceled() || !task.isSuccessful()) {
            System.err.printf("Image labeling for image not work.%n");
        }

        List<FirebaseVisionImageLabel> labels = task.getResult();

        extra = labels.stream().
                mapToDouble(FirebaseVisionImageLabel::getConfidence).toArray();
        if(extra == null) {
            extra = new double[0];
        }
        descr = join(labels.stream().map(FirebaseVisionImageLabel::getText).
                toArray(String[]::new));
        /*
        NEEDS TO BE FIXED

        labels.get(0).getConfidence();
        conf *= labels.get(0).getConfidence(); */
    }

    public static String join(String[] data) {
        StringBuilder sb = new StringBuilder();
        for (String s : data) {
            sb.append(s);
            sb.append('\\'); sb.append('t');
        }

        return sb.toString().trim();
    }

    public Annotation(FirebaseVisionText.TextBlock p) {
        rect = p.getBoundingBox();
        tag = 't';
        conf = p.getConfidence() != null ? p.getConfidence() : -1;
        descr = p.getText().replaceAll("\\s+", " ");
    }

    // default constructor
    public Annotation() {
    }

    public Annotation(char tag, String descr, float conf, Rect bp) {
        this.tag = tag;
        this.descr = descr;
        this.conf = conf;
        rect = bp;
    }

    public char getRTag() {
        return tag;
    }

    public String getDescription() {
        return descr;
    }

    public float getConfidence() {
        return conf;
    }

    public int getExtraCount() {return extra.length;}

    public void multConf(float conf) {this.conf *= conf;}

    public Rect getRect() {
        return rect;
    }

    public void updateDescr(String s) {
        descr = s;
    }

    public void dotScores(float[] vals, int s, int e) {
        float score = 0;
        for(int i = s; i<e; ++i) {
            score += extra[i-s]*vals[i];
        }
        score /= (e-s);
        conf *= score;
    }

    @Override
    public int compareTo(Annotation e) {
        return Float.compare(e.getConfidence(), getConfidence());
    }

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("Type: %c, Description: %s, Conf: %f, Bounds: %f, %f",
                tag, descr, conf, rect.exactCenterX(), rect.exactCenterY());
    }

    // makes serializing methods more efficient.
    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeChar(tag);
        out.writeUTF(descr == null ? "NULL" : descr);
        out.writeFloat(conf);
        if(extra == null) {
            out.writeInt(0);
        } else {
            out.writeInt(extra.length);
            for(int i = 0; i<extra.length; ++i) {
                out.writeDouble(extra[i]);
            }
        }

        if(rect != null) {
            out.writeInt(4);
            out.writeInt(rect.left); out.writeInt(rect.top);
            out.writeInt(rect.right); out.writeInt(rect.bottom);
        } else {
            out.writeInt(0);
        }
    }

    public void readObject(ObjectInputStream in) throws IOException {
        tag = in.readChar();
        descr = in.readUTF();
        descr = descr.equals(NULL) ? null : descr;
        conf = in.readFloat();
        int extraLIM = in.readInt();
        extra = new double[extraLIM];
        for(int i = 0; i<extra.length; ++i) {
            extra[i] = in.readDouble();
        }
        final int next = in.readInt();
        rect = next == 0 ? null : new Rect(in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }
}