package com.apps.navai;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

public class Session implements java.io.Serializable {
    private Annotation[] annotations;
    private String filePath;
    private String srcFile;

    private final transient String nullstr = "NULL7019";
    private int imageWidth;
    private int imageHeight;

    public Session(Annotation[] a, String fp, String src, int width, int height) {
        this(a,fp,width,height);
        srcFile = src;
    }

    public Session(Annotation[] a, String fp, int width, int height) {
        annotations = a;
        filePath = fp;
        imageWidth = width;
        imageHeight = height;
    }

    // why dont they have streams?
    public void genDescriptions() {
        System.out.println("Got to descriptions!");
        final Bitmap bitmap = CallAPI.getBitmap();
        int iter = 0;
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

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setAnnotation(Annotation[] a) {
        annotations = a;
    }

    public void setSourceFile(String src) {
        srcFile = src;
    }

    public Annotation getAnnotation(int i) {
        return annotations[i];
    }

    public int size() {
        return annotations.length;
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeInt(annotations.length);
        for(Annotation a: annotations) {
            a.writeObject(oos);
        }
        oos.writeUTF(filePath == null ? nullstr : filePath);
        oos.writeUTF(srcFile == null ? nullstr : srcFile);
        oos.writeInt(imageWidth);
        oos.writeInt(imageHeight);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final int lim = ois.readInt();
        annotations = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            (annotations[i] = new Annotation()).readObject(ois);
        }
        filePath = ois.readUTF();
        filePath = filePath.equals(nullstr) ? null : filePath;

        srcFile = ois.readUTF();
        srcFile = srcFile.equals(nullstr) ? null : srcFile;
        imageWidth = ois.readInt();
        imageHeight = ois.readInt();
    }

    public CharBuffer outform() throws IOException {
        CharBuffer buffer = new CharBuffer(10);
        buffer.appendln(filePath);
        buffer.appendln(imageWidth);
        buffer.appendln(imageHeight);
        buffer.appendln(annotations.length);
        for(Annotation ant: annotations) {
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
        return buffer;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", Arrays.toString(annotations), filePath, srcFile);
    }
}
