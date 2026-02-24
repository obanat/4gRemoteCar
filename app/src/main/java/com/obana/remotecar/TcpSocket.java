package com.obana.remotecar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.obana.remotecar.utils.AppLog;
import com.obana.remotecar.utils.ByteUtility;
import com.obana.remotecar.utils.Constant;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

import javax.net.SocketFactory;

public class TcpSocket {
    private static final String TAG = "TcpSocket";
    private static final int MEDIA_BUF_SIZE = (640*1024);
    private static final int SUCCESS = 1;
    private static final int FAILED = 0;
    private static final int SOCKET_TIMEOUT_MS = 3000;

    private static final int DEF_P2P_PORT = 28000;

    private static final String SP_KEY_MAC= "clientId";
    private static final String SP_KEY_LOCAL_IP= "serverIp";
    private static final String SP_KEY_LOCAL_PORT= "serverPort";
    private static final String SP_KEY_NETWORK_TYPE= "networkType";
    private static final String SP_KEY_NET_TYPE= "netType";

    private static final String SP_KEY_CONTROL_TYPE= "controlType";
    private static final int CONTROL_TYPE_NONE = 0;
    private static final int CONTROL_TYPE_MEDIA = 2;
    private static final int CONTROL_TYPE_CMD = 1;
    private static final int CONTROL_TYPE_BOTH = 3;

    private static final String DEF_LOCAL_IP ="192.168.10.1";
    private static final int DEF_LOCAL_PORT =28000;
    private static final int DEF_MJPEF_PORT =28080;
    private static final String DEF_REDIS_HOST= "i4free.x3322.net";
    private static final int DEF_REDIS_PORT = 38086;
    private DataInputStream mediaRecvStream;
    private DataInputStream cmdRecvStream;
    private Socket mediaSocket;//tcp socket
    private DatagramSocket mediaUdpSocket;//udp socket
    private Socket cmdSocket;
    private byte[] mediaBuffer = new byte[MEDIA_BUF_SIZE];
    private byte[] cmdBuffer = new byte[256];
    private Thread mediaRecvThread;
    private Thread cmdRecvThread;

    private String targetHost = DEF_LOCAL_IP;
    private int targetCmdPort = 0;
    private int targetMediaPort = 0;

    private int mJpegPort = DEF_MJPEF_PORT;
    private Context context;
    boolean bMediaConnected = false;
    boolean bCmdConnected = false;
    boolean mediaThreadBool = false;
    boolean cmdThreadBool = false;

    boolean bWriteToFile = false;

    public TcpSocket(Context ctx){
        context = ctx;
    }

    public int connect() {
        if (bCmdConnected) {
            AppLog.i(TAG, "--->alreay connect cmd socket, just return");
            return SUCCESS;
        }

        //use local host & port
        targetHost = getSharedPreference(SP_KEY_LOCAL_IP, DEF_LOCAL_IP);
        String strPort = getSharedPreference(SP_KEY_LOCAL_PORT, Integer.toString(DEF_LOCAL_PORT));
        int intPort = Integer.parseInt(strPort);
        targetCmdPort = intPort > 0 ? intPort : DEF_LOCAL_PORT;
        targetMediaPort = targetCmdPort+1;
        boolean isTcp = "0".equals(getSharedPreference(SP_KEY_NET_TYPE, "0"));

        if ("p2p".equalsIgnoreCase(getSharedPreference(SP_KEY_NETWORK_TYPE, ""))){
            targetHost = getIpv6HostName();
            targetCmdPort = DEF_P2P_PORT;
            targetMediaPort = targetCmdPort+1;
        }
        String strType = getSharedPreference(SP_KEY_CONTROL_TYPE, "3");
        int controlType = Integer.parseInt(strType);
        AppLog.i(TAG, "controlType:" + controlType);

        if (controlType == CONTROL_TYPE_BOTH || controlType == CONTROL_TYPE_CMD) {
            if (bCmdConnected) {
                AppLog.i(TAG, "--->alreay connect cmd socket, do nothing");
            } else {
                AppLog.i(TAG, "--->cmd socket creating .....host:" + targetHost + " port:" + targetCmdPort);
                createCmdSocket(targetHost, targetCmdPort);
            }
            if(!cmdSocket.isConnected()){
                AppLog.e(TAG, "--->cmd socket status error, just return!");
            }
            startCmdTcpReceiver(cmdSocket);
        }

        /// ================================================////

        if (controlType == CONTROL_TYPE_BOTH || controlType == CONTROL_TYPE_MEDIA) {
            if (bMediaConnected) {
                AppLog.i(TAG, "--->alreay connect media socket, do nothing");
            } else {
                AppLog.i(TAG, "--->media socket creating .....host:" + targetHost + " port:" + targetMediaPort);
                if (isTcp) {
                    createMediaReceiverSocket(targetHost, targetMediaPort);
                } else {
                    createMediaUdpSocket();
                }
            }

            if (isTcp && !mediaSocket.isConnected()) {
                AppLog.e(TAG, "--->media socket status error, just return!");
                return FAILED;
            } else if (!isTcp) {
                startMediaUdpReceiver(mediaUdpSocket, targetHost, targetMediaPort);
            } else {
                startMediaTcpReceiver(mediaSocket);
            }
        }
        return SUCCESS;
    }

