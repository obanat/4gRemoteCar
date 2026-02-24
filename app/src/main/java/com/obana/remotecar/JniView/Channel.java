package com.obana.remotecar.JniView;

import android.view.Surface;
import android.view.SurfaceView;

/* loaded from: classes.dex */
public class Channel {
    private boolean agreeTextData;
    private int audioBlock;
    private int audioByte;
    private int audioEncType;
    private int audioType;
    private int channel;
    private int index;
    private boolean isAuto;
    private boolean isConfigChannel;
    private boolean isConnected;
    private boolean isPaused;
    private boolean isRemotePlay;
    private boolean isVoiceCall;
    private Device parent;
    private boolean singleVoice;
    private boolean supportVoice;
    private Surface surface;
    private SurfaceView surfaceView;

    public Channel(int i, int i2, boolean z, boolean z2) {
        this.isConnected = false;
        this.isPaused = false;
        this.isAuto = false;
        this.audioType = 0;
        this.audioByte = 0;
        this.audioEncType = 0;
        this.audioBlock = 0;
        this.agreeTextData = false;
        this.supportVoice = true;
        this.isVoiceCall = false;
        this.singleVoice = false;
        this.index = i;
        this.channel = i2;
        this.isConnected = z;
        this.isRemotePlay = z2;
        this.isConfigChannel = false;
    }

    public Channel() {
        this.isConnected = false;
        this.isPaused = false;
        this.isAuto = false;
        this.audioType = 0;
        this.audioByte = 0;
        this.audioEncType = 0;
        this.audioBlock = 0;
        this.agreeTextData = false;
        this.supportVoice = true;
        this.isVoiceCall = false;
        this.singleVoice = false;
        this.index = 0;
        this.channel = 0;
        this.isConnected = false;
        this.isRemotePlay = false;
        this.isConfigChannel = false;
    }

    public int getIndex() {
        return this.index;
    }

    public void setIndex(int i) {
        this.index = i;
    }

    public void setChannel(int i) {
        this.channel = i;
    }

    public int getChannel() {
        return this.channel;
    }

    public SurfaceView getSurfaceView() {
        return this.surfaceView;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public Surface getSurface() {
        return this.surface;
    }

    public void setSurface(Surface surface) {
        this.surface = surface;
    }

    public boolean isConnected() {
        return this.isConnected;
    }

    public void setConnected(boolean z) {
        this.isConnected = z;
    }

    public boolean isPaused() {
        return this.isPaused;
    }

    public void setPaused(boolean z) {
        this.isPaused = z;
    }

    public boolean isRemotePlay() {
        return this.isRemotePlay;
    }

    public void setRemotePlay(boolean z) {
        this.isRemotePlay = z;
    }

    public boolean isConfigChannel() {
        return this.isConfigChannel;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Channel-");
        int i = this.channel;
        sb.append(i < 0 ? "X" : Integer.valueOf(i));
        sb.append(", window = ");
        sb.append(this.index);
        sb.append(": isConnected = ");
        sb.append(this.isConnected);
        sb.append(", isPaused: ");
        sb.append(this.isPaused);
        sb.append(", surface = ");
        Surface surface = this.surface;
        sb.append(surface != null ? Integer.valueOf(surface.hashCode()) : "null");
        sb.append(", hashcode = ");
        sb.append(hashCode());
        return sb.toString();
    }

    public Device getParent() {
        return this.parent;
    }

    public void setParent(Device device) {
        this.parent = device;
    }

    public int getAudioType() {
        return this.audioType;
    }

    public void setAudioType(int i) {
        this.audioType = i;
    }

    public int getAudioByte() {
        return this.audioByte;
    }

    public void setAudioByte(int i) {
        this.audioByte = i;
    }

    public int getAudioEncType() {
        return this.audioEncType;
    }

    public int getAudioBlock() {
        return this.audioBlock;
    }

    public void setAudioEncType(int i) {
        this.audioEncType = i;
        if (i == 0) {
            this.audioBlock = 640;
        } else if (i == 1 || i == 2) {
            this.audioBlock = 640;
        } else if (i == 3) {
            this.audioBlock = AppConsts.ENC_G729_SIZE;
        } else {
            this.audioBlock = AppConsts.ENC_PCM_SIZE;
        }
    }

    public void setConfigChannel(boolean z) {
        this.isConfigChannel = z;
    }

    public boolean isAuto() {
        return this.isAuto;
    }

    public void setAuto(boolean z) {
        this.isAuto = z;
    }

    public boolean isVoiceCall() {
        return this.isVoiceCall;
    }

    public void setVoiceCall(boolean z) {
        this.isVoiceCall = z;
    }

    public boolean isSingleVoice() {
        return this.singleVoice;
    }

    public void setSingleVoice(boolean z) {
        this.singleVoice = z;
    }

    public boolean isSupportVoice() {
        return this.supportVoice;
    }

    public void setSupportVoice(boolean z) {
        this.supportVoice = z;
    }

    public boolean isAgreeTextData() {
        return this.agreeTextData;
    }

    public void setAgreeTextData(boolean z) {
        this.agreeTextData = z;
    }
}