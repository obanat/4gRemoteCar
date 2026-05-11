package com.obana.remotecar.ws;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * WebSocket 模式的 SurfaceView
 * 管理 WsH264Decoder 和 WsStreamClient 的生命周期
 */
public class WsSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "WsSurfaceView";

    private WsH264Decoder mDecoder;
    private WsStreamClient mStreamClient;
    private boolean mSurfaceReady = false;

    public WsSurfaceView(Context context) {
        super(context);
        init();
    }

    public WsSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSurfaceReady = true;
        Log.d(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceReady = false;
        Log.d(TAG, "surfaceDestroyed");
        stop();
    }

    /**
     * 连接 WebSocket 并开始解码
     * @param wsUrl WebSocket URL, 如 ws://192.168.10.1:28000/ws
     */
    public void start(String wsUrl) {
        stop();

        if (!mSurfaceReady) {
            Log.w(TAG, "Surface not ready, abort start");
            return;
        }

        // 创建解码器
        mDecoder = new WsH264Decoder(getHolder().getSurface());
        mDecoder.start();

        // 创建 WebSocket 客户端
        mStreamClient = new WsStreamClient(wsUrl);
        mStreamClient.setOnRawData(data -> {
            if (mDecoder != null) mDecoder.feedRawData(data);
        });
        mStreamClient.setOnConnected(() ->
                Log.d(TAG, "WS connected, waiting SPS..."));
        mStreamClient.setOnDisconnected(() ->
                Log.d(TAG, "WS disconnected"));
        mStreamClient.setOnError(error ->
                Log.e(TAG, "WS error: " + error));
        mStreamClient.connect();

        Log.d(TAG, "Connecting to " + wsUrl);
    }

    public void stop() {
        if (mStreamClient != null) {
            mStreamClient.disconnect();
            mStreamClient = null;
        }
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder = null;
        }
    }
}
