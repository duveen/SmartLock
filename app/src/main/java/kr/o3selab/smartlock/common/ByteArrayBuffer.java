package kr.o3selab.smartlock.common;

import java.util.Vector;

public class ByteArrayBuffer {

    private Vector<Byte> buffer;

    public static ByteArrayBuffer getBuffer() {
        return new ByteArrayBuffer();
    }

    private ByteArrayBuffer() {
        buffer = new Vector<>();
    }

    public ByteArrayBuffer append(byte[] bytes) {
        for (byte b : bytes) buffer.add(b);
        return this;
    }

    public byte[] toByteArray() {
        byte[] bytes = new byte[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) bytes[i] = buffer.get(i);
        return bytes;
    }
}