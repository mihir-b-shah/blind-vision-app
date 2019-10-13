package com.example.myfirstapp;

import com.google.api.services.vision.v1.model.*;

import java.io.*;
import java.util.*;

public class Annotation implements Serializable,Comparable<Annotation> {

    public String d; // description
    public float s; // similarity
    public float c; // confidence
    public BoundingPoly b; // bounding polygon
    public String t; // tag

    public Annotation(LocalizedObjectAnnotation loa) {
        t = "o";
        d = loa.getName();
        c = loa.getScore() != null ? loa.getScore() : -1;
        b = loa.getBoundingPoly();
    }

    public Annotation(EntityAnnotation ea) {
        t = "l";
        d = ea.getDescription();
        c = ea.getScore() != null ? ea.getScore() : -1;
        b = ea.getBoundingPoly();
    }

    public Annotation(Paragraph p) {
        b = p.getBoundingBox();
        t = "t";
        c = p.getConfidence() != null ? p.getConfidence() : -1;

        StringBuilder sb = new StringBuilder();

        List<Word> words = p.getWords();
        for(Word w: words) {
            List<Symbol> symbols = w.getSymbols();
            for(Symbol s: symbols)
                sb.append(s.getText());
        }

        d = sb.toString();
    }

    public Annotation(String tag, String descr, float conf, BoundingPoly bp) {
        t = tag;
        d = descr;
        c = conf;
        b = bp;
    }

    public String getTag() {
        if(t.equals("o")) {
            return "Object ";
        } else if(t.equals("l")) {
            return "Logo ";
        } else {
            return "Text ";
        }
    }

    public float get_conf() {
        return c*s;
    }

    @Override
    public int compareTo(Annotation e) {
        return Float.compare(e.get_conf(), get_conf());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getTag());
        sb.append("Vertices: ");

        if(b != null && b.getVertices() != null)
            for(Vertex v: b.getVertices()) {
                if(v != null)
                    sb.append(String.format("(%d, %d)", v.getX(), v.getY()));
            }
        else
            sb.append("NONE ");

        sb.append("Name: " + d);
        sb.append("Score: " + c);
        return sb.toString();
    }

    // makes serializing methods more efficient.
    private void writeObject(ObjectOutputStream out) throws IOException {

        out.writeInt(t.length());
        out.writeChars(t);
        out.writeInt(d.length());
        out.writeChars(d);
        out.writeFloat(c);

        // Avoids expensive gc eInt(t.lengtof the bounding poly.

        if(b != null) {
            List<Vertex> vertices = b.getVertices();
            if(vertices != null)
                for(Vertex v: vertices) {
                    if(v != null && v.getX() != null && v.getY() != null) {
                        out.writeInt(v.getX());
                        out.writeInt(v.getY());
                    }
                }
        }

        out.writeChar('\0');
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        t = getChars(in);
        d = getChars(in);
        c = in.readFloat();
        b = getBP(in);
    }

    private BoundingPoly getBP(ObjectInputStream in) throws IOException {

        BoundingPoly bp = new BoundingPoly();
        List<Vertex> vertices = new ArrayList<>();

        int curr1;
        try {
            while ((curr1 = in.readInt()) != '\0') {
                int curr2 = in.readInt();
                Vertex v = new Vertex();
                v.setX(curr1);
                v.setY(curr2);
                vertices.add(v);
            }
        } catch (EOFException e) {}

        bp.setVertices(vertices);
        return bp;
    }

    private String getChars(ObjectInputStream in) throws IOException {
        int t_len = in.readInt();
        char[] t_chars = new char[t_len];

        for(int i = 0; i<t_len; i++)
            t_chars[i] = in.readChar();

        return new String(t_chars);
    }
}
