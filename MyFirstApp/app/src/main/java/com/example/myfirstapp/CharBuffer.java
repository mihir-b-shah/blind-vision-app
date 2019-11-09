package com.example.myfirstapp;

import java.nio.BufferOverflowException;

public class CharBuffer {
    private char[] buf;
    private int pos;

    public CharBuffer() {
        buf = new char[2];
    }

    public CharBuffer(int N) {
        if(N > 1_000) {
            throw new BufferOverflowException();
        }
        buf = new char[Math.min(N,1_000)];
    }

    public int size() {
        return pos;
    }

    public char[] get_chars() {
        return buf;
    }

    public void appendln(int c) {
        range_check(2);
        buf[pos++] = (char) (c+'0');
        buf[pos++] = '\n';
    }

    public void appendln(String chars) {
        range_check(chars.length()+1);
        for(int i = 0; i<chars.length(); ++i) {
            buf[pos++] = chars.charAt(i);
        }
        buf[pos++] = '\n';
    }

    public void append(char c) {
        range_check(1);
        buf[pos++] = c;
    }

    public void appendln_nc(String chars) {
        if(chars != null) {
            appendln(chars);
        }
    }

    // lets hope this inlines... noooooo
    private final void range_check(int lim) {
        if(lim>Math.min(pos<<3,1_000)) {
            throw new BufferOverflowException();
        }
        if(buf.length < pos+lim) {
            char[] aux = new char[Math.max(pos<<1,(pos+lim)<<1)];
            System.arraycopy(buf,0,aux,0,pos);
            buf = aux;
        }
    }

}
