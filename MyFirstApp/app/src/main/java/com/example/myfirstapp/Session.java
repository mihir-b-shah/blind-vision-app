package com.example.myfirstapp;

import com.google.api.services.vision.v1.model.Vertex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;

public class Session implements java.io.Serializable {
    private Annotation[] annotations;
    private String filepath;
    private String srcfile;
    private final String nullstr = "NULL7019";

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
        for(Annotation a: annotations) {
            a.writeObject(oos);
        }
        oos.writeUTF(filepath == null ? nullstr : filepath);
        oos.writeUTF(srcfile == null ? nullstr : srcfile);
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        final int lim = ois.readInt();
        annotations = new Annotation[lim];
        for(int i = 0; i<lim; ++i) {
            (annotations[i] = new Annotation()).readObject(ois);
        }
        filepath = ois.readUTF();
        filepath = filepath.equals(nullstr) ? null : filepath;

        srcfile = ois.readUTF();
        srcfile = srcfile.equals(nullstr) ? null : srcfile;
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
                    System.out.printf("(%d, %d)%n", v.getX(), v.getY());
                }
            }
        }
        buffer.append('\0');
        return buffer;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", Arrays.toString(annotations), filepath, srcfile);
    }
}
