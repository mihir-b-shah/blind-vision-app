package com.apps.navai;

import java.util.Arrays;

public class ByteArray {
    private byte[] buffer;
    private int size;

    ByteArray() {
        buffer = new byte[2];
    }
    ByteArray(int N) { buffer = new byte[N];}

    public void add(byte b) {
        if(buffer.length == size) {
            byte[] aux = new byte[size << 1];
            System.arraycopy(buffer, 0, aux, 0, size);
            buffer = aux;
        }
        buffer[size++] = b;
    }

    public void expand(int len) {
        if(len > buffer.length) {
            byte[] aux = new byte[(int) (len*1.5)];
            System.arraycopy(buffer, 0, aux, 0, size);
            buffer = aux;
        }
    }

    public void contract(int len) {
        size = len;
    }

    public int size() {
        return size;
    }

    public void enqueue(byte[] b, int offset, int len) {
        System.arraycopy(b, offset, buffer, 0, len);
        size = len;
    }

    public byte[] getBuffer() {
        return buffer;
    }
    public void clear() {
        final byte b = 0;
        Arrays.fill(buffer, b);
        size = 0;
    }

    public boolean isEmpty() {
        return size == 0 || buffer[size-1] == '\0';
    }

    public String limString(int lim) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i<lim; ++i) {
            sb.append(buffer[i]);
            sb.append(' ');
        }
        return sb.toString();
    }
}