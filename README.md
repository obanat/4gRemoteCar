# 4gRemoteCar
4g远控车android app

支持3种方案
1.搭配openwrt系统的mjpg-streamer插件使用，获取流媒体的网址需配置为"http://%s:%d/?action=stream"
第一个参数是openwrt的地址，第二个参数是端口，默认值对应如下常量定义
    private static final String DEF_LOCAL_IP ="192.168.10.1";
    private static final int DEF_LOCAL_PORT =28000;
支持用户配置，配置后端口后为CMD端口号+1

2.搭配H264流媒体使用，即用ffmpeg生成流媒体后，在本机socket读取流媒体后，硬解码播放
socat tcp-l:1000,fork,reuseaddr system:'ffmpeg -y -f v4l2 -i /dev/video0 -an -r 10 -vcodec libx264 -pix_fmt yuv420p -preset ultrafast -tune zerolatency -f h264 -' &
实际测试，用tcp传流量，效果优于udp传输

3.jjrc，需搭配jjrc的解码库使用，封装较好

4.native播放，此方案是配合spy car专用，使用jnilib的方式填充surface，native的lib负责与遥控车交互
本地的地址参考如下常量定义：
    public static final String LOCAL_IP = "192.168.10.231";
    public static final int IPC_DEFAULT_PORT = 9101;
也支持远程p2p方式，地址和端口与mjpg方案一致


