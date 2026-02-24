package com.obana.remotecar.JniView;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;

/* loaded from: classes.dex */
public class Device implements Parcelable {
    public static final Parcelable.Creator<Device> CREATOR = new Parcelable.Creator<Device>() { // from class: com.jovision.beans.Device.1
        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Device createFromParcel(Parcel parcel) {
            Device device = new Device();
            device.fullNo = parcel.readString();
            device.ip = parcel.readString();
            return device;
        }

        /* JADX WARN: Can't rename method to resolve collision */
        @Override // android.os.Parcelable.Creator
        public Device[] newArray(int i) {
            return new Device[i];
        }
    };
    private ArrayList<Channel> channelList;
    private String fullNo;
    private String gid;
    private String ip;
    private boolean is05;
    private boolean isCard;
    private boolean isHelperEnabled;
    private boolean isJFH;
    private int no;
    private int port;
    private String pwd;
    private int type;
    private String user;

    @Override // android.os.Parcelable
    public int describeContents() {
        return 0;
    }

    public Device(String str, int i, String str2, int i2, String str3, String str4, int i3) {
        int i4 = 0;
        this.isCard = false;
        this.ip = str;
        this.port = i;
        this.gid = str2;
        this.no = i2;
        this.fullNo = str2 + i2;
        this.user = str3;
        this.pwd = str4;
        this.isHelperEnabled = false;
        this.channelList = new ArrayList<>();
        while (i4 < i3) {
            Channel channel = new Channel();
            channel.setIndex(i4);
            i4++;
            channel.setChannel(i4);
            this.channelList.add(channel);
        }
    }

    public Device() {
        this.isCard = false;
    }

    public ArrayList<Channel> getChannelList() {
        return this.channelList;
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public String getGid() {
        return this.gid;
    }

    public int getNo() {
        return this.no;
    }

    public String getFullNo() {
        return this.fullNo;
    }

    public String getUser() {
        return this.user;
    }

    public String getPwd() {
        return this.pwd;
    }

    public void setHelperEnabled(boolean z) {
        this.isHelperEnabled = z;
    }

    public boolean isHelperEnabled() {
        return this.isHelperEnabled;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append(this.fullNo);
        sb.append("(");
        sb.append(this.ip);
        sb.append(":");
        sb.append(this.port);
        sb.append("): ");
        sb.append("user = ");
        sb.append(this.user);
        sb.append(", pwd = ");
        sb.append(this.pwd);
        sb.append(", enabled = ");
        sb.append(this.isHelperEnabled);
        ArrayList<Channel> arrayList = this.channelList;
        if (arrayList != null) {
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                sb.append("\n");
                sb.append(this.channelList.get(i).toString());
            }
        }
        return sb.toString();
    }

    public int getType() {
        return this.type;
    }

    public void setType(int i) {
        this.type = i;
    }

    public boolean is05() {
        return this.is05;
    }

    public void set05(boolean z) {
        this.is05 = z;
    }

    public boolean isJFH() {
        return this.isJFH;
    }

    public void setJFH(boolean z) {
        this.isJFH = z;
    }

    public boolean isCard() {
        return this.isCard;
    }

    public void setCard(boolean z) {
        this.isCard = z;
    }

    public void setChannelList(ArrayList<Channel> arrayList) {
        this.channelList = arrayList;
    }

    public void setIp(String str) {
        this.ip = str;
    }

    public void setPort(int i) {
        this.port = i;
    }

    public void setGid(String str) {
        this.gid = str;
    }

    public void setNo(int i) {
        this.no = i;
    }

    public void setFullNo(String str) {
        this.fullNo = str;
    }

    public void setUser(String str) {
        this.user = str;
    }

    public void setPwd(String str) {
        this.pwd = str;
    }

    @Override // android.os.Parcelable
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.fullNo);
        parcel.writeString(this.ip);
    }
}