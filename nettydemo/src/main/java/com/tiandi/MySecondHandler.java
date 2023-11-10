package com.tiandi;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@Sharable
public class MySecondHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        FullHttpRequest request = (FullHttpRequest) msg;
        String uri = request.uri();
        Path path = Paths.get(System.getProperty("user.dir"), uri);
        System.out.println(path.toString());
        if (Files.exists(path)) {
            RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
            FullHttpResponse r = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            r.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
            .set(HttpHeaderNames.CONTENT_LENGTH, raf.length());
            ChunkedFile cf = new ChunkedFile(raf, 0, raf.length(), 8192);
            ctx.write(r);
            HttpChunkedInput chunkedInput = new HttpChunkedInput(cf);
            ctx.writeAndFlush(chunkedInput);
        } else {
            FullHttpResponse r = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(404));
            ctx.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
