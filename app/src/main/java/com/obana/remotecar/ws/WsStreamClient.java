package com.obana.remotecar.ws;

import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.util.concurrent.TimeUnit;

/**
 * WebSocket 流客户端 - 连接小车推流端
 * 直接将收到的原始字节块传给解码器，由 MediaCodec 内部处理 Annex B 流
 */
public class WsStreamClient {

    private static final String TAG = "WsStreamClient";

    public interface RawDataCallback {
        void onRawData(byte[] data);
    }

    public interface SimpleCallback {
        void onEvent();
    }

    public interface ErrorCallback {
        void onError(String message);
    }

    private final String url;
    private WebSocket webSocket = null;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build();

    private RawDataCallback onRawData = null;
    private SimpleCallback onConnected = null;
    private SimpleCallback onDisconnected = null;
    private ErrorCallback onError = null;

    private long totalBytes = 0;
    private long chunkCount = 0;

    public WsStreamClient(String url) {
        this.url = url;
    }

    public void setOnRawData(RawDataCallback cb) { this.onRawData = cb; }
    public void setOnConnected(SimpleCallback cb) { this.onConnected = cb; }
    public void setOnDisconnected(SimpleCallback cb) { this.onDisconnected = cb; }
    public void setOnError(ErrorCallback cb) { this.onError = cb; }

    public void connect() {
        Request request = new Request.Builder().url(url).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, Response response) {
                Log.d(TAG, "WebSocket connected to " + url);
                totalBytes = 0;
                chunkCount = 0;
                if (onConnected != null) onConnected.onEvent();
            }

            @Override
            public void onMessage(WebSocket ws, ByteString bytes) {
                byte[] data = bytes.toByteArray();
                totalBytes += data.length;
                chunkCount++;
                if (onRawData != null) onRawData.onRawData(data);
            }

            @Override
            public void onMessage(WebSocket ws, String text) {}

            @Override
            public void onClosing(WebSocket ws, int code, String reason) {
                ws.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                Log.d(TAG, "WebSocket closed: " + code + " " + reason);
                if (onDisconnected != null) onDisconnected.onEvent();
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, Response response) {
                Log.e(TAG, "WebSocket failure: " + t.getMessage());
                if (onError != null) onError.onError(t.getMessage() != null ? t.getMessage() : "Connection failed");
            }
        });
    }

    public void disconnect() {
        if (webSocket != null) webSocket.close(1000, "Client disconnect");
    }

    public String getStats() {
        if (totalBytes == 0) return "";
        double mb = totalBytes / (1024.0 * 1024.0);
        return String.format("%.1f MB, %d chunks", mb, chunkCount);
    }
}
