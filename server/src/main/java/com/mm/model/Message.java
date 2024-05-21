package com.mm.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Message {
    public Message(byte[] bytes) {
        id = fromInteger(Byte.toUnsignedInt(bytes[0]));
        if (id == MessageEnum.FILE_CHUNK) {
            fileChunk = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 4, 8)).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }
    }

    public enum MessageEnum {
        FILE_SIZE,
        FILE_CHUNK
    }

    private static MessageEnum fromInteger(int x) {
        return switch (x) {
            case 0 -> MessageEnum.FILE_SIZE;
            case 1 -> MessageEnum.FILE_CHUNK;
            default -> null;
        };
    }

    public MessageEnum getId() {
        return id;
    }

    public int getFileChunk() {
        return fileChunk;
    }

    private final MessageEnum id;
    private int fileChunk;
}

