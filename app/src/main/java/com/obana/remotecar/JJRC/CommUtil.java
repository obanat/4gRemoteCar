package com.obana.remotecar.JJRC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;

import com.fh.lib.PlayInfo;
import com.obana.remotecar.utils.AppLog;
import com.obana.remotecar.utils.ByteUtility;
import com.obana.remotecar.utils.TcpForwarder;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;


public class CommUtil {
	private static final String TAG = "CommUtil";
    public static final int TCP_CONNECT_SUCCEED = 2701;
    public static final int TCP_ERROR = 2703;
    public static final int TCP_RECEIVE_DATA = 2702;
    public static final int UDP_RECEIVE_DATA = 2704;

    public static final String HELIWAY_LOCAL_TCP_ADDR = "172.16.10.1";
    public static final int HELIWAY_LOCAL_TCP_PORT = 8888;


    public static final int HELIWAY_REMOTE_TCP_PORT = 28000;
    public static final int HELIWAY_REMOTE_UDP_PORT = 28002;
    
    public static final int TCP_CHECK_PORT = 80;//this port is use for check connectivity

    
    private static final boolean USE_TCP_FOWARDER = true;
    
	private static CommUtil instance;
    private UDPServerTemp mUdpSer = new UDPServerTemp();
    private InetAddress mDevAddr;
    private int sendCheckValidateCount = 0;
    SocketClient socketClient = null;
    private Context context;
    CbTcpConnected callBack;
    private TcpForwarder fr;
    
    public static CommUtil getInstance() {
    	if (instance == null ) {
            instance = new CommUtil();
        }
        return instance;
    }

    public CommUtil() {
    
    }
    
    public void init (Context ctx, CbTcpConnected cb) {
    	//just do nothing
    	AppLog.i(TAG,"CommUtil init");
    	callBack = cb;
    	context = ctx;

    	this.mHandler.post(this.checkAP);


    }
    
    public void requestIFrame() {
        this.mHandler.post(this.sendRequestCmd);
    }

    private Runnable sendRequestCmd = new Runnable() { // from class: com.vison.baselibrary.base.CommUtil.8
        @Override // java.lang.Runnable
        public void run() {
        	AppLog.i(TAG,"CommUitl, requestIFrame");
        	CommUtil.this.writeUDPCmd(new byte[]{39});
        }
    };

    public void writeUDPCmd(byte[] bArr) {
        this.mUdpSer.addCmdP(bArr, this.mDevAddr, HELIWAY_REMOTE_UDP_PORT);
    }

