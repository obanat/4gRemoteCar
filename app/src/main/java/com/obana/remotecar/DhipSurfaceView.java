package com.obana.remotecar;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.obana.remotecar.utils.AppLog;

import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * 雄迈/大华DHIP TCP协议视频播放视图
 * 双Socket + 双线程架构：
 *   - 控制Socket: Login(如需) + KeepAlive + OPMonitor Start
 *   - 流Socket: OPMonitor Claim + 接收H.264媒体数据
 *
 * 完整流程（兼容需要Login和不需要Login的设备）：
 *   1. 连接控制Socket → 发Claim → 如Ret=103则先Login → 再发Claim直到成功
 *   2. 连接流Socket → 发Claim → 读响应
 *   3. 控制Socket发Start → 读响应
 *   4. 流Socket循环收媒体数据
 *
 * 线程模型：
 *   connectWorker: 建立连接 + Login + 启动工作线程
 *   keepAliveWorker: 控制Socket上周期发KeepAlive + 独立读响应
 *   mediaWorker: 流Socket上发Claim → 读响应 → 触发控制Socket发Start → 循环收媒体数据
 */
public class DhipSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "DhipSurfaceView";

    private static final int DHIP_HEADER_SIZE = 20;
    private static final int DHIP_PORT = 34567;
    private static final int CONNECT_TIMEOUT = 3000;
    private static final int SOCKET_RECV_BUF = 512 * 1024;
    private static final int KEEPALIVE_INTERVAL = 5000; // ms

    // DHIP消息类型码
    private static final int MSG_LOGIN_REQ      = 0x03E8;
    private static final int MSG_KEEPALIVE_REQ   = 0x03EE;
    private static final int MSG_START_REQ       = 0x0582;
    private static final int MSG_MEDIA_DATA      = 0x0584;
    private static final int MSG_CLAIM_REQ       = 0x0585;

    // Login默认凭据
    private static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASS = "tlJwpbo6";

    private SurfaceHolder mSurfaceHolder;
    private MediaCodec mCodec;
    private int mCodecState = -1; // -1=未初始化

    // 双Socket
    private Socket mControlSocket;   // KeepAlive + Start
    private Socket mStreamSocket;    // Claim + 媒体数据接收
    private OutputStream mControlOut; // 控制Socket输出流（synchronized访问）

    private Thread mConnectThread;
    private Thread mMediaThread;
    private Thread mKeepAliveThread;
    private volatile boolean mRunning = false;

    private String mHost;
    private int mPort = DHIP_PORT;

    // SessionID（Login后设备分配新值）
    private volatile int mSessionId = 0x10; // 默认0x10，Login成功后更新

    // SPS/PPS缓存
    private byte[] mSpsData = null;
    private byte[] mPpsData = null;
    private boolean mSpsPpsReady = false;

    // 流缓冲区（跨DHIP包重组H.264码流）
    private byte[] mStreamBuf = new byte[512 * 1024];
    private int mStreamBufLen = 0;

    public DhipSurfaceView(Context context) {
        super(context);
        init();
    }

    public DhipSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DhipSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        AppLog.i(TAG, "surfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        AppLog.i(TAG, "surfaceChanged: " + width + "x" + height);
        initMediaCodec();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        AppLog.i(TAG, "surfaceDestroyed");
        stopPlayback();
    }

    private void initMediaCodec() {
        if (mCodecState > 0) return;
        try {
            MediaFormat fmt = MediaFormat.createVideoFormat("video/avc", 1920, 1080);
            mCodec = MediaCodec.createDecoderByType("video/avc");
            Surface surface = mSurfaceHolder.getSurface();
            if (surface != null && surface.isValid()) {
                mCodec.configure(fmt, surface, null, 0);
                mCodec.start();
                mCodecState = 1;
                AppLog.i(TAG, "MediaCodec initialized");
            }
        } catch (Exception e) {
            AppLog.e(TAG, "MediaCodec init failed: " + e.getMessage());
        }
    }

    /**
     * 开始播放
     */
    public void startPlayback(String host, int port) {
        if (mRunning) {
            AppLog.i(TAG, "Already running, stop first");
            stopPlayback();
        }

        mHost = host;
        mPort = port > 0 ? port : DHIP_PORT;
        AppLog.i(TAG, "Starting DHIP playback to " + mHost + ":" + mPort);

        mRunning = true;
        mSessionId = 0x10;
        mConnectThread = new Thread(this::connectWorker);
        mConnectThread.setName("DHIP-Connect");
        mConnectThread.start();
    }

    public void startPlayback(String host) {
        startPlayback(host, DHIP_PORT);
    }

    /**
     * 停止播放
     */
    public void stopPlayback() {
        mRunning = false;

        closeSocket(mStreamSocket);
        mStreamSocket = null;
        closeSocket(mControlSocket);
        mControlSocket = null;
        mControlOut = null;

        if (mMediaThread != null) {
            try { mMediaThread.join(2000); } catch (Exception ignored) {}
            mMediaThread = null;
        }
        if (mKeepAliveThread != null) {
            try { mKeepAliveThread.join(2000); } catch (Exception ignored) {}
            mKeepAliveThread = null;
        }
        if (mConnectThread != null) {
            try { mConnectThread.join(2000); } catch (Exception ignored) {}
            mConnectThread = null;
        }

        releaseCodec();
        mSpsPpsReady = false;
        mSpsData = null;
        mPpsData = null;
        mStreamBufLen = 0;
    }

    private void closeSocket(Socket s) {
        if (s != null) {
            try { s.close(); } catch (Exception ignored) {}
        }
    }

    private void releaseCodec() {
        if (mCodec != null) {
            try {
                mCodec.stop();
                mCodec.release();
            } catch (Exception e) {
                AppLog.e(TAG, "releaseCodec error: " + e.getMessage());
            }
            mCodec = null;
            mCodecState = -1;
        }
    }

    /**
     * 连接工作线程
     * 1. 建立控制Socket → 发Claim探测是否需要Login → 如果需要则Login → 再发KeepAlive确认
     * 2. 建立流Socket
     * 3. 启动keepAlive线程和media线程
     */
    private void connectWorker() {
        try {
            // === 1. 建立控制Socket ===
            mControlSocket = new Socket();
            mControlSocket.setReceiveBufferSize(SOCKET_RECV_BUF);
            mControlSocket.setTcpNoDelay(true);
            mControlSocket.setSoTimeout(10000);
            mControlSocket.connect(new InetSocketAddress(mHost, mPort), CONNECT_TIMEOUT);
            mControlOut = mControlSocket.getOutputStream();
            DataInputStream ctrlIn = new DataInputStream(mControlSocket.getInputStream());
            AppLog.i(TAG, "Control socket connected to " + mHost + ":" + mPort);

            // === 2. 在控制Socket上先发Claim探测是否需要Login ===
            String claimJson = buildClaimJson();
            sendDhipControl(MSG_CLAIM_REQ, claimJson);
            AppLog.i(TAG, "Sent probe Claim on control socket");

            int[] claimResp = readDhipResponse(ctrlIn, "Probe Claim");
            int retCode = claimResp[1]; // retCode在[1]位置

            if (retCode == 103) {
                // === 需要Login ===
                AppLog.i(TAG, "Claim returned Ret=103, need Login");

                // 发送Login
                String loginJson = buildLoginJson();
                sendDhipControl(MSG_LOGIN_REQ, loginJson);
                AppLog.i(TAG, "Sent Login request");

                int[] loginResp = readDhipResponse(ctrlIn, "Login");
                int loginRet = loginResp[1];

                if (loginRet == 100) {
                    AppLog.i(TAG, "Login successful, sessionId=0x" + Integer.toHexString(mSessionId));
                } else {
                    throw new Exception("Login failed, Ret=" + loginRet);
                }
            } else if (retCode == 100) {
                AppLog.i(TAG, "Probe Claim Ret=100, no Login needed");
            }

            // === 3. 建立流Socket ===
            mStreamSocket = new Socket();
            mStreamSocket.setReceiveBufferSize(SOCKET_RECV_BUF);
            mStreamSocket.setTcpNoDelay(true);
            mStreamSocket.setSoTimeout(10000);
            mStreamSocket.connect(new InetSocketAddress(mHost, mPort), CONNECT_TIMEOUT);
            AppLog.i(TAG, "Stream socket connected to " + mHost + ":" + mPort);

            // === 4. 启动工作线程 ===
            mKeepAliveThread = new Thread(this::keepAliveWorker);
            mKeepAliveThread.setName("DHIP-KeepAlive");
            mKeepAliveThread.start();

            mMediaThread = new Thread(this::mediaWorker);
            mMediaThread.setName("DHIP-Media");
            mMediaThread.start();

        } catch (Exception e) {
            if (mRunning) {
                AppLog.e(TAG, "connectWorker error: " + e.getMessage());
            }
            mRunning = false;
        }
    }

    /**
     * KeepAlive线程 - 在控制Socket上独立周期发送KeepAlive并读响应
     */
    private void keepAliveWorker() {
        try {
            DataInputStream ctrlIn = new DataInputStream(mControlSocket.getInputStream());

            while (mRunning && mControlSocket != null && !mControlSocket.isClosed()) {
                try {
                    String kaJson = buildKeepAliveJson();
                    sendDhipControl(MSG_KEEPALIVE_REQ, kaJson);

                    // 读取KeepAlive响应
                    byte[] header = new byte[DHIP_HEADER_SIZE];
                    ctrlIn.readFully(header);
                    int msgCode = ((header[14] & 0xFF) | ((header[15] & 0xFF) << 8));
                    int payloadLen = readPayloadLength(header);
                    AppLog.d(TAG, "KeepAlive response msg=0x" + Integer.toHexString(msgCode) +
                            " payloadLen=" + payloadLen);
                    if (payloadLen > 0 && payloadLen < 65536) {
                        byte[] discard = new byte[payloadLen];
                        ctrlIn.readFully(discard);
                    }
                } catch (java.net.SocketTimeoutException ste) {
                    AppLog.w(TAG, "KeepAlive read timeout, retrying");
                } catch (Exception e) {
                    if (mRunning) {
                        AppLog.e(TAG, "KeepAlive failed: " + e.getMessage());
                    }
                }
                Thread.sleep(KEEPALIVE_INTERVAL);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Media线程 - 在流Socket上发Claim → 读响应 → 触发Start → 循环收媒体数据
     */
    private void mediaWorker() {
        try {
            DataInputStream streamIn = new DataInputStream(mStreamSocket.getInputStream());

            // === 1. 流Socket: 发送Claim ===
            String claimJson = buildClaimJson();
            sendDhipStream(MSG_CLAIM_REQ, claimJson);
            AppLog.i(TAG, "Sent OPMonitor Claim on stream socket");

            // 读取Claim响应
            readDhipResponse(streamIn, "OPMonitor Claim");

            // === 2. Claim成功后，触发控制Socket发送Start ===
            String startJson = buildStartJson();
            sendDhipControl(MSG_START_REQ, startJson);
            AppLog.i(TAG, "Sent OPMonitor Start via control socket");

            // === 3. 清除超时，循环接收媒体数据 ===
            mStreamSocket.setSoTimeout(0);

            byte[] header = new byte[DHIP_HEADER_SIZE];
            while (mRunning) {
                streamIn.readFully(header);

                int payloadLen = readPayloadLength(header);
                int msgCode = ((header[14] & 0xFF) | ((header[15] & 0xFF) << 8));

                if (payloadLen <= 0 || payloadLen > 2 * 1024 * 1024) {
                    AppLog.w(TAG, "Invalid DHIP payload len=" + payloadLen + " msg=0x" +
                            Integer.toHexString(msgCode));
                    continue;
                }

                byte[] payload = new byte[payloadLen];
                streamIn.readFully(payload);

                if (msgCode == MSG_MEDIA_DATA) {
                    processMediaPayload(payload, payloadLen);
                } else {
                    AppLog.d(TAG, "Stream non-media msg=0x" + Integer.toHexString(msgCode) +
                            " len=" + payloadLen);
                }
            }
        } catch (Exception e) {
            if (mRunning) {
                AppLog.e(TAG, "mediaWorker error: " + e.getMessage());
            }
        } finally {
            mRunning = false;
        }
    }

    // ==================== JSON构建 ====================

    private String buildKeepAliveJson() {
        return "{\"Name\":\"KeepAlive\",\"SessionID\":\"0x" +
                String.format("%08x", mSessionId) + "\"}\n";
    }

    private String buildClaimJson() {
        return "{\"Name\":\"OPMonitor\",\"OPMonitor\":{\"Action\":\"Claim\"," +
                "\"Parameter\":{\"Channel\":0,\"CombinMode\":\"CONNECT_ALL\"," +
                "\"StreamType\":\"Main\",\"TransMode\":\"TCP\"}}," +
                "\"SessionID\":\"0x" + String.format("%08x", mSessionId) + "\"}\n";
    }

    private String buildStartJson() {
        return "{\"Name\":\"OPMonitor\",\"OPMonitor\":{\"Action\":\"Start\"," +
                "\"Parameter\":{\"Channel\":0,\"CombinMode\":\"CONNECT_ALL\"," +
                "\"StreamType\":\"Main\",\"TransMode\":\"TCP\"}}," +
                "\"SessionID\":\"0x" + String.format("%08x", mSessionId) + "\"}\n";
    }

    private String buildLoginJson() {
        return "{\"EncryptType\":\"MD5\",\"LoginType\":\"DVRIP-Web\"," +
                "\"UserName\":\"" + DEFAULT_USER + "\"," +
                "\"PassWord\":\"" + DEFAULT_PASS + "\"," +
                "\"ClientID\":\"android-" + Integer.toHexString((int)(System.currentTimeMillis() & 0xFFFF)) + "\"}\n";
    }

    // ==================== DHIP协议 ====================

    /**
     * 通过控制Socket发送DHIP消息（synchronized保护OutputStream）
     */
    private void sendDhipControl(int msgCode, String payload) throws Exception {
        synchronized (mControlOut) {
            sendDhipRaw(mControlOut, msgCode, payload);
        }
    }

    /**
     * 通过流Socket发送DHIP消息
     */
    private void sendDhipStream(int msgCode, String payload) throws Exception {
        sendDhipRaw(mStreamSocket.getOutputStream(), msgCode, payload);
    }

    /**
     * 写入DHIP原始数据到指定OutputStream
     * 头部20字节格式（参考抓包）:
     *   [0]     = 0xFF (magic)
     *   [1]     = 0x00 (direction: request)
     *   [2-3]   = 0x0000
     *   [4-7]   = sessionId (LE uint32)
     *   [8-13]  = 0x0000
     *   [14-15] = msgCode (LE uint16)
     *   [16-19] = payload length (LE uint32)
     */
    private void sendDhipRaw(OutputStream out, int msgCode, String payload) throws Exception {
        byte[] payloadBytes = payload.getBytes("UTF-8");
        int totalLen = DHIP_HEADER_SIZE + payloadBytes.length;
        byte[] packet = new byte[totalLen];

        packet[0] = (byte) 0xFF;
        packet[1] = 0x00;
        // bytes 2-3: 0x0000
        // bytes 4-7: sessionId LE
        packet[4] = (byte) (mSessionId & 0xFF);
        packet[5] = (byte) ((mSessionId >> 8) & 0xFF);
        packet[6] = (byte) ((mSessionId >> 16) & 0xFF);
        packet[7] = (byte) ((mSessionId >> 24) & 0xFF);
        // bytes 8-13: 0x0000
        // bytes 14-15: msgCode (LE uint16)
        packet[14] = (byte) (msgCode & 0xFF);
        packet[15] = (byte) ((msgCode >> 8) & 0xFF);
        // bytes 16-19: payload length (LE uint32)
        packet[16] = (byte) (payloadBytes.length & 0xFF);
        packet[17] = (byte) ((payloadBytes.length >> 8) & 0xFF);
        packet[18] = (byte) ((payloadBytes.length >> 16) & 0xFF);
        packet[19] = (byte) ((payloadBytes.length >> 24) & 0xFF);

        System.arraycopy(payloadBytes, 0, packet, DHIP_HEADER_SIZE, payloadBytes.length);

        out.write(packet);
        out.flush();

        // hex dump for debugging
        StringBuilder hexDump = new StringBuilder();
        for (int i = 0; i < Math.min(packet.length, 60); i++) {
            hexDump.append(String.format("%02X ", packet[i] & 0xFF));
        }
        AppLog.i(TAG, "Sent DHIP msg=0x" + Integer.toHexString(msgCode) + " sessionId=0x"
                + Integer.toHexString(mSessionId) + " len=" + payloadBytes.length
                + " hex=" + hexDump.toString());
    }

    /**
     * 从DHIP头部读取payload长度(LE uint32 @ bytes 16-19)
     */
    private int readPayloadLength(byte[] header) {
        return (header[16] & 0xFF) |
                ((header[17] & 0xFF) << 8) |
                ((header[18] & 0xFF) << 16) |
                ((header[19] & 0xFF) << 24);
    }

    /**
     * 从DHIP头部读取sessionId(LE uint32 @ bytes 4-7)
     */
    private int readSessionId(byte[] header) {
        return (header[4] & 0xFF) |
                ((header[5] & 0xFF) << 8) |
                ((header[6] & 0xFF) << 16) |
                ((header[7] & 0xFF) << 24);
    }

    /**
     * 读取DHIP响应，返回[msgCode, retCode, payloadLen]
     * 同时从Login响应中更新SessionID
     */
    private int[] readDhipResponse(DataInputStream in, String label) throws Exception {
        byte[] header = new byte[DHIP_HEADER_SIZE];
        in.readFully(header);
        int msgCode = ((header[14] & 0xFF) | ((header[15] & 0xFF) << 8));
        int payloadLen = readPayloadLength(header);

        String body = "";
        if (payloadLen > 0 && payloadLen < 65536) {
            byte[] respPayload = new byte[payloadLen];
            in.readFully(respPayload);
            body = new String(respPayload, "UTF-8").trim();
        }

        AppLog.i(TAG, label + " response msg=0x" + Integer.toHexString(msgCode) +
                " payloadLen=" + payloadLen + " body=" + body);

        // 提取Ret值
        int retCode = -1;
        String retStr = extractJsonString(body, "Ret");
        if (retStr != null) {
            try {
                retCode = Integer.parseInt(retStr);
            } catch (NumberFormatException e) {
                AppLog.w(TAG, "Failed to parse Ret: " + retStr);
            }
        }

        // 从Login响应中提取并更新SessionID
        if (msgCode == (MSG_LOGIN_REQ + 1)) { // Login响应 msg=0x03E9
            String sessionIdStr = extractJsonString(body, "SessionID");
            if (sessionIdStr != null) {
                try {
                    mSessionId = (int) Long.parseLong(sessionIdStr.replace("0x", ""), 16);
                    AppLog.i(TAG, "Updated sessionId to 0x" + Integer.toHexString(mSessionId));
                } catch (NumberFormatException e) {
                    AppLog.w(TAG, "Failed to parse SessionID: " + sessionIdStr);
                }
            }
        }

        return new int[]{msgCode, retCode, payloadLen};
    }

    /**
     * 从JSON字符串中提取指定key的值
     */
    private String extractJsonString(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIdx = json.indexOf(searchKey);
        if (keyIdx < 0) return null;

        // 找到冒号后的值
        int colonIdx = json.indexOf(':', keyIdx + searchKey.length());
        if (colonIdx < 0) return null;

        // 跳过空白
        int valStart = colonIdx + 1;
        while (valStart < json.length() && json.charAt(valStart) == ' ') valStart++;

        if (valStart >= json.length()) return null;

        if (json.charAt(valStart) == '"') {
            // 字符串值
            int valEnd = json.indexOf('"', valStart + 1);
            if (valEnd < 0) return null;
            return json.substring(valStart + 1, valEnd);
        } else {
            // 数值
            int valEnd = valStart;
            while (valEnd < json.length() && json.charAt(valEnd) != ',' && json.charAt(valEnd) != '}') {
                valEnd++;
            }
            return json.substring(valStart, valEnd).trim();
        }
    }

    // ==================== 媒体数据处理 ====================

    /**
     * 处理DHIP媒体数据payload (msg=0x0584)
     *
     * 正常抓包(vms-stream-ok.txt)分析:
     *   DHIP payload固定8192字节(0x2000)，是H.264码流的一个分片，完整帧跨多个DHIP包
     *   码流中混合标准H.264 NALU和大华私有标记:
     *     - 00 00 01 fc / 00 00 00 01 fc  = I帧私有标记 (NAL type 28, forbidden=1)
     *     - 00 00 01 fd / 00 00 00 01 fd  = P帧私有标记 (NAL type 29, forbidden=1)
     *     - 00 00 00 01 67 = SPS, 68 = PPS, 06 = SEI, 65 = IDR, 61 = non-IDR
     *
     * 策略: 将所有payload累积到流缓冲区，搜索 00 00 01 / 00 00 00 01 起始码，
     *       按NAL类型提取标准H.264 NALU送解码器，跳过私有NAL(fc=28/fd=29)
     */
    private void processMediaPayload(byte[] data, int length) {
        // 追加到流缓冲区
        if (mStreamBufLen + length > mStreamBuf.length) {
            byte[] newBuf = new byte[Math.max(mStreamBuf.length * 2, mStreamBufLen + length)];
            System.arraycopy(mStreamBuf, 0, newBuf, 0, mStreamBufLen);
            mStreamBuf = newBuf;
        }
        System.arraycopy(data, 0, mStreamBuf, mStreamBufLen, length);
        mStreamBufLen += length;

        // 查找所有起始码位置 (00 00 01 和 00 00 00 01)
        ArrayList<Integer> scPos = new ArrayList<>();
        for (int i = 0; i <= mStreamBufLen - 3; i++) {
            if (mStreamBuf[i] == 0 && mStreamBuf[i + 1] == 0) {
                if (mStreamBuf[i + 2] == 1) {
                    scPos.add(i); // 3字节起始码 00 00 01
                } else if (mStreamBuf[i + 2] == 0 && i <= mStreamBufLen - 4 && mStreamBuf[i + 3] == 1) {
                    scPos.add(i); // 4字节起始码 00 00 00 01
                    i++; // 跳过额外的0
                }
            }
        }

        if (scPos.size() < 2) return; // 至少需要2个起始码才能确定一个完整NALU

        // 处理每个完整NALU (从起始码i到起始码i+1，最后一个起始码后的NALU不完整)
        for (int i = 0; i < scPos.size() - 1; i++) {
            int start = scPos.get(i);
            int end = scPos.get(i + 1);

            // 确定NAL头字节位置 (3字节或4字节起始码之后)
            int nalHeaderIdx = (mStreamBuf[start + 2] == 1) ? start + 3 : start + 4;
            if (nalHeaderIdx >= mStreamBufLen) continue;

            int nalType = mStreamBuf[nalHeaderIdx] & 0x1F;

            // 跳过大华私有NAL类型 (fc=28, fd=29)
            if (nalType >= 28) continue;

            int naluSize = end - start;

            switch (nalType) {
                case 7: // SPS
                    mSpsData = new byte[naluSize];
                    System.arraycopy(mStreamBuf, start, mSpsData, 0, naluSize);
                    if (mPpsData != null) mSpsPpsReady = true;
                    AppLog.d(TAG, "SPS extracted: " + naluSize + " bytes");
                    break;
                case 8: // PPS
                    mPpsData = new byte[naluSize];
                    System.arraycopy(mStreamBuf, start, mPpsData, 0, naluSize);
                    if (mSpsData != null) mSpsPpsReady = true;
                    AppLog.d(TAG, "PPS extracted: " + naluSize + " bytes");
                    break;
                case 5: // IDR (I帧)
                    if (mSpsData != null && mPpsData != null) {
                        mSpsPpsReady = true;
                        feedToDecoder(mSpsData, 0, mSpsData.length);
                        feedToDecoder(mPpsData, 0, mPpsData.length);
                    }
                    feedToDecoder(mStreamBuf, start, naluSize);
                    break;
                case 1: // non-IDR (P帧slice)
                    if (mSpsPpsReady) {
                        feedToDecoder(mStreamBuf, start, naluSize);
                    }
                    break;
                case 6: // SEI
                    if (mSpsPpsReady) {
                        feedToDecoder(mStreamBuf, start, naluSize);
                    }
                    break;
            }
        }

        // 保留最后一个起始码之后的数据（不完整NALU，等后续数据到达再处理）
        int keepFrom = scPos.get(scPos.size() - 1);
        int remaining = mStreamBufLen - keepFrom;
        if (keepFrom > 0 && remaining > 0) {
            System.arraycopy(mStreamBuf, keepFrom, mStreamBuf, 0, remaining);
        }
        mStreamBufLen = remaining;
    }

    /**
     * 将H.264 NALU送入MediaCodec解码
     */
    private void feedToDecoder(byte[] data, int offset, int length) {
        if (mCodec == null || mCodecState <= 0) {
            initMediaCodec();
            if (mCodecState <= 0) return;
        }

        try {
            int inputIndex = mCodec.dequeueInputBuffer(10000);
            if (inputIndex >= 0) {
                ByteBuffer inputBuf = mCodec.getInputBuffer(inputIndex);
                inputBuf.clear();
                inputBuf.put(data, offset, length);
                long pts = System.nanoTime() / 1000; // 微秒，单调递增
                mCodec.queueInputBuffer(inputIndex, 0, length, pts, 0);
            }

            // 排出所有已解码帧
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int outputIndex = mCodec.dequeueOutputBuffer(info, 0);
            while (outputIndex >= 0) {
                mCodec.releaseOutputBuffer(outputIndex, true);
                outputIndex = mCodec.dequeueOutputBuffer(info, 0);
            }
        } catch (Exception e) {
            AppLog.e(TAG, "Decode error: " + e.getMessage());
        }
    }

    public boolean isPlaying() {
        return mRunning;
    }
}
