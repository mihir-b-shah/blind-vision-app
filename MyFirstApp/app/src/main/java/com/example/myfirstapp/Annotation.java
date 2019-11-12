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

    // default constructor
    public Annotation() {

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
        return d;
        /*
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
         */
    }

    // makes serializing methods more efficient.
    public void writeObject(ObjectOutputStream out) throws IOException {

        out.writeUTF(t);
        out.writeUTF(d);
        out.writeFloat(c);

        // Avoids expensive gc eInt(t.lengtof the bounding poly.

        if(b != null) {
            List<Vertex> vertices = b.getVertices();
            if(vertices != null) {
                out.writeInt(vertices.size());
                for (Vertex v : vertices) {
                    if (v != null && v.getX() != null && v.getY() != null) {
                        out.writeInt(v.getX());
                        out.writeInt(v.getY());
                    }
                }
            } else {
                out.writeInt(0);
            }

        } else {
            out.writeInt(0);
        }
    }

    public void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        t = in.readUTF();
        d = in.readUTF();
        c = in.readFloat();
        b = getBP(in);
    }

    private BoundingPoly getBP(ObjectInputStream in) throws IOException {

        BoundingPoly bp = new BoundingPoly();
        List<Vertex> vertices = new ArrayList<>();
        final int next = in.readInt();

        for(int i = 0; i<next; ++i) {
            Vertex v = new Vertex();
            v.setX(in.readInt());
            v.setY(in.readInt());
            vertices.add(v);
        }

        bp.setVertices(vertices);
        return bp;
    }
}