    private Runnable sendCheckWorkCmd = new Runnable() { // from class: com.vison.baselibrary.base.CommUtil.7
        @Override // java.lang.Runnable
        public void run() {
            try {
                //InetAddress byAddress = InetAddress.getByAddress(socketClient.getHost().getBytes());
                if (-1 == PlayInfo.transMode) {
                    if (PlayInfo.udpDevType == 4) {
                        CommUtil.this.writeTCPCmd("remote\r\n");
                    } else {
                        CommUtil.this.writeTCPCmd(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 37, 37});
                    }
                } else {
                    if (-1 != PlayInfo.transMode && PlayInfo.transMode != 0) {
                        if (1 == PlayInfo.transMode) {
                            if (PlayInfo.udpDevType == 4) {
                                CommUtil.this.writeTCPCmd("remote\r\n");
                            } else {
                                //CommUtil.this.writeTCPCmd(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 37, 37});
                            }
                        }
                    }
                    CommUtil.this.mUdpSer.addCmdP(new byte[]{37}, CommUtil.this.mDevAddr, HELIWAY_REMOTE_UDP_PORT);
                }
            } catch (Exception e) {
                AppLog.e(TAG,"sendCheckWorkCmd exception:" + e.getMessage());
            }
            CommUtil.access$110(CommUtil.this);
            if (CommUtil.this.sendCheckValidateCount > 0) {
                CommUtil.this.mHandler.postDelayed(CommUtil.this.sendCheckWorkCmd, 100L);
            } else {
                CommUtil.this.mHandler.postDelayed(CommUtil.this.sendCheckWorkCmd, 1500L);
            }
        }
    };

    Runnable sendDevTime = new Runnable() { // from class: com.vison.baselibrary.base.BaseApplication.5
        @Override // java.lang.Runnable
        public void run() {
            if (PlayInfo.udpDevType == 2) {
            	CommUtil.this.udpSendTime();
            }
            CommUtil.this.mHandler.postDelayed(CommUtil.this.sendDevTime, 1000L);
        }
    };

    Runnable checkAP = new Runnable() { // from class: com.vison.baselibrary.base.BaseApplication.3
        @Override // java.lang.Runnable
        public void run() {

            boolean isLocal = "local".equals(getSharedPreference("networkType"));

            PlayInfo.udpDevType = 2;//for heliway wifi camera
            PlayInfo.transMode = 1;
            PlayInfo.deviceId = PlayInfo.DeviceId._872_720.getId();// _8626_5G_8801_8603_1080P.get"720p";
            PlayInfo.deviceType = PlayInfo.DeviceType._720P;
            PlayInfo.playType = 1;

            restartTCP(isLocal);
            if (isLocal) {
                PlayInfo.targetIpAddr = CommUtil.this.socketClient.getHost();
                PlayInfo.udpPort = CommUtil.this.socketClient.getPort();;
            } else if (!isLocal && USE_TCP_FOWARDER) {

                InetSocketAddress to = new InetSocketAddress(CommUtil.this.socketClient.getHost(),CommUtil.this.socketClient.getPort());

                try {
                    to = TcpForwarder.runFowarder(to);
                } catch (IOException e) {
                    AppLog.e(TAG,"TcpForwarder error!" + e.getMessage());
                }

                PlayInfo.targetIpAddr = to.getHostString();
                PlayInfo.udpPort = to.getPort();

                try {
                    Thread.sleep(200L);
                } catch (InterruptedException e) {

                }
            }

             CommUtil.this.mUdpSer.Stop();

            CommUtil.this.mUdpSer.setAccesser(CommUtil.this.mHandler);
            CommUtil.this.mUdpSer.Start();


            if (CommUtil.this.callBack != null) {
                CommUtil.this.callBack.cb_connected();
            }
        }
    };
   
    
    public interface CbTcpConnected {
        void cb_connected();
    }

    private String getWifiDhcpAddr() {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

        int intAddr = dhcpInfo.gateway;
        return long2ip(intAddr);
    }
    
    private String long2ip(int i) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(String.valueOf(i & 255));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((i >> 8) & 255));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((i >> 16) & 255));
        stringBuffer.append('.');
        stringBuffer.append(String.valueOf((i >> 24) & 255));
        return stringBuffer.toString();
    }


    public void udpSendTime() {
        Date date = new Date();
        int year = date.getYear() + 1900;
        int month = date.getMonth() + 1;
        int date2 = date.getDate();
        int day = date.getDay();
        int hours = date.getHours();
        int minutes = date.getMinutes();
        int seconds = date.getSeconds();
        writeUDPCmd(new byte[]{38, (byte) (year & 255), (byte) ((year & 0xff00) >> 8), (byte) ((year & 16711680) >> 16), (byte) ((year & 0xff000000) >> 24), (byte) (month & 255), (byte) ((month & 0xff00) >> 8), (byte) ((month & 16711680) >> 16), (byte) ((month & 0xff000000) >> 24), (byte) (date2 & 255), (byte) ((date2 & 0xff00) >> 8), (byte) ((date2 & 16711680) >> 16), (byte) ((date2 & 0xff000000) >> 24), (byte) (day & 255), (byte) ((day & 0xff00) >> 8), (byte) ((day & 16711680) >> 16), (byte) ((day & 0xff000000) >> 24), (byte) (hours & 255), (byte) ((hours & 0xff00) >> 8), (byte) ((hours & 16711680) >> 16), (byte) ((hours & 0xff000000) >> 24), (byte) (minutes & 255), (byte) ((minutes & 0xff00) >> 8), (byte) ((minutes & 16711680) >> 16), (byte) ((minutes & 0xff000000) >> 24), (byte) (seconds & 255), (byte) ((seconds & 0xff00) >> 8), (byte) ((seconds & 16711680) >> 16), (byte) ((seconds & 0xff000000) >> 24)});
    }

    
    public void requestVideoData(boolean z) {
        if (z) {
            this.mHandler.removeCallbacks(this.sendRequestVideo);
            this.mHandler.postDelayed(this.sendRequestVideo, 10L);
            return;
        }
        this.mHandler.removeCallbacks(this.sendRequestVideo);
    }
    private Runnable sendRequestVideo = new Runnable() { // from class: com.vison.baselibrary.base.BaseApplication.6
        @Override // java.lang.Runnable
        public void run() {
            if (PlayInfo.transMode == 0) {
                try {
                    ByteBuffer allocate = ByteBuffer.allocate(5);
                    allocate.put((byte) 8);
                    //allocate.put(CommUtil.long2byte(CommUtil.this.mIPinfo[0]));TODO
                    CommUtil.this.writeUDPCmd(allocate.array());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                CommUtil.this.mHandler.postDelayed(CommUtil.this.sendRequestVideo, 1000L);
            }
        }
    };

    
    public void writeTCPCmd(byte[] bArr) {
        AppLog.i(TAG,"heliway tcp send data len:" + bArr.length);
        if (this.socketClient != null) {
            this.socketClient.sendData(bArr);
        }
    }

    public void writeTCPCmd(String str) {
        writeTCPCmd(str.getBytes());
    }

    static /* synthetic */ int access$110(CommUtil baseApplication) {
        int i = baseApplication.sendCheckValidateCount;
        baseApplication.sendCheckValidateCount = i - 1;
        return i;
    }

    public void onUdpReceiveData(byte[] bArr) {
        String bytesToHexString = ByteUtility.bytesToHexString(bArr);
       if (bytesToHexString.startsWith("55445037323050") || bytesToHexString.startsWith("3130383050")) {
            if (bytesToHexString.startsWith("55445037323050")) {
                //PlayInfo.deviceId = "720p";
            } else {
                //PlayInfo.deviceId = "1080P";
            }
            PlayInfo.transMode = 1;
           AppLog.i(TAG,"onUdpReceiveData, devId:" + PlayInfo.deviceId);
            PlayInfo.udpDevType = 2;
            this.mHandler.post(this.sendDevTime);
            //restartTCP(true);
            //getVersion();
            
        }
    }
 
    public void onTcpReceiveData(byte[] bArr) {
       
    }

    private void restartTCP(boolean isLocal) {
    	
    	if (this.socketClient == null) {
            this.socketClient = new SocketClient();
            this.socketClient.setCallBackHandler(this.mHandler);
        }
    	
        if (this.socketClient.isConnected()) {
            AppLog.i(TAG,"restartTCP, socket already connected, just return!");
        }

        String localAddr = getWifiDhcpAddr();
    	if (isLocal && localAddr != null && localAddr.length() > 8) {
            AppLog.i(TAG,"restartTCP, run in local mode, local addr:" + localAddr);
    		
    		socketClient.setHost(localAddr);
        	socketClient.setPort(HELIWAY_LOCAL_TCP_PORT);

	        try {
				this.mDevAddr = InetAddress.getByName(localAddr);
			} catch (IOException e) {
                AppLog.e(TAG,"failed to set mDevAddr");
			}
    	} else {
    	
    		//ipv6 rc mode
	        String ip = getIpv6HostName();
	        if (ip != null && ip.length() > 4) {
	        	this.socketClient.setHost(ip);  	
	        	this.socketClient.setPort(HELIWAY_REMOTE_TCP_PORT);
	        }
	        
	        try {
				this.mDevAddr = InetAddress.getByName(ip);
			} catch (IOException e) {
                AppLog.e(TAG,"failed to set mDevAddr");
			}
    	}
    }
    
    private String getIpv6HostName() {
    	String targetHost = null;
    	Socket socket = null;
    	
    	String mac = getSharedPreference("clientId");
    	String getUrl = String.format("http://i4free.x3322.net:38086/wificar/getClientIp?mac=%s", mac);

        AppLog.i(TAG,"getURLContent: url:" + getUrl);
    	
        String ipaddr = getURLContent(getUrl);
        
        if (ipaddr != null && ipaddr.length() > 8){
            AppLog.i(TAG,"getIpv6HostName=" + ipaddr);
            return ipaddr;
        }

        return null;
    }
    
    private static String getURLContent(String url) {
        StringBuffer sb = new StringBuffer();
        AppLog.i(TAG,"getURLContent:" +url);
        try {
            URL updateURL = new URL(url);
            URLConnection conn = updateURL.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF8"));
            while (true) {
                String s = rd.readLine();
                if (s == null) {
                    break;
                }
                sb.append(s);
            }
        } catch (Exception e){

        }
        return sb.toString();
    }
    

    
    private Handler mHandler = new Handler() { // from class: com.vison.baselibrary.base.CommUtil.1
        @Override // android.os.Handler
        public void handleMessage(Message message) {
            switch (message.what) {
                case TCP_CONNECT_SUCCEED /* 2701 */:
                	CommUtil.this.sendCheckValidateCount = 10;
                	CommUtil.this.mHandler.removeCallbacks(CommUtil.this.sendCheckWorkCmd);
                	CommUtil.this.mHandler.postDelayed(CommUtil.this.sendCheckWorkCmd, 10L);
                    return;
                case TCP_RECEIVE_DATA /* 2702 */:
                	CommUtil.this.onTcpReceiveData((byte[]) message.obj);
                    return;
                case TCP_ERROR /* 2703 */:
                    //CommUtil.this.socketClient.connect();
                    return;
                case UDP_RECEIVE_DATA:
                	CommUtil.this.onUdpReceiveData((byte[]) message.obj);
                    return;
                default:
                    return;
            }
        }
    };
    
	private String getSharedPreference(String key) {
		//return AndRovio.getSharedPreference(key);
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
		return sp.getString(key, "");
	}

}
