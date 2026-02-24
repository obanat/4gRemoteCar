package com.fh.lib;

/* renamed from: com.fh.lib.Define */
/* loaded from: classes.dex */
public class Define {

    /* renamed from: com.fh.lib.Define$CbDataInterface */
    /* loaded from: classes.dex */
    public interface CbDataInterface {
        void cb_data(int i, byte[] bArr, int i2);
    }

    /* renamed from: com.fh.lib.Define$CbFlyDataInterface */
    /* loaded from: classes.dex */
    public interface CbFlyDataInterface {
        void cb_data(FlyRecord flyRecord);
    }

    /* renamed from: com.fh.lib.Define$SerialDataCallBackInterface */
    /* loaded from: classes.dex */
    public interface SerialDataCallBackInterface {
        int SerialDataCallBack(int i, byte[] bArr, int i2);
    }

    /* renamed from: com.fh.lib.Define$StreamDataCallBackInterface */
    /* loaded from: classes.dex */
    public interface StreamDataCallBackInterface {
        void StreamDataCallBack(int i, int i2, FrameHead frameHead, byte[] bArr, int i3);
    }

    /* renamed from: com.fh.lib.Define$YUVDataCallBackInterface */
    /* loaded from: classes.dex */
    public interface YUVDataCallBackInterface {
        void update(int i, int i2);

        void update(byte[] bArr);
    }

    /* renamed from: com.fh.lib.Define$FlyRecord */
    /* loaded from: classes.dex */
    public class FlyRecord {
        public int bAvailable;
        public int mask;
        public long time;
        public int type;

        public FlyRecord() {
        }
    }

    /* renamed from: com.fh.lib.Define$VideoEncode */
    /* loaded from: classes.dex */
    public class VideoEncode {
        public int ctrlType;
        public int deinter;
        public int denoise;
        public int iFrameInterval;
        public int maxBitRate;
        public int maxFRate;
        public int quality;
        public int res;

        public VideoEncode() {
        }
    }

    /* renamed from: com.fh.lib.Define$WifiConfig */
    /* loaded from: classes.dex */
    public class WifiConfig {
        public String sChan;
        public String sPSK;
        public String sSSID;
        public int status;
        public int wifiMode;
        public int wifiType;

        public WifiConfig() {
        }
    }

    /* renamed from: com.fh.lib.Define$IpConfig */
    /* loaded from: classes.dex */
    public class IpConfig {
        public int isAutoIP;
        public String sGateway;
        public String sIP;
        public String sMark;
        public String sPort;

        public IpConfig() {
        }
    }

    /* renamed from: com.fh.lib.Define$DevSearch */
    /* loaded from: classes.dex */
    public class DevSearch {
        public String devIP;
        public String devName;
        public int isAlive;
        public String port;

        public DevSearch() {
        }
    }

    /* renamed from: com.fh.lib.Define$RecSearch */
    /* loaded from: classes.dex */
    public class RecSearch {
        public int chanSeldID;
        public int lockFSeldID;
        public int startDay;
        public int startMonth;
        public int startYear;
        public int stopDay;
        public int stopMonth;
        public int stopYear;
        public int typeSeldID;

        public RecSearch() {
        }
    }

    /* renamed from: com.fh.lib.Define$Record */
    /* loaded from: classes.dex */
    public class Record {
        public int chanID;
        public long dataSize;
        public int lockFlag;
        public int recType;
        public long startTime;
        public long stopTime;

        public Record() {
        }
    }

    /* renamed from: com.fh.lib.Define$PicSearch */
    /* loaded from: classes.dex */
    public class PicSearch {
        public int chanSeldID;
        public int lockFSeldID;
        public int startDay;
        public int startMonth;
        public int startYear;
        public int stopDay;
        public int stopMonth;
        public int stopYear;
        public int typeSeldID;

        public PicSearch() {
        }
    }

    /* renamed from: com.fh.lib.Define$Picture */
    /* loaded from: classes.dex */
    public class Picture {
        public int chanID;
        public long dataSize;
        public long frameCount;
        public int lockFlag;
        public int picType;
        public long startTime;
        public long stopTime;

        public Picture() {
        }
    }

    /* renamed from: com.fh.lib.Define$SDCardInfo */
    /* loaded from: classes.dex */
    public class SDCardInfo {
        public byte state;
        public long totalSize;
        public long usedSize;

        public SDCardInfo() {
        }
    }

    /* renamed from: com.fh.lib.Define$SDCardFormat */
    /* loaded from: classes.dex */
    public class SDCardFormat {
        public int formatProgress;
        public int formatState;

        public SDCardFormat() {
        }
    }

    /* renamed from: com.fh.lib.Define$PBRecTime */
    /* loaded from: classes.dex */
    public class PBRecTime {
        public long pbStartTime;
        public long pbStopTime;

        public PBRecTime() {
        }
    }

    /* renamed from: com.fh.lib.Define$BCSS */
    /* loaded from: classes.dex */
    public class BCSS {
        public int brightness;
        public int contrast;
        public int saturation;
        public int sharpness;

        public BCSS() {
        }
    }

    /* renamed from: com.fh.lib.Define$Preview */
    /* loaded from: classes.dex */
    public class Preview {
        public int blocked;
        public int chan;
        public int encId;
        public int transMode;

        public Preview() {
        }
    }

    /* renamed from: com.fh.lib.Define$FrameHead */
    /* loaded from: classes.dex */
    public class FrameHead {
        public int frameType;
        public int height;
        public long timeStamp;
        public int videoFormat;
        public int width;

        public FrameHead() {
        }

        public String toString() {
            return "FrameHead{frameType=" + this.frameType + ", videoFormat=" + this.videoFormat + ", width=" + this.width + ", height=" + this.height + ", timeStamp=" + this.timeStamp + '}';
        }
    }

    /* renamed from: com.fh.lib.Define$DeviceTime */
    /* loaded from: classes.dex */
    public class DeviceTime {
        public int day;
        public int hour;
        public int minute;
        public int month;
        public int msecond;
        public int second;
        public int wday;
        public int year;

        public DeviceTime() {
        }
    }

    /* renamed from: com.fh.lib.Define$Res_e */
    /* loaded from: classes.dex */
    public enum Res_e {
        FHNPEN_ER_QCIF("QCIF", 0),
        FHNPEN_ER_CIF("CIF", 1),
        FHNPEN_ER_4CIF("4CIF", 2),
        FHNPEN_ER_D1("D1", 3),
        FHNPEN_ER_640x480("VGA", 4),
        FHNPEN_ER_QVGA("QVGA", 5),
        FHNPEN_ER_720P("720P", 6),
        FHNPEN_ER_960P("960P", 7),
        FHNPEN_ER_1080P("1080P", 8),
        FHNPEN_ER_960H("960H", 9);
        
        private int index;
        private String name;

        Res_e(String str, int i) {
            this.name = str;
            this.index = i;
        }

        public int getIndex() {
            return this.index;
        }

        public String getName() {
            return this.name;
        }

        public static String getNameByIndex(int i) {
            Res_e[] values;
            for (Res_e res_e : values()) {
                if (i == res_e.index) {
                    return res_e.name;
                }
            }
            return null;
        }
    }

    /* renamed from: com.fh.lib.Define$MP4Config */
    /* loaded from: classes.dex */
    public class MP4Config {
        public int audioBitWidth;
        public int audioLenPerPacket;
        public int audioMaxBitrate;
        public int audioObjectType;
        public int audioSamplerate;
        public int audioTrack;
        public int streamType;
        public int videoHeight;
        public int videoWidth;

        public MP4Config() {
        }
    }
}