    public boolean isConnected(){
        return bCmdConnected;
    }

    public String getJpegMediaHost() {
        if ("p2p".equalsIgnoreCase(getSharedPreference(SP_KEY_NETWORK_TYPE, ""))) {
            String url = String.format("[%s]", targetHost);
            return url;
        } else {
            return targetHost;
        }
    }

    public int getmJpegPort() {
        return mJpegPort;
    }
    private void createCmdSocket(String host, int port) {
        try {
            cmdSocket = SocketFactory.getDefault().createSocket();
            InetSocketAddress addr = new InetSocketAddress(host, port);
            cmdSocket.setReceiveBufferSize(256);
            cmdSocket.connect(addr, SOCKET_TIMEOUT_MS);
        } catch (IOException e) {
            AppLog.e(TAG, "--->cmd socket connected exception! e:" + e.getMessage());
            return;
        }
        AppLog.i(TAG, "--->cmd socket connected successfully!.....");
    }
    private void createMediaReceiverSocket(String host, int port) {
        try {
            mediaSocket = SocketFactory.getDefault().createSocket();
            InetSocketAddress addr = new InetSocketAddress(host, port);
            mediaSocket.setReceiveBufferSize(MEDIA_BUF_SIZE);
            mediaSocket.connect(addr, SOCKET_TIMEOUT_MS);
        } catch (IOException e) {
            AppLog.e(TAG, "--->media socket connected exception! e:" + e.getMessage());
            return;
        }
        AppLog.i(TAG, "--->media socket connected successfully!.....");
    }

    private void createMediaUdpSocket(){
        try {
            mediaUdpSocket = new DatagramSocket();
        } catch (SocketException e) {
            AppLog.e(TAG, "--->media udp socket status error, just return!" + e.getMessage());
            return;
        }
    }
    private String getSharedPreference(String key, String def) {
        //return AndRovio.getSharedPreference(key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(key, def);
    }

    private int getIntSharedPreference(String key, Integer def) {
        //return AndRovio.getSharedPreference(key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(key, def);
    }

    public String getIpv6HostName() {
        String url ;
        String clientId = getSharedPreference(SP_KEY_MAC, "mavic");

        url = String.format("http://%s:%d/wificar/getClientIp?mac=%s", DEF_REDIS_HOST, DEF_REDIS_PORT,clientId);
        AppLog.i(TAG, "wificar server url:" + url);
        String ipaddr = getURLContent(url);
        AppLog.i(TAG, "ip v6 addr:" + ipaddr);
        return ipaddr;
    }

