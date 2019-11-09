package com.example.myfirstapp;

import com.google.api.services.vision.v1.model.Vertex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class Session implements java.io.Serializable {
    private Annotation[] annotations;
    private String filepath;
    private String srcfile;

    public Session(Annotation[] a, String fp, String src) {
        this(a,fp);
        srcfile = src;
    }

    public Session(Annotation[] a, String fp) {
        annotations = a;
        filepath = fp;
    }

    public void set_srcfile(String src) {
        srcfile = src;
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
        write_string(filepath, oos);
        write_string(srcfile, oos);
        oos.writeChar('\0');
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final int lim = ois.readInt();
        annotations = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            (annotations[i] = new Annotation()).readObject(ois);
        }
        filepath = Annotation.getChars(ois);
        srcfile = Annotation.getChars(ois);
    }

    public CharBuffer outform() throws IOException {
        CharBuffer buffer = new CharBuffer(10);
        buffer.appendln(filepath);
        buffer.appendln(annotations.length);
        for(Annotation ant: annotations) {
            buffer.appendln_nc(ant.t);
            buffer.appendln_nc(ant.d);
            String wr = ant.c != -1 ? Float.toString(ant.c) : "";
            buffer.appendln(wr);
            List<Vertex> vertices = ant.b.getVertices();
            if(vertices != null) {
                buffer.appendln(vertices.size());
                for (Vertex v : vertices) {
                    buffer.appendln(v.getX());
                    buffer.appendln(v.getY());
                }
            }
        }
        buffer.append('\0');
        return buffer;
    }

    private void write_string(String s, ObjectOutputStream oos) throws IOException {
        oos.writeInt(s.length());
        oos.write(' ');
        oos.writeChars(s);
    }
}
