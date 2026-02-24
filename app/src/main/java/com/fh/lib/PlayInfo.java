package com.fh.lib;


/* renamed from: com.fh.lib.PlayInfo */
/* loaded from: classes.dex */
public class PlayInfo {
    public static String RTSPUrl = null;
    public static final int TRANS_MODE_TCP = 1;
    public static final int TRANS_MODE_UDP = 0;
    public static int bandId = 0;
    public static int decodeType = 0;
    public static int deviceId = 0;
    public static DeviceType deviceType = null;
    public static int doubleCameraType = 0;
    public static String firVersion = null;
    public static String firmwareVer = null;
    public static int frameCacheNum = 0;
    public static int frameHeight = 0;
    public static int frameRate = 25;
    public static int frameWidth = 0;
    public static String localIpAddr = null;
    public static int locateRecType = 0;
    public static boolean mirror = false;
    public static int pbChan = 0;
    public static String pbRecFilePath = null;
    public static int pbRecType = 0;
    public static long pbStartTime = 0;
    public static long pbStopTime = 0;
    public static int playType = 1;
    public static int streamMode = 0;
    public static int streamType = 0;
    public static String targetIpAddr = null;
    public static int transMode = -1;
    public static int udpDevType = 0;
    public static int udpPort = 8888;
    public static int userID;
    public static int wifiRate;

    /* renamed from: com.fh.lib.PlayInfo$DeviceType */
    /* loaded from: classes.dex */
    public enum DeviceType {
        MJ_VGA,
        VS_VGA,
        VGA,
        VGA2,
        VGA2_872ET_320_320,
        VGA2_872AT_640_640,
        YU_YAN_720,
        UDP_720,
        HZ_5G_720_1,
        HZ_5G_720_2,
        UDP_1080,
        HZ_5G_1080_1,
        HZ_5G_1080_2,
        UDP_2K,
        UDP_4K,
        RTSP_720,
        _720P,
        RTSP_1080,
        I_1080,
        UDP_2K_30,
        UDP_2_7K,
        HI2K,
        HI4K,
        FH8626_ssv6x5x_24g_1920_1280,
        FH8626_ssv6x5x_5g_1920_1280,
        FH8856_8812cu_4000_1280,
        Mr100_ssv6x5x_2048_2048,
        Mr100_ssv6x5x_1920_1920,
        Mr100_ssv6x5x_1920_1280,
        Mr100_ssv6x5x_4000_2048,
        Mr100_ssv6x5x_4000_1920,
        Mr100_ssv6x5x_4000_1280,
        Mr100_8801_2048_2048,
        Mr100_8801_1920_1920,
        Mr100_8801_1920_1280,
        Mr100_8801_4000_2048,
        Mr100_8801_4000_1920,
        Mr100_8801_4000_1280
    }

    public static boolean is5G() {
        return wifiRate == 1 || deviceType == DeviceType.HZ_5G_720_1 || deviceType == DeviceType.HZ_5G_720_2 || deviceType == DeviceType.HZ_5G_1080_1 || deviceType == DeviceType.HZ_5G_1080_2 || deviceType == DeviceType.UDP_2K || deviceType == DeviceType.RTSP_720 || deviceType == DeviceType.RTSP_1080 || deviceType == DeviceType.UDP_2K_30 || deviceType == DeviceType.UDP_2_7K || deviceType == DeviceType.HI2K || deviceType == DeviceType.HI4K || deviceType == DeviceType.FH8626_ssv6x5x_5g_1920_1280;
    }

    public static String string() {
        return "{userID:" + userID + ",playType:" + playType + ",transMode:" + transMode + ",frameCacheNum:" + frameCacheNum + ",pbRecFilePath:" + pbRecFilePath + ",udpDevType:" + udpDevType + ",udpPort:" + udpPort + ",targetIpAddr:" + targetIpAddr + ",localIpAddr:" + localIpAddr + ",decodeType:" + decodeType + ",RTSPUrl:" + RTSPUrl + ",firmwareVer:" + firmwareVer + ",mirror:" + mirror + ",deviceType:" + deviceType + ",streamMode:" + streamMode + ",wifiRate:" + wifiRate + ",deviceId:" + deviceId + ",frameRate:" + frameRate + ",frameWidth:" + frameWidth + ",frameHeight:" + frameHeight + ",doubleCameraType:" + doubleCameraType + "}";
    }

    /* renamed from: com.fh.lib.PlayInfo$DeviceId */
    /* loaded from: classes.dex */
    public enum DeviceId {
        _872_720(1),
        _872_DOUBLE_CAMERA(2),
        MR100_5G_8822CS_4K(10),
        _8626_5G_8801_8603_1080P(13);
        

        /* renamed from: id */
        private int f56id;

        DeviceId(int i) {
            this.f56id = i;
        }

        public int getId() {
            return this.f56id;
        }
    }

    /* renamed from: com.fh.lib.PlayInfo$Band */
    /* loaded from: classes.dex */
    public enum Band {
        QZ_24G_1(1),
        NFGG_24G_2(2),
        QZ_58G_200(200),
        NFGG_58G_201(100),
        _8822CS_58G_202(202);
        

        /* renamed from: id */
        private int f55id;

        Band(int i) {
            this.f55id = i;
        }

        public int getId() {
            return this.f55id;
        }
    }
}
