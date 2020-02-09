package com.apps.navai;

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
import java.util.List;

public class Annotation implements Serializable,Comparable<Annotation> {

    private static final String NULL = "NULL";
    public static final char OBJECT_TAG = 'o';

    private String descr; // description
    private float conf; // confidence
    private Rect rect; // bounding polygon
    private char tag; // tag
    private float match; // match with query string

    public Annotation(FirebaseVisionObject obj) {
        tag = 'o';
        conf = 1f;
        rect = obj.getBoundingBox();
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

        descr = labels.get(0).getText();
        labels.get(0).getConfidence();
        conf *= labels.get(0).getConfidence();
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

    public void setMatch(float match) {this.match = match; }

    public void setConf(float conf) {this.conf = conf;}

    public Rect getRect() {
        return rect;
    }

    public float getConf() {
        return conf*match;
    }

    public void updateDescr(String s) {
        descr = s;
    }

    @Override
    public int compareTo(Annotation e) {
        return Float.compare(e.getConf(), getConf());
    }

    @Override
    public String toString() {
        return descr;
    }

    // makes serializing methods more efficient.
    public void writeObject(ObjectOutputStream out) throws IOException {
        out.writeChar(tag);
        out.writeUTF(descr == null ? "NULL" : descr);
        out.writeFloat(conf);
        out.writeFloat(match);

        if(rect != null) {
            out.writeInt(4);
            out.writeInt(rect.left); out.writeInt(rect.top);
            out.writeInt(rect.right); out.writeInt(rect.bottom);
        } else {
            out.writeInt(0);
        }
    }

    public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        tag = in.readChar();
        descr = in.readUTF();
        descr = descr.equals(NULL) ? null : descr;
        conf = in.readFloat();
        match = in.readFloat();
        final int next = in.readInt();
        rect = next == 0 ? null : new Rect(in.readInt(), in.readInt(), in.readInt(), in.readInt());
    }
}