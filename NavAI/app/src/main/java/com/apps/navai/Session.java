package com.apps.navai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class Session implements java.io.Serializable {
    private Annotation[] annotationsOne;
    private Annotation[] annotationsTwo;

    private String filePathOne;
    private String filePathTwo;

    private String srcFileOne;
    private String srcFileTwo;

    private final transient String nullstr = "NULL7019";

    public Session(Annotation[] a1, Annotation[] a2, String fp1, String fp2,
                   String src1, String src2) {
        this(a1,a2,fp1,fp2);
        srcFileOne = src1;
        srcFileTwo = src2;
    }

    public Session(Annotation[] a1, Annotation[] a2, String fp1, String fp2) {
        annotationsOne = a1;
        annotationsTwo = a2;
        filePathOne = fp1;
        filePathTwo = fp2;
    }

    /**
     * Generate a complete session object.
     *
     * @param s1 a half-session, with other args null
     * @param s2 the other half or args null
     * @return the full session object.
     */
    public static Session combine(Session s1, Session s2) {
        if(s1.annotationsOne != null && s2.annotationsTwo != null) {
            return new Session(s1.annotationsOne, s2.annotationsTwo, s1.filePathOne,
                    s2.filePathTwo, s1.srcFileOne, s2.srcFileTwo);
        } else if(s1.annotationsTwo != null && s2.annotationsOne == null){
            return new Session(s2.annotationsOne, s1.annotationsTwo, s2.filePathOne,
                    s1.filePathTwo, s1.srcFileTwo, s2.srcFileOne);
        }
        return null;
    }

    public void genDescriptions(int index) {
        System.out.println("Got to descriptions!");
        final Bitmap bitmap = index == 0 ? CallAPI.getFirstBitmap() : CallAPI.getSecondBitmap();
        int iter = 0;
        final Annotation[] annotations = index == 0 ? annotationsOne : annotationsTwo;
        while(iter < annotations.length && annotations[iter].getRTag()==Annotation.OBJECT_TAG) {
            System.out.println("Iter counter " + iter);
            final Rect rect = annotations[iter].getRect();
            final Annotation annot = annotations[iter];
            // total extra memory bounded as <= sizeof(bitmap).
            Bitmap small = Bitmap.createBitmap(
                    bitmap, rect.left, rect.top, rect.width(), rect.height());
            FirebaseVisionImage img = FirebaseVisionImage.fromBitmap(small);
            annot.genDescription(img);
            small.recycle();
            ++iter;
        }
        bitmap.recycle();
        System.out.println("Recycled bitmaps!");
    }

    public String[] getDescrArray(int callNum) {
        Annotation[] annotations = callNum == 0 ? annotationsOne : annotationsTwo;
        String[] input = Arrays.stream(annotations)
                .map(Annotation::getDescription).toArray(String[]::new);
        return input;
    }

    public void setOutput(int callNum, ArrayList<String> strings, SpellCheck.FloatVector floats) {
        Annotation[] annotations = callNum == 0 ? annotationsOne : annotationsTwo;
        for(int i = 0; i<annotations.length; ++i) {
            annotations[i].updateDescr(String.format("%s %s",
                    strings.get(i<<1), strings.get(1+(i<<1))));
            annotations[i].setMatch(annotations[i].getConfidence() * (
                    floats.get(i) > 1f ? 1f : floats.get(i)));
        }
    }

    public void setAnnotationsOne(Annotation[] a) {
        annotationsOne = a;
    }
    public void setAnnotationsTwo(Annotation[] a) { annotationsTwo = a; }

    public void setSourceFileOne(String src) {
        srcFileOne = src;
    }
    public void setSourceFileTwo(String src) {
        srcFileTwo = src;
    }

    public Annotation getAnnotationFirst(int i) {
        return annotationsOne[i];
    }
    public Annotation getAnnotationSecond(int i) {
        return annotationsTwo[i];
    }

    public int sizeOne() {
        return annotationsOne.length;
    }
    public int sizeTwo() {
        return annotationsTwo.length;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeInt(annotationsOne.length);
        for(Annotation a: annotationsOne) {
            a.writeObject(oos);
        }
        oos.writeUTF(filePathOne == null ? nullstr : filePathOne);
        oos.writeInt(annotationsTwo.length);
        for(Annotation a: annotationsTwo) {
            a.writeObject(oos);
        }
        oos.writeUTF(filePathTwo == null ? nullstr : filePathTwo);
        oos.writeUTF(srcFileOne == null ? nullstr : srcFileOne);
        oos.writeUTF(srcFileOne == null ? nullstr : srcFileOne);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        int lim = ois.readInt();
        annotationsOne = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            (annotationsOne[i] = new Annotation()).readObject(ois);
        }
        filePathOne = ois.readUTF();
        filePathOne = filePathOne.equals(nullstr) ? null : filePathOne;

        lim = ois.readInt();
        annotationsTwo = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            (annotationsTwo[i] = new Annotation()).readObject(ois);
        }
        filePathTwo = ois.readUTF();
        filePathTwo = filePathTwo.equals(nullstr) ? null : filePathTwo;

        srcFileOne = ois.readUTF();
        srcFileOne = srcFileOne.equals(nullstr) ? null : srcFileOne;
        srcFileTwo = ois.readUTF();
        srcFileTwo = srcFileTwo.equals(nullstr) ? null : srcFileTwo;
    }

    private void annotBuffer(Annotation ant, CharBuffer buffer) {
        buffer.append(ant.getRTag());
        buffer.append('\n');
        buffer.appendlnNC(ant.getDescription());
        String wr = ant.getConfidence() != -1f ? Float.toString(ant.getConfidence()) : "";
        buffer.appendln(wr);
        if(ant.getRect() == null) {
            buffer.appendln(0);
        } else {
            Rect rect = ant.getRect();
            buffer.appendln(4);
            buffer.appendln(rect.left);
            buffer.appendln(rect.top);
            buffer.appendln(rect.right);
            buffer.appendln(rect.bottom);
        }
    }

    public CharBuffer outform() {
        CharBuffer buffer = new CharBuffer(10);
        if(filePathOne != null)
            buffer.appendln(filePathOne);
        if(filePathTwo != null)
            buffer.appendln(filePathTwo);
        if(annotationsOne != null) {
            buffer.appendln(annotationsOne.length);
            for (Annotation ant : annotationsOne) {
                annotBuffer(ant, buffer);
            }
        }
        if(annotationsTwo != null) {
            buffer.appendln(annotationsTwo.length);
            for (Annotation ant : annotationsTwo) {
                annotBuffer(ant, buffer);
            }
        }
        return buffer;
    }
}
