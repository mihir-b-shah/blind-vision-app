package com.example.myfirstapp;

import java.util.Arrays;

public class ByteBuffer {
    private byte[] buffer;
    private int size;

    ByteBuffer() {
        buffer = new byte[2];
    }

    public void add(byte b) {
        if(buffer.length == size) {
            byte[] aux = new byte[size << 1];
            System.arraycopy(buffer, 0, aux, 0, size);
            buffer = aux;
        }
        buffer[size++] = b;
    }

    public byte[] getBuffer() {
        return buffer;
    }
    public void clear() {
        byte b = 0;
        Arrays.fill(buffer, b);
    }
}