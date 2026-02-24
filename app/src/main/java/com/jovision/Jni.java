package com.jovision;

/* loaded from: classes.dex */
public class Jni {
    public static native int Mp4Init();

    public static native int Mp4Pause();

    public static native int Mp4Prepare();

    public static native int Mp4Release();

    public static native int Mp4Resume();

    public static native int Mp4Start(Object obj);

    public static native int Mp4Stop(int i);

    public static native int SetMP4Uri(String str);

    public static native int StopMobLansearch();

    public static native boolean checkRecord(int i);

    public static native int connect(int i, int i2, String str, int i3, String str2, String str3, int i4, String str4, boolean z, int i5, boolean z2, int i6, Object obj, boolean z3, boolean z4, boolean z5, boolean z6, String str5);

    public static native void deinit();

    public static native boolean deinitAudioEncoder();

    public static native void deleteLog();

    public static native boolean disconnect(int i);

    public static native boolean enableLinkHelper(boolean z, int i, int i2);

    public static native void enableLog(boolean z);

    public static native boolean enablePlayAudio(int i, boolean z);

    public static native boolean enablePlayback(int i, boolean z);

    public static native byte[] encodeAudio(byte[] bArr);

    public static native void genVoice(String str, int i);

    public static native String getVersion();

    public static native boolean init(Object obj, int i, String str);

    public static native boolean initAudioEncoder(int i, int i2, int i3, int i4, int i5);

    public static native boolean isPlayAudio(int i);

    public static native boolean pause(int i);

    public static native boolean pauseAudio(int i);

    public static native boolean queryDevice(String str, int i, int i2);

    public static native boolean resume(int i, Object obj);

    public static native boolean resumeAudio(int i);

    public static native boolean screenshot(int i, String str, int i2);

    public static native int searchLanDevice(String str, int i, int i2, int i3, String str2, int i4, int i5);

    public static native int searchLanServer(int i, int i2);

    public static native boolean sendBytes(int i, byte b, byte[] bArr, int i2);

    public static native boolean sendInteger(int i, byte b, int i2);

    public static native boolean sendPrimaryBytes(int i, byte b, int i2, int i3, int i4, int i5, int i6, int i7, byte[] bArr, int i8);

    public static native boolean sendString(int i, byte b, boolean z, int i2, int i3, String str);

    public static native boolean sendSuperBytes(int i, byte b, boolean z, int i2, int i3, int i4, int i5, int i6, byte[] bArr, int i7);

    public static native boolean sendTextData(int i, byte b, int i2, int i3);

    public static native boolean setAccessPoint(int i, byte b, String str);

    public static native boolean setLinkHelper(String str);

    public static native boolean startRecord(int i, String str, boolean z, boolean z2, int i2);

    public static native boolean stopRecord();

    public static native void stopSearchLanServer();

    static {
        System.loadLibrary("alu");
        System.loadLibrary("play");
    }
}
