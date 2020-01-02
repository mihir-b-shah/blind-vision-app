package com.example.myfirstapp;

import java.util.Arrays;

public class ByteBuffer {
    private byte[] buffer;
    private int size;

    ByteBuffer() {
        buffer = new byte[2];
    }
    ByteBuffer(int N) { buffer = new byte[N];}

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
            System.arraycopy(buffer, 0, aux, 0, len);
            buffer = aux;
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }
    public void clear() {
        byte b = 0;
        Arrays.fill(buffer, b);
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