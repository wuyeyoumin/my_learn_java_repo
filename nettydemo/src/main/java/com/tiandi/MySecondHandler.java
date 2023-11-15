package com.tiandi;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedFile;

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
            HttpResponse r = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            r.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
            .set(HttpHeaderNames.CONTENT_LENGTH, raf.length());
            ChunkedFile cf = new ChunkedFile(raf, 0, raf.length(), 8192);
            ctx.write(r);
            HttpChunkedInput chunkedInput = new HttpChunkedInput(cf);
            ctx.writeAndFlush(chunkedInput).addListener(ChannelFutureListener.CLOSE);
        } else {
            FullHttpResponse r = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(404));
            ctx.writeAndFlush(r).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
