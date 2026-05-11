package com.obana.remotecar.utils;

public class Constant {
    public static final int CMD_MOVE_UP = 0x0001;
    public static final int CMD_MOVE_DOWN = 0x0002;
    public static final int CMD_MOVE_LEFT = 0x0003;
    public static final int CMD_MOVE_RIGHT = 0x0004;
    public static final int CMD_MOVE_STOP = 0x0005;

    public static final int CMD_CAMERA_UP = 0x0010;
    public static final int CMD_CAMERA_DOWN = 0x0011;
    public static final int CMD_CAMERA_STOP = 0x0012;
    public static final int CMD_BATTERY = 0x0020;

    public static final String DEF_LOCAL_IP ="192.168.10.1";
    public static final int DEF_LOCAL_PORT =28000;
    public static final int DEF_WS_PORT =34000;
    public static final String DEF_REDIS_HOST= "118.25.94.111";
    public static final int DEF_REDIS_PORT = 38086;

    public static final String SP_KEY_MAC= "clientId";
    public static final String SP_KEY_LOCAL_IP= "serverIp";
    public static final String SP_KEY_LOCAL_PORT= "serverPort";
    public static final String SP_KEY_NETWORK_TYPE= "networkType";
    public static final String SP_KEY_NET_TYPE= "netType";

    public static final String SP_KEY_CONTROL_TYPE= "controlType";
}
