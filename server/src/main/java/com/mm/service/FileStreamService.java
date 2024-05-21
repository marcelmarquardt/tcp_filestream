package com.mm.service;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.stream.ChunkedFile;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileStreamService {
    private static final Logger LOGGER = LogManager.getLogger(FileStreamService.class);

    // chunkSize 8192 is netty default
    private static final Integer CHUNK_SIZE = 8192;
    private static FileStreamService INSTANCE;

    private static class FileInformation {
        long fileSize;
        String filePath;
        List<ByteBuffer> fileChunks;
    }

    private static final FileInformation file = new FileInformation();;

    private FileStreamService() {
    }

    public static FileStreamService get() {
        if (INSTANCE == null) {
            INSTANCE = new FileStreamService();
        }

        return INSTANCE;
    }

    synchronized public Optional<Long> getFileSize() {
        if (file.fileSize <= 0) {
            return Optional.empty();
        }

        return Optional.of(file.fileSize);
    }

    synchronized public Optional<ByteBuf> getChunkedFile(int chunk) {
        List<ByteBuffer> chunkedFile = file.fileChunks;
        if (chunkedFile != null) {
            // Unpooled.wrappedBuffer creates a new buffer since we don't have too much
            // control over netty's garbage collection (increasing reference count has not worked for me)
            return Optional.of(Unpooled.wrappedBuffer(chunkedFile.get(chunk)));
        }

        return Optional.empty();
    }

    synchronized public Optional<Integer> getMaxChunks() {
        List<ByteBuffer> chunkedFile = file.fileChunks;
        if (chunkedFile != null) {
            return Optional.of(chunkedFile.size() - 1);
        }

        return Optional.empty();
    }

    synchronized public boolean loadFile(String filePath) {
        file.filePath = filePath;
        LOGGER.info("loading file: " + file.filePath);

        try {
            RandomAccessFile f = new RandomAccessFile(file.filePath, "r");
            file.fileSize = f.length();

            ChunkedFile chunkedFile = new ChunkedFile(f, 0, f.length(), CHUNK_SIZE);
            List<ByteBuffer> chunks = new ArrayList<>();
            while (chunkedFile.endOffset() - chunkedFile.currentOffset() > 0) {
                chunks.add(chunkedFile.readChunk(ByteBufAllocator.DEFAULT).nioBuffer());
            }

            file.fileChunks = chunks;
            LOGGER.info("successfully loaded: " + file.filePath);
            return true;
        } catch (FileNotFoundException f) {
            LOGGER.error("file not found: " + file.filePath);
        } catch (Exception e) {
            LOGGER.error("exception: " + e.getMessage());
        }

        LOGGER.error("failed to load file: " + file.filePath);
        return false;
    }
}
