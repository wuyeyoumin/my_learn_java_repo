package com.tiandi;

import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class MyTestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        Path path = Paths.get(System.getProperty("user.dir"), "test_file.txt");
        RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r");
        ChunkedFile cf = new ChunkedFile(raf);
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, raf.length())
        .set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
        .set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        ctx.write(response);
        ByteBuf bf = Unpooled.copiedBuffer("hehehehe", CharsetUtil.UTF_8);
        ctx.writeAndFlush(bf).addListener(ChannelFutureListener.CLOSE);
        //ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()),ctx.newProgressivePromise());
        //ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        super.exceptionCaught(ctx, cause);
    }
}
