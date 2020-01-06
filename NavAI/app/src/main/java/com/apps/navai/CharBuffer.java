package com.apps.navai;

import java.nio.BufferOverflowException;

public class CharBuffer {
    private char[] buf;
    private int pos;

    public CharBuffer() {
        buf = new char[2];
    }

    public CharBuffer(int N) {
        if(N > 10_000) {
            throw new BufferOverflowException();
        }
        buf = new char[N];
    }

    public int size() {
        return pos;
    }

    public char[] getChars() {
        return buf;
    }

    public void appendln(int c) {
        char[] buffer = new char[5];
        int div = 0;
        int ctr = buffer.length;
        do {
            div = c/10;
            buffer[--ctr] = (char) (c-10*div+48);
            c = div;
        } while(c > 0);
        final int lim = 5-ctr;
        rangeCheck(lim+1);
        System.arraycopy(buffer, ctr, buf, pos, lim);
        pos+=lim;
        buf[pos++] = '\n';
    }

    public void appendln(String chars) {
        rangeCheck(chars.length()+1);
        for(int i = 0; i<chars.length(); ++i) {
            buf[pos++] = chars.charAt(i);
        }
        buf[pos++] = '\n';
    }

    public void append(char c) {
        rangeCheck(1);
        buf[pos++] = c;
    }

    public void appendlnNC(String chars) {
        if(chars != null) {
            appendln(chars);
        }
    }

    // lets hope this inlines... noooooo
    private final void rangeCheck(int lim) {
        if(lim>10_000) {
            throw new BufferOverflowException();
        }
        if(buf.length < pos+lim) {
            char[] aux = new char[(pos+lim) << 1];
            System.arraycopy(buf,0,aux,0,pos);
            buf = aux;
        }
    }

}
