package com.obana.remotecar;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;

import com.obana.remotecar.utils.AppLog;

/**
 * RTSP播放视图
 * 使用ExoPlayer (Media3) 播放RTSP码流，避免Android原生MediaPlayer的RTSP解析bug
 */
public class RtspSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "RtspSurfaceView";

    private static final String RTSP_LOCAL_URL =
            "rtsp://192.168.1.1:554/user=admin&password=&channel=1&stream=0.sdp?real_stream";

    // 默认雄迈摄像头RTSP路径
    private static final String RTSP_PATH = "/user=admin&password=&channel=1&stream=0.sdp?real_stream";
    private static final int RTSP_PORT = 554;

    private SurfaceHolder mSurfaceHolder;
    private ExoPlayer mPlayer;
    private boolean mIsPlaying = false;
    private String mRtspUrl;
    private boolean mSurfaceReady = false;
    private String mPendingUrl = null;

    public RtspSurfaceView(Context context) {
        super(context);
        init();
    }

    public RtspSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RtspSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        AppLog.i(TAG, "surfaceCreated");
        mSurfaceReady = true;
        if (mPendingUrl != null) {
            String url = mPendingUrl;
            mPendingUrl = null;
            doStartPlayback(url);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        AppLog.i(TAG, "surfaceChanged: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        AppLog.i(TAG, "surfaceDestroyed");
        mSurfaceReady = false;
        stopPlayback();
    }

    /**
     * 设置RTSP地址
     * @param host 主机地址（IPv4或IPv6），如果为null则使用local模式默认地址
     */
    public void setRtspUrl(String host) {
        if (host == null || host.isEmpty()) {
            mRtspUrl = RTSP_LOCAL_URL;
        } else if (host.contains(":")) {
            // IPv6地址，需要用中括号
            mRtspUrl = "rtsp://[" + host + "]:" + RTSP_PORT + RTSP_PATH;
        } else {
            // IPv4地址
            mRtspUrl = "rtsp://" + host + ":" + RTSP_PORT + RTSP_PATH;
        }
        AppLog.i(TAG, "RTSP URL: " + mRtspUrl);
    }

    /**
     * 开始播放RTSP码流
     * @param ipv6Addr IPv6地址，null则使用local模式
     */
    public void startPlayback(String ipv6Addr) {
        if (mIsPlaying) {
            AppLog.i(TAG, "Already playing, stop first");
            stopPlayback();
        }

        setRtspUrl(ipv6Addr);

        if (!mSurfaceReady) {
            AppLog.i(TAG, "Surface not ready, queuing playback");
            mPendingUrl = mRtspUrl;
            return;
        }

        doStartPlayback(mRtspUrl);
    }

    private void doStartPlayback(String url) {
        try {
            releasePlayer();

            Context context = getContext();
            mPlayer = new ExoPlayer.Builder(context).build();
            mPlayer.setVideoSurface(mSurfaceHolder.getSurface());

            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_READY:
                            AppLog.i(TAG, "RTSP stream ready, playing");
                            mIsPlaying = true;
                            break;
                        case Player.STATE_ENDED:
                            AppLog.i(TAG, "Playback ended");
                            mIsPlaying = false;
                            break;
                        case Player.STATE_BUFFERING:
                            AppLog.i(TAG, "Buffering...");
                            break;
                        case Player.STATE_IDLE:
                            AppLog.i(TAG, "Player idle");
                            break;
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    AppLog.e(TAG, "ExoPlayer error: " + error.getMessage());
                    mIsPlaying = false;
                }
            });

            RtspMediaSource.Factory rtspFactory = new RtspMediaSource.Factory();
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(url))
                    .build();

            mPlayer.setMediaSource(rtspFactory.createMediaSource(mediaItem));
            mPlayer.prepare();
            mPlayer.setPlayWhenReady(true);

            AppLog.i(TAG, "ExoPlayer preparing RTSP: " + url);

        } catch (Exception e) {
            AppLog.e(TAG, "Failed to start RTSP playback: " + e.getMessage());
            mIsPlaying = false;
        }
    }

    /**
     * 停止播放
     */
    public void stopPlayback() {
        releasePlayer();
        mIsPlaying = false;
        mPendingUrl = null;
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            try {
                mPlayer.release();
            } catch (Exception e) {
                AppLog.e(TAG, "Error releasing player: " + e.getMessage());
            }
            mPlayer = null;
        }
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        return mIsPlaying;
    }
}
