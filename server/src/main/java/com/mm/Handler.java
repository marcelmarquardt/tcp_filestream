package com.mm;

import com.mm.model.Message;
import com.mm.service.FileStreamService;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Optional;

public class Handler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LogManager.getLogger(Handler.class);

    private static final FileStreamService fileStreamService = FileStreamService.get();

    private void fileSize(ChannelHandlerContext ctx) {
        Optional<Long> fileSize = fileStreamService.getFileSize();
        if (fileSize.isEmpty()) {
            LOGGER.error("fileSize is null, closing socket");
            ctx.close();
            return;
        }
        ctx.channel().writeAndFlush(String.valueOf(fileSize.get()));
    }

    private void fileChunk(ChannelHandlerContext ctx, Message message) {
        int currentChunk = message.getFileChunk();
        if (currentChunk < 0) {
            LOGGER.error("invalid fileChunk, closing socket");
            ctx.close();
            return;
        }

        Optional<ByteBuf> byteBuf = fileStreamService.getChunkedFile(currentChunk);
        if (byteBuf.isEmpty() || !byteBuf.get().isReadable()) {
            LOGGER.error("invalid file chunk, closing socket");
            ctx.close();
            return;
        }
        ctx.channel().writeAndFlush(byteBuf.get());

        Optional<Integer> maxChunks = fileStreamService.getMaxChunks();
        if (maxChunks.isPresent()) {
            float progress = (float)currentChunk / (float)maxChunks.get() * 100; // in percent (0-100)
            LOGGER.info("progress: " + progress + " / 100");
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object in) {
        ByteBuf msg = (ByteBuf)in;
        int length = msg.readableBytes();
        byte[] bytes = new byte[length];
        msg.getBytes(msg.readerIndex(), bytes);

        Message message = new Message(bytes);
        switch (message.getId()) {
            case FILE_SIZE:
                fileSize(ctx);
                break;

            case FILE_CHUNK:
                fileChunk(ctx, message);
                break;

            default:
                LOGGER.error("unknown message id");
                break;
        }

        msg.release();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        LOGGER.info("client connected");
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("client disconnected");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("exception: " + cause.getMessage());
    }
}
