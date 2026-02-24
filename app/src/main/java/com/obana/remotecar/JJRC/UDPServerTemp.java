package com.obana.remotecar.JJRC;


import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class UDPServerTemp {
    public static final String TAG = "UDPServerTemp";
    public static final int UDP_RECEIVE_DATA = 10001;
    public static final int UDP_SERVER_INIT = 10002;
    private Handler mAccesser;
    private Thread mReceiveThread;
    private DatagramSocket mUDPSocket;
    private Thread mWriteThread;
    private boolean isWork = true;
    private Handler mHandler = new Handler();
    private List<DatagramPacket> mCmdPool = new ArrayList();
    Runnable runUDPReceiveThread = new Runnable() { // from class: com.vison.baselibrary.connect.UDPServerTemp.1
        @Override // java.lang.Runnable
        public void run() {
            UDPServerTemp.this.mReceiveThread = new Thread(new UDPReceiveThread());
            UDPServerTemp.this.mReceiveThread.start();
        }
    };
    Runnable runUDPWriteThread = new Runnable() { // from class: com.vison.baselibrary.connect.UDPServerTemp.2
        @Override // java.lang.Runnable
        public void run() {
            UDPServerTemp.this.mWriteThread = new Thread(new UDPWriteThread());
            UDPServerTemp.this.mWriteThread.start();
        }
    };

    public void Start() {
        this.isWork = true;
        StartServer();
        this.mReceiveThread = new Thread(new UDPReceiveThread());
        this.mReceiveThread.start();
        this.mWriteThread = new Thread(new UDPWriteThread());
        this.mWriteThread.start();
    }

    private void StartServer() {
        try {
            this.mUDPSocket = new DatagramSocket();
        } catch (IOException e) {
            this.isWork = false;
            System.out.print(e.toString());
        }
    }

    public boolean isRun() {
        return (this.mUDPSocket == null || this.mUDPSocket.isClosed()) ? false : true;
    }

    public void Stop() {
        this.isWork = false;
        this.mHandler.removeCallbacks(this.runUDPReceiveThread);
        this.mHandler.removeCallbacks(this.runUDPWriteThread);
        if (this.mReceiveThread != null) {
            this.mReceiveThread.interrupt();
            this.mReceiveThread = null;
        }
        if (this.mWriteThread != null) {
            this.mWriteThread.interrupt();
            this.mWriteThread = null;
        }
        if (this.mUDPSocket != null) {
            this.mUDPSocket.close();
            this.mUDPSocket = null;
        }
    }

    /* loaded from: classes.dex */
    class UDPReceiveThread implements Runnable {
        UDPReceiveThread() {
        }

        @Override // java.lang.Runnable
        public void run() {
            byte[] bArr = new byte[262144];
            DatagramPacket datagramPacket = new DatagramPacket(bArr, bArr.length);
            if (UDPServerTemp.this.mAccesser != null) {
                Message message = new Message();
                message.what = 10002;
                UDPServerTemp.this.mAccesser.sendMessage(message);
            }
            while (UDPServerTemp.this.isWork) {
                try {
                    UDPServerTemp.this.mUDPSocket.receive(datagramPacket);
                    byte[] bArr2 = new byte[datagramPacket.getLength()];
                    System.arraycopy(datagramPacket.getData(), 0, bArr2, 0, bArr2.length);
                    if (UDPServerTemp.this.mAccesser != null) {
                        Message message2 = new Message();
                        message2.obj = bArr2;
                        message2.what = 10001;
                        UDPServerTemp.this.mAccesser.sendMessage(message2);
                    }
                } catch (Exception e) {
                    UDPServerTemp.this.mHandler.postDelayed(UDPServerTemp.this.runUDPReceiveThread, 1000L);
                    System.out.print(e.toString());
                    return;
                }
            }
        }
    }

    /* loaded from: classes.dex */
    class UDPWriteThread implements Runnable {
        UDPWriteThread() {
        }

        @Override // java.lang.Runnable
        public void run() {
            while (UDPServerTemp.this.isWork) {
                try {
                    if (UDPServerTemp.this.mCmdPool.size() > 0) {
                        UDPServerTemp.this.mUDPSocket.send((DatagramPacket) UDPServerTemp.this.mCmdPool.get(0));
                        UDPServerTemp.this.mCmdPool.remove(0);
                    }
                    Thread.sleep(1L);
                } catch (Exception e) {
                    UDPServerTemp.this.mHandler.postDelayed(UDPServerTemp.this.runUDPWriteThread, 1000L);
                    System.out.print(e.toString());
                    return;
                }
            }
        }
    }

    public void setAccesser(Handler handler) {
        this.mAccesser = handler;
    }

    public void addCmdP(byte[] bArr, InetAddress inetAddress, int i) {
        if (this.mUDPSocket == null || inetAddress == null || bArr == null || bArr.length <= 0) {
            return;
        }
        this.mCmdPool.add(new DatagramPacket(bArr, bArr.length, inetAddress, i));
    }
}
