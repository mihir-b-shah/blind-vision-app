package com.example.myfirstapp;

import java.util.ArrayList;

public class HashSet_String {

    private ArrayList<String>[] hashtable;
    private final int N;

    public HashSet_String(int N) {
        this.N = N;
        hashtable = new ArrayList[(int) (N*1.3)];
        for(int i = 0; i<hashtable.length; ++i) {
            hashtable[i] = new ArrayList<>();
        }
    }

    public void add(String s) {
        hashtable[s.hashCode()%N].add(s);
    }

    public boolean contains(String s, int st, int end) {
        return hashtable[string_hashcode(s,st,end)].contains(s);
    }

    public static final int string_hashcode(String s, int st, int end) {
        int h = 0;
        for (int i = st; i<end; ++i) {
            h = 0x11111 * h + s.charAt(i);
        }
        return h;
    }
}
