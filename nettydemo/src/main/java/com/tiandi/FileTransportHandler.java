package com.tiandi;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.Tika;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class FileTransportHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final String DIR_PREFIX = "/home/tiandi/files";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        // 只支持get请求
        if (!(msg.method().equals(HttpMethod.GET))) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
            return;
        }

        String uri = msg.uri();
        Path path = saniziteUri(uri);
        if (path == null) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN);
            return;
        }

        File file = path.toFile();
        if (!file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }
        if (file.isDirectory()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
            return;
        }

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        setMimetype(response, file);

        if (msg.headers().contains(HttpHeaderNames.RANGE)) {
            handleRange(ctx, response, file, msg.headers().get(HttpHeaderNames.RANGE));
        } else {
            sendFile(ctx, response, file);
        }
    }

    private void sendFile(ChannelHandlerContext ctx, HttpResponse response, File file) {
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());
        ctx.write(response);
        ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length())).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleRange(ChannelHandlerContext ctx, HttpResponse response, File file, String range) {
        if (range.startsWith("bytes=")) {
            range = range.substring(6);
            int pos = range.indexOf('-');
            if (pos == -1) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            long start = Long.parseLong(range.substring(0, pos));
            long end = Long.parseLong(range.substring(pos + 1));
            if (start > end) {
                sendError(ctx, HttpResponseStatus.BAD_REQUEST);
                return;
            }
            if (start == end) {
                sendError(ctx, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            if (start > file.length() || end > file.length() || start < 0) {
                sendError(ctx, HttpResponseStatus.REQUESTED_RANGE_NOT_SATISFIABLE);
                return;
            }
            response.headers().set(HttpHeaderNames.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + file.length());
            ctx.write(response);
            try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
                ChunkedFile chunkedFile = new ChunkedFile(raf);
                DefaultFileRegion defaultFileRegion = new DefaultFileRegion(file, start, end - start);
                ctx.writeAndFlush(defaultFileRegion).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void setMimetype(HttpResponse response, File file) {
        Tika tika = new Tika();
        try {
            String mime = tika.detect(file);
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, mime);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private Path saniziteUri(String uri) {
        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        uri = uri.replace("/", File.separator);
        Path path = Paths.get(DIR_PREFIX, uri);
        path = path.toAbsolutePath();
        if (!path.startsWith(DIR_PREFIX)) {
            return null;
        } else {
            return path;
        }
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer("error: " + status, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
