package com.example.myfirstapp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Session implements java.io.Serializable {
    private Annotation[] annotations;
    private String filepath;

    public Session(Annotation[] a, String fp) {
        annotations = a;
        filepath = fp;
    }

    public Annotation get_annotation(int i) {
        return annotations[i];
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.writeInt(annotations.length);
        oos.write('\n');
        for(Annotation a: annotations) {
            a.writeObject(oos);
        }
        oos.writeInt(filepath.length());
        oos.write(' ');
        oos.writeChars(filepath);
        oos.writeChar('\0');
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final int lim = ois.readInt();
        annotations = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            annotations[i] = new Annotation();
            annotations[i].readObject(ois);
        }
        filepath = Annotation.getChars(ois);
    }
}
