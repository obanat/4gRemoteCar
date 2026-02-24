package com.obana.remotecar.JJRC;



import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCrypto;
import android.media.MediaFormat;
import android.view.Surface;

import com.fh.lib.Define;
import com.fh.lib.FHSDK;
import com.fh.lib.PlayInfo;
import com.obana.remotecar.utils.AppLog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/* loaded from: classes.dex */
public class MyMediaCodec {
    public static int SHOW_MODE_3D = 2;
    public static int SHOW_MODE_FULLSCREEN = 1;
    public static int SHOW_MODE_NORMAL = 0;
    private static String TAG = "MyMediaCodec";
    private static MyMediaCodec instance;
    public int curVideoHeight;
    public int curVideoWidth;
    private ByteBuffer[] inputBuffers;
    private byte[] lastPPSData;
    private byte[] lastSPSData;
    protected Thread mDecodeThread;
    public GLFrameRenderer mFrameRender;
    public int mLastVideoHeight;
    public int mLastVideoWidth;
    public int mVideoHeight;
    public int mVideoWidth;
    private MediaCodec mediaCodec;
    private MediaFormat mediaFormat;
    private ByteBuffer[] outputBuffers;
    private int playHandle;
    private byte[] ppsData;
    private byte[] spsData;
    private Surface mSurface = null;
    private boolean mediaCodecStartF = false;
    private int mFrameNo = 0;
    private boolean forceIframe = false;
    protected boolean threadStartF = false;
    final BlockingQueue<DecodeDataQueue> queue = new LinkedBlockingQueue(100);
    public boolean isUpdateShowRect = true;
    public YUVData mYUVData = null;
    private int showMode = 0;
    public Define.StreamDataCallBackInterface fun = new Define.StreamDataCallBackInterface() { // from class: com.vison.baselibrary.Interface.MyMediaCodec.1
        @Override // com.p005fh.lib.Define.StreamDataCallBackInterface
        public void StreamDataCallBack(int i, int i2, Define.FrameHead frameHead, byte[] bArr, int i3) {
            AppLog.i(TAG,"StreamDataCallBack======> buf len:" + bArr.length);
            DecodeDataQueue decodeDataQueue = new DecodeDataQueue();
            decodeDataQueue.mDataBuf = bArr;
            decodeDataQueue.mFrameNo = MyMediaCodec.access$108(MyMediaCodec.this);
            decodeDataQueue.mBufLen = i3;
            decodeDataQueue.mFrameType = frameHead.frameType;
            decodeDataQueue.mVideoWidth = frameHead.width;
            decodeDataQueue.mVideoHeight = frameHead.height;
            try {
                MyMediaCodec.this.queue.put(decodeDataQueue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    static /* synthetic */ int access$108(MyMediaCodec myMediaCodec) {
        int i = myMediaCodec.mFrameNo;
        myMediaCodec.mFrameNo = i + 1;
        return i;
    }

    /* loaded from: classes.dex */
    public class YUVData {
        public byte[] dataU;
        public byte[] dataV;
        public byte[] dataY;
        public byte[] dataYUV;
        public byte[] dataYV12;

        public YUVData() {
        }
    }

    /* loaded from: classes.dex */
    public class DecodeDataQueue {
        public int mBufLen;
        public byte[] mDataBuf;
        public int mFrameNo;
        public int mFrameType;
        public int mStreamType;
        public int mVideoHeight;
        public int mVideoWidth;

        public DecodeDataQueue() {
        }
    }

    public static MyMediaCodec getInstance() {
        if (instance == null) {
            instance = new MyMediaCodec();
        }
        return instance;
    }

    public void setShowMode(int i) {
        this.showMode = i;
    }

    public int getShowMode() {
        return this.showMode;
    }

    public int getVideoWidth() {
        return this.mVideoWidth;
    }

    public int getVideoHeight() {
        return this.mVideoHeight;
    }

    public void init(GLFrameRenderer gLFrameRenderer) throws IOException {
        this.mFrameRender = gLFrameRenderer;
        setShowMode(SHOW_MODE_FULLSCREEN);
        if (PlayInfo.decodeType == 0) {
            return;
        }
        FHSDK.registerStreamDataCallBack(FHSDK.handle,this.fun);
        this.mYUVData = new YUVData();
        this.mediaCodec = MediaCodec.createDecoderByType("video/avc");
        this.mediaFormat = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
        this.mediaFormat.setInteger("color-format", 2135033992);
        this.mDecodeThread = new Thread(new decodeThread(), "decodeThread");
        this.threadStartF = true;
        this.mDecodeThread.start();
    }

    public void unInit() {
        stopPlay();
        this.threadStartF = false;
        if (this.mDecodeThread != null) {
            this.mDecodeThread = null;
        }
        closeMediaCodec();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:52:0x0095  */
    /* JADX WARN: Removed duplicated region for block: B:60:0x00a9 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:64:0x00b7 A[LOOP:4: B:63:0x00b5->B:64:0x00b7, LOOP_END] */
    /* JADX WARN: Removed duplicated region for block: B:67:0x00c2 A[LOOP:5: B:66:0x00c0->B:67:0x00c2, LOOP_END] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public int getSPSAndPPS(byte[] r10, int r11) {
        /*
            Method dump skipped, instructions count: 238
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.vison.baselibrary.Interface.MyMediaCodec.getSPSAndPPS(byte[], int):int");
    }

    /* loaded from: classes.dex */
    class decodeThread implements Runnable {
        boolean firstTest = true;

        decodeThread() {
        }

        @Override // java.lang.Runnable
        public void run() {
            while (MyMediaCodec.this.threadStartF) {
                if (MyMediaCodec.this.queue.size() < PlayInfo.frameCacheNum) {
                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    DecodeDataQueue poll = MyMediaCodec.this.queue.poll();
                    if (poll != null) {
                        for (int i = 0; i < poll.mBufLen - 4; i++) {
                            int i2 = i + 0;
                            if ((poll.mDataBuf[i2] == 0 && poll.mDataBuf[i + 1] == 0 && poll.mDataBuf[i + 2] == 1) || (poll.mDataBuf[i2] == 0 && poll.mDataBuf[i + 1] == 0 && poll.mDataBuf[i + 2] == 0 && poll.mDataBuf[i + 3] == 1)) {
                                if (poll.mFrameType == 0 && this.firstTest) {
                                    MyMediaCodec.this.getSPSAndPPS(poll.mDataBuf, poll.mBufLen);
                                }
                                MyMediaCodec.this.inputFrame(poll.mDataBuf, i, poll.mBufLen - i, poll.mFrameType);
                            }
                        }
                    }
                    synchronized (this) {
                        if (MyMediaCodec.this.outputFrame() < 0) {
                            try {
                                Thread.sleep(10L);
                            } catch (InterruptedException e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                }
            }
            MyMediaCodec.this.mYUVData.dataY = null;
            MyMediaCodec.this.mYUVData.dataU = null;
            MyMediaCodec.this.mYUVData.dataV = null;
        }
    }

    public void openMediaCodec() {
        if (this.mediaCodecStartF && this.mediaFormat != null) {
            this.mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(this.spsData));
            this.mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(this.ppsData));
            return;
        }
        this.mediaFormat.setByteBuffer("csd-0", ByteBuffer.wrap(this.spsData));
        this.mediaFormat.setByteBuffer("csd-1", ByteBuffer.wrap(this.ppsData));
        if (PlayInfo.decodeType == 4) {
            this.mediaCodec.configure(this.mediaFormat, this.mSurface, (MediaCrypto) null, 0);
        } else {
            this.mediaCodec.configure(this.mediaFormat, (Surface) null, (MediaCrypto) null, 0);
        }
        this.mediaCodec.start();
        this.inputBuffers = this.mediaCodec.getInputBuffers();
        this.outputBuffers = this.mediaCodec.getOutputBuffers();
        this.mediaCodecStartF = true;
    }

    public void closeMediaCodec() {
        try {
            this.mediaCodecStartF = false;
            this.mSurface = null;
            if (this.mediaCodec != null) {
                this.mediaCodec.stop();
                this.mediaCodec.release();
                this.mediaCodec = null;
            }
            this.lastSPSData = null;
            this.lastPPSData = null;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void inputFrame(byte[] bArr, int i, int i2, int i3) {
        if (this.mediaCodecStartF) {
            try {
                int dequeueInputBuffer = this.mediaCodec.dequeueInputBuffer(0L);
                if (dequeueInputBuffer >= 0) {
                    ByteBuffer byteBuffer = this.inputBuffers[dequeueInputBuffer];
                    byteBuffer.clear();
                    byteBuffer.put(bArr, i, i2);
                    this.mediaCodec.queueInputBuffer(dequeueInputBuffer, 0, i2, System.currentTimeMillis(), 0);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    public int outputFrame() {
        if (this.mediaCodec == null) {
            return -1;
        }
        try {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            int dequeueOutputBuffer = this.mediaCodec.dequeueOutputBuffer(bufferInfo, 0L);
            switch (dequeueOutputBuffer) {
                case  -3 :
                    this.outputBuffers = this.mediaCodec.getOutputBuffers();
                    break;
                case -2:
                    this.outputBuffers = this.mediaCodec.getOutputBuffers();
                    break;
                case -1:
                    return -1;
                default:
                    if (PlayInfo.decodeType != 4) {
                        int i = this.mVideoWidth;
                        int i2 = this.mVideoHeight;
                        if (this.mYUVData.dataY == null && this.mYUVData.dataU == null && this.mYUVData.dataV == null) {
                            int i3 = i * i2;
                            this.mYUVData.dataY = new byte[i3];
                            int i4 = i3 * 1;
                            this.mYUVData.dataU = new byte[i4 / 4];
                            this.mYUVData.dataV = new byte[i4 / 4];
                            int i5 = i3 * 3;
                            this.mYUVData.dataYV12 = new byte[i5 / 2];
                            this.mYUVData.dataYUV = new byte[i5 / 2];
                        }
                        ByteBuffer byteBuffer = this.outputBuffers[dequeueOutputBuffer];
                        byteBuffer.position(bufferInfo.offset);
                        byteBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byteBuffer.get(this.mYUVData.dataYV12, 0, this.mYUVData.dataYV12.length);
                        int integer = this.mediaCodec.getOutputFormat().getInteger("color-format");
                        if (integer == 21) {
                            FHSDK.yuv420sp2yuv(this.mYUVData.dataYV12, i, i2, this.mYUVData.dataY, this.mYUVData.dataU, this.mYUVData.dataV);
                            if (this.mFrameRender != null) {
                                this.mFrameRender.dataFun.update(i, i2);
                            } else {
                                //FHSDK.send2Sdl(this.mYUVData.dataY, this.mYUVData.dataU, this.mYUVData.dataV, i, i2);
                            }
                        } else {
                            AppLog.e(TAG,"unSupport color Format :" + integer);
                        }
                    } else {
                        MediaFormat outputFormat = this.mediaCodec.getOutputFormat();
                        this.mVideoWidth = outputFormat.getInteger("width");
                        this.mVideoHeight = outputFormat.getInteger("height");
                    }
                    this.mediaCodec.releaseOutputBuffer(dequeueOutputBuffer, true);
                    break;
            }
            return 0;
        } catch (Exception unused) {
            return -1;
        }
    }

    public void stopPlay() {
        FHSDK.stopPlay(FHSDK.handle);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: classes.dex */
    public class PlayThread implements Runnable {
        PlayThread() {
        }

        @Override // java.lang.Runnable
        public void run() {
            synchronized (this) {
                FHSDK.startPlay(FHSDK.handle);
            }
        }
    }

    public void startPlay(Surface surface) {
        if (this.mSurface == null) {
            this.mSurface = surface;
        }
        new Thread(new PlayThread(), "PlayThread").start();
    }

    public void checkFrame(int i, int i2) {
        if (this.curVideoWidth == i && this.curVideoHeight == i2) {
            this.isUpdateShowRect = false;
            return;
        }
        this.curVideoWidth = i;
        this.curVideoHeight = i2;
        this.isUpdateShowRect = true;
    }

    private static MediaCodecInfo selectCodec(String str) {
        int codecCount = MediaCodecList.getCodecCount();
        for (int i = 0; i < codecCount; i++) {
            MediaCodecInfo codecInfoAt = MediaCodecList.getCodecInfoAt(i);
            if (codecInfoAt.isEncoder()) {
                for (String str2 : codecInfoAt.getSupportedTypes()) {
                    if (str2.equalsIgnoreCase(str)) {
                        return codecInfoAt;
                    }
                }
                continue;
            }
        }
        return null;
    }

    public void cleanQueue() {
        this.queue.clear();
    }
}