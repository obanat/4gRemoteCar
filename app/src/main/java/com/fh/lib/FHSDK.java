package com.fh.lib;

import com.fh.lib.Define;

/* renamed from: com.fh.lib.FHSDK */
/* loaded from: classes.dex */
public class FHSDK {
    public static final int DECODE_TYPE_FFMPEG2OPENGL = 0;
    public static final int DECODE_TYPE_FFMPEG2SDL = 2;
    public static final int DECODE_TYPE_MEDIACODEC2GLVIEW = 4;
    public static final int DECODE_TYPE_MEDIACODEC2OPENGL = 1;
    public static final int DECODE_TYPE_MEDIACODEC2SDL = 3;
    @Deprecated
    public static final int DEVICE_TYPE_3518E = 4;
    public static final int DEVICE_TYPE_EYE = 6;
    public static final int DEVICE_TYPE_FH8610 = 1;
    public static final int DEVICE_TYPE_FH8620 = 2;
    public static final int DEVICE_TYPE_FH8810 = 3;
    @Deprecated
    public static final int DEVICE_TYPE_GM8136 = 5;
    public static final int DEVICE_TYPE_MJVGA = 7;
    public static final int DEVICE_TYPE_RTSP1080P = 9;
    public static final int DEVICE_TYPE_RTSP720P = 8;
    public static final int DEVICE_TYPE_VGA2 = 10;
    public static final int DEVICE_TYPE_VGA2_872 = 11;
    public static final int PLAY_TYPE_LOCATE_PLAYBACK = 3;
    public static final int PLAY_TYPE_MJVGA = 5;
    public static final int PLAY_TYPE_MP4FILE = 6;
    public static final int PLAY_TYPE_PREVIEW = 0;
    public static final int PLAY_TYPE_REMOTE_PLAYBACK = 2;
    public static final int PLAY_TYPE_RTSP = 4;
    public static final int PLAY_TYPE_UDP = 1;
    public static long handle;

    public static native int MP4Write(long j, int i, int i2, int i3, byte[] bArr);

    public static native long MP4WriteStart(String str, Define.MP4Config mP4Config);

    public static native int MP4WriteStop(long j);

    public static native boolean SetVodOffline(long j, boolean z);

    public static native boolean bind(long j, long j2);

    public static native boolean cleanPlayHandle(long j);

    public static native boolean clear();

    public static native long createBuffer(int i);

    public static native long createPlayHandle();

    public static native long createWindow(int i);

    public static native int destroyBuffer(long j);

    public static native boolean destroyWindow(long j);

    public static native boolean draw(long j);

    public static native boolean expandLookAt(long j, float f);

    public static native boolean eyeLookAt(long j, float f, float f2, float f3);

    public static native boolean faacCreateEncoder(long j, int i, int i2, int i3);

    public static native boolean faacDeleteEncoder(long j);

    public static native boolean faacEncode(long j, byte[] bArr, int i);

    public static native int ffScale(long j, byte[] bArr, byte[] bArr2);

    public static native long ffScaleStart(int i, int i2, int i3, int i4, int i5, int i6);

    public static native int ffScaleStop(long j);

    public static native boolean frameParse(long j, byte[] bArr, int i);

    public static native long getCurrentPts(long j);

    public static native int getDisplayMode(long j);

    public static native int getFieldOfView(long j);

    public static native int getImagingType(long j);

    public static native float getMaxHDegress(long j);

    public static native float getMaxVDegress(long j);

    public static native float getMaxZDepth(long j);

    public static native float getMinHDegress(long j);

    public static native float getMinVDegress(long j);

    public static native String getVersion();

    public static native float getVerticalCutRatio(long j);

    public static native float getViewAngle(long j);

    public static native boolean init(int i, int i2);

    public static native boolean isBind(long j);

    public static native int mp4GetCurSec(long j);

    public static native int mp4GetFileDuration(long j);

    public static native int mp4GetPlayStatus(long j);

    public static native boolean mp4SeekTo(long j, int i);

    public static native boolean mp4SetPlayStatus(long j, int i);

    public static native boolean phoneAudioRecord(int i);

    public static native boolean phoneAudioRecordData(byte[] bArr, int i);

    public static native boolean registerNotifyCallBack(long j, Define.CbDataInterface cbDataInterface);

    public static native boolean registerStreamDataCallBack(long j, Define.StreamDataCallBackInterface streamDataCallBackInterface);

    public static native boolean registerUpdateCallBack(long j, Define.YUVDataCallBackInterface yUVDataCallBackInterface);

    public static native boolean resetEyeView(long j);

    public static native boolean resetStandardCircle(long j);

    public static native boolean setDebugMode(long j, byte[] bArr, int i, int i2);

    public static native boolean setFieldOfView(long j, int i);

    public static native boolean setImagingType(long j, int i);

    public static native boolean setPlayInfo(long j, PlayInfo playInfo);

    public static native boolean setShotOn(long j);

    public static native boolean setShotPath(long j, String str);

    public static native boolean setStandardCircle(long j, float f, float f2, float f3);

    public static native boolean setVerticalCutRatio(long j, float f);

    public static native byte[] snapshot(int i, int i2, int i3, int i4);

    public static native boolean startLocalRecord(long j, String str);

    public static native boolean startLocalRecordMP4(long j, int i, String str);

    public static native boolean startLocalRecordMP4Ex(long j, int i, int i2, int i3, String str);

    public static native boolean startPlay(long j);

    public static native boolean stopLocalRecord(long j);

    public static native boolean stopLocalRecordMP4(long j);

    public static native boolean stopLocalRecordMP4Ex(long j);

    public static native boolean stopPlay(long j);

    public static native boolean unInit();

    public static native boolean unRegisterNotifyCallBack(long j);

    public static native boolean unRegisterStreamDataCallBack(long j);

    public static native boolean unRegisterUpdateCallBack(long j);

    public static native boolean unbind(long j);

    public static native boolean update(long j, byte[] bArr, int i, int i2);

    public static native boolean viewport(int i, int i2, int i3, int i4);

    public static native boolean yuv420sp2yuv(byte[] bArr, int i, int i2, byte[] bArr2, byte[] bArr3, byte[] bArr4);

    public static native boolean yuv420sp2yuv420p(byte[] bArr, int i, int i2, byte[] bArr2);

    static {
        System.loadLibrary("FHComponent");
        System.loadLibrary("FHDEV_Discover");
        System.loadLibrary("FHDEV_Net");
        System.loadLibrary("FHMP4");
        System.loadLibrary("main");
        handle = 0L;
    }
}