    private static String getURLContent(String url) {
        StringBuffer sb = new StringBuffer();

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

    public DataInputStream getCmdRecvStream() {
        return cmdRecvStream;
    }

    public OutputStream getCmdOutputStream() throws IOException {
        if (bCmdConnected) {
            if (cmdSocket.isConnected()){
                if (cmdSocket.getOutputStream() == null){

                } else {
                    return cmdSocket.getOutputStream();
                }
            }
        }
        return null;
    }
    public void startWriteToFile(boolean write) {
        bWriteToFile = write;
    }

    public void sendCmd(String data){
        try {
            DataOutputStream out = new DataOutputStream(getCmdOutputStream());
            if (out != null) {
                out.writeChars(data);
                out.flush();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public void sendCmd(int intCmd){
        String strCmd;
        switch (intCmd) {
            case Constant.CMD_MOVE_UP:
                strCmd = "MO11\n";
                break;
            case Constant.CMD_MOVE_DOWN:
                strCmd = "MO22\n";
                break;
            case Constant.CMD_MOVE_LEFT:
                strCmd = "MO10\n";
                break;
            case Constant.CMD_MOVE_RIGHT:
                strCmd = "MO01\n";
                break;
            case Constant.CMD_MOVE_STOP:
                strCmd = "MO00\n";
                break;
            case Constant.CMD_CAMERA_UP:
                strCmd = "CV11";
                break;
            case Constant.CMD_CAMERA_DOWN:
                strCmd = "CV22";
                break;
            case Constant.CMD_CAMERA_STOP:
                strCmd = "CV00";
                break;
            default:
                strCmd = "NON";
                break;
        }
        try {
            DataOutputStream out = new DataOutputStream(getCmdOutputStream());
            if (out != null) {
                //out.writeUTF(strCmd);
                out.write(strCmd.getBytes());
                out.flush();
                AppLog.i(TAG, "send cmd:" + strCmd);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void startMediaUdpReceiver(DatagramSocket socket, String addr, int port) {
        mediaThreadBool = true;
        InetAddress udpAddress = null;

        if (socket == null) return;

        new Thread(() -> {
            try {
                byte[] buffer = new byte[32767];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                while (mediaThreadBool) {
                    // 1. 接收服务器响应
                    socket.receive(responsePacket);
                    int i = responsePacket.getLength();
                    AppLog.i(TAG,"recv 1 udp package, len:" + i);

                    if(i == -1) {
                        AppLog.e(TAG, "media receive length error!, just exit thread!");
                        break;
                    }

                    MainActivity activity = (MainActivity)context;
                    activity.drawH264View(ByteUtility.arrayCopy(responsePacket.getData(), 0, i), i);

                    if (bWriteToFile) activity.writeH264File(ByteUtility.arrayCopy(responsePacket.getData(), 0, i), i);

                    try {
                        //Thread.sleep(5L);
                    } catch(Exception e) {
                    }
                }
            } catch (Exception e) {
                AppLog.e(TAG,"recv&send error:" + e.getMessage());
            }
        }).start();

        try {
            udpAddress = InetAddress.getByName(addr);
            byte[] payload = {1,0,1,0};//fake data

            DatagramPacket sendPacket = new DatagramPacket(
                    payload,
                    payload.length,
                    udpAddress,
                    port);
            mediaUdpSocket.send(sendPacket);
        } catch (Exception e) {
            AppLog.e(TAG, "media socket send error!, just exit thread!" + e.getMessage());
            return;
        }

        bMediaConnected = true;
        AppLog.i(TAG, "media receive thread success!");
    }

    private void startMediaTcpReceiver(Socket socket) {
        try {
            mediaRecvStream = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            AppLog.e(TAG, "--->media socket connect failed!");
            return;
        }

        mediaThreadBool = true;
        mediaRecvThread = new Thread(new Runnable() {
            public void run() {
                int i;
                do {
                    try {
                        i = mediaRecvStream.read(mediaBuffer);
                        AppLog.i(TAG, "Receive media socket data, length:" + i);
                    } catch(IOException ioexception) {
                        AppLog.e(TAG, "media receive loop io exception!, just exit thread!");
                        break;
                    }
                    if(i == -1) {
                        AppLog.e(TAG, "media receive length error!, just exit thread!");
                        break;
                    }

                    MainActivity activity = (MainActivity)context;
                    activity.drawH264View(ByteUtility.arrayCopy(mediaBuffer, 0, i), i);

                    if (bWriteToFile) activity.writeH264File(ByteUtility.arrayCopy(mediaBuffer, 0, i), i);

                    try {
                        Thread.sleep(5L);
                    } catch(Exception e) {
                    }
                } while(mediaThreadBool);

            }
        });
        mediaRecvThread.setName("Media Thread");
        mediaRecvThread.start();
        bMediaConnected = true;
        AppLog.i(TAG, "media receive thread success!");
    }

    private void startCmdTcpReceiver(Socket socket) {
        try {
            cmdRecvStream = new DataInputStream(socket.getInputStream());
        } catch (Exception e) {
            AppLog.e(TAG, "--->cmd DataInputStream create failed!");
        }

        cmdThreadBool = true;
        cmdRecvThread = new Thread(new Runnable() {
            public void run() {
                int i;
                do {
                    try {
                        i = cmdRecvStream.read(cmdBuffer);
                        AppLog.i(TAG, "Receive cmd socket data, length:" + i);
                    } catch(Exception ioexception) {
                        AppLog.e(TAG, "cmd loop io exception!, just exit thread!");
                        break;
                    }
                    if(i == -1) {
                        AppLog.e(TAG, "cmd receive length error!, just exit thread!");
                        break;
                    }

                    MainActivity activity = (MainActivity)context;
                    activity.handleCmd(ByteUtility.arrayCopy(cmdBuffer, 0, i), i);

                    try {
                        Thread.sleep(5L);
                    } catch(Exception e) {
                    }
                } while(cmdThreadBool);

            }
        });
        cmdRecvThread.setName("CMD Thread");
        cmdRecvThread.start();
        bCmdConnected = true;
        AppLog.i(TAG, "cmd receive thread success!");
    }
}
