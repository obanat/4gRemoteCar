package com.obana.remotecar.JJRC;

import android.os.Handler;
import android.os.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class SocketClient extends Socket {
    public static final int TCP_CONNECT_SUCCEED = 2701;
    public static final int TCP_ERROR = 2703;
    public static final int TCP_RECEIVE_DATA = 2702;
    
    public static final boolean CHECK_TCP = true;
    
    private Handler callBackHandler;
    private String host;
    private InputStream inputStream;
    private Socket mSocket;
    private OutputStream outputStream;
    private int port;
    private boolean connect = false;
    private List<byte[]> writeBytes = new ArrayList();

    /* JADX WARN: Type inference failed for: r0v1, types: [com.vison.baselibrary.connect.SocketClient$1] */
    public void connect() {
        this.writeBytes.clear();
        new Thread() { // from class: com.vison.baselibrary.connect.SocketClient.1
            @Override // java.lang.Thread, java.lang.Runnable
            public void run() {
                super.run();
                SocketClient.this.close();
                try {
                    SocketClient.this.mSocket = new Socket(SocketClient.this.host, SocketClient.this.port);
                    SocketClient.this.outputStream = SocketClient.this.mSocket.getOutputStream();
                    SocketClient.this.inputStream = SocketClient.this.mSocket.getInputStream();
                    if (SocketClient.this.callBackHandler != null) {
                        SocketClient.this.callBackHandler.removeMessages(SocketClient.TCP_ERROR);
                        SocketClient.this.callBackHandler.sendEmptyMessage(SocketClient.TCP_CONNECT_SUCCEED);
                    }
                    SocketClient.this.connect = true;
                    new WriteThread().start();
                    while (SocketClient.this.connect) {
                        if (CHECK_TCP) {
                            SocketClient.this.callBackHandler.removeMessages(SocketClient.TCP_ERROR);
                            SocketClient.this.callBackHandler.sendEmptyMessageDelayed(SocketClient.TCP_ERROR, 10000L);
                        }
                        byte[] bArr = new byte[1048576];
                        int read = SocketClient.this.inputStream.read(bArr);
                        if (SocketClient.this.callBackHandler != null && read > 0) {
                            byte[] bArr2 = new byte[read];
                            System.arraycopy(bArr, 0, bArr2, 0, read);
                            Message message = new Message();
                            message.obj = bArr2;
                            message.what = SocketClient.TCP_RECEIVE_DATA;
                            SocketClient.this.callBackHandler.sendMessage(message);
                        }
                    }
                } catch (IOException unused) {
                    SocketClient.this.connect = false;
                    SocketClient.this.close();
                    if (SocketClient.this.callBackHandler != null) {
                        SocketClient.this.callBackHandler.removeMessages(SocketClient.TCP_ERROR);
                        SocketClient.this.callBackHandler.sendEmptyMessageDelayed(SocketClient.TCP_ERROR, 5000L);
                    }
                }
            }
        }.start();
    }

    @Override // java.net.Socket, java.io.Closeable, java.lang.AutoCloseable
    public void close() {
        this.writeBytes.clear();
        try {
            if (this.mSocket != null) {
                this.mSocket.close();
                this.mSocket = null;
            }
            if (this.inputStream != null) {
                this.inputStream.close();
            }
            if (this.outputStream != null) {
                this.outputStream.close();
            }
        } catch (Exception unused) {
        }
    }

    public void sendData(byte[] bArr) {
        this.writeBytes.add(bArr);
    }

    public void setHost(String str) {
        this.host = str;
    }

    public void setPort(int i) {
        this.port = i;
    }
    public String getHost() {
        return this.host;
    }

    public int getPort() {
        return this.port;
    }
    public void setCallBackHandler(Handler handler) {
        this.callBackHandler = handler;
    }

    /* loaded from: classes.dex */
    class WriteThread extends Thread {
        WriteThread() {
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            super.run();
            while (SocketClient.this.connect) {
                if (!SocketClient.this.writeBytes.isEmpty() && SocketClient.this.writeBytes.size() > 0) {
                    try {
                        SocketClient.this.outputStream.write((byte[]) SocketClient.this.writeBytes.get(0));
                        SocketClient.this.outputStream.flush();
                        SocketClient.this.writeBytes.remove(0);
                    } catch (Exception unused) {
                    }
                }
            }
            SocketClient.this.writeBytes.clear();
        }
    }
}