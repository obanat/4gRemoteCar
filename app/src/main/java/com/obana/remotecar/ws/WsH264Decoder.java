package com.obana.remotecar.ws;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * H.264 实时解码器 — WebSocket 模式
 *
 * 两阶段工作模式:
 *   Phase 1 (初始配置): 扫描 WS chunk 中的 SPS/PPS，配置解码器 (csd-0/csd-1)
 *   Phase 2 (正常流式): 原始 chunk 直透传到 MediaCodec，零处理开销
 */
public class WsH264Decoder {

    private static final String TAG = "WsH264Decoder";
    private static final int MAX_QUEUE = 30;

    // 核心状态
    private final Surface surface;
    private MediaCodec codec;
    private volatile boolean isRunning;
    private volatile boolean decoderReady;

    // SPS/PPS 扫描 (仅初始配置阶段使用)
    private byte[] scanResidue;
    private byte[] cachedSPS, cachedPPS;
    private volatile boolean spsFound, ppsFound;

    // 原始 chunk 队列
    private final LinkedBlockingQueue<byte[]> chunkQueue = new LinkedBlockingQueue<>(MAX_QUEUE);

    // 线程
    private Thread decodeThread;

    // 统计
    private volatile long inputCount, outputCount, ptsCounter;

    public WsH264Decoder(Surface surface) {
        this.surface = surface;
    }

    /**
     * 喂入原始 H.264 数据块 (来自 WebSocket 消息)
     */
    public synchronized void feedRawData(byte[] data) {
        if (!isRunning) return;

        // Phase 1: 扫描 SPS/PPS
        if (!decoderReady && (!spsFound || !ppsFound)) {
            scanForSPSPPS(data);
        }
        if (spsFound && ppsFound && !decoderReady) {
            configureDecoder();
        }

        // Phase 2: 原始 chunk 直接入队
        if (!chunkQueue.offer(data)) {
            chunkQueue.poll();
            chunkQueue.offer(data);
        }
    }

    // SPS/PPS 扫描器
    private void scanForSPSPPS(byte[] data) {
        byte[] buf;
        if (scanResidue != null && scanResidue.length > 0) {
            buf = new byte[scanResidue.length + data.length];
            System.arraycopy(scanResidue, 0, buf, 0, scanResidue.length);
            System.arraycopy(data, 0, buf, scanResidue.length, data.length);
            scanResidue = null;
        } else {
            buf = data;
        }

        int i = 0;
        while (i < buf.length - 3) {
            int scLen = 0;
            if (i + 3 < buf.length && buf[i] == 0 && buf[i + 1] == 0 && buf[i + 2] == 0 && buf[i + 3] == 1)
                scLen = 4;
            else if (buf[i] == 0 && buf[i + 1] == 0 && buf[i + 2] == 1)
                scLen = 3;

            if (scLen == 0) { i++; continue; }

            int nalType = buf[i + scLen] & 0x1F;

            if ((nalType == 7 && !spsFound) || (nalType == 8 && !ppsFound)) {
                int end = findNextSC(buf, i + scLen + 1);
                byte[] nal = new byte[end - i];
                System.arraycopy(buf, i, nal, 0, end - i);

                if (nalType == 7) {
                    cachedSPS = nal;
                    spsFound = true;
                    Log.d(TAG, "SPS found (" + nal.length + "B)");
                } else {
                    cachedPPS = nal;
                    ppsFound = true;
                    Log.d(TAG, "PPS found (" + nal.length + "B)");
                }
                if (spsFound && ppsFound) return;
                i = end;
            } else {
                i++;
            }
        }

        // 保存尾部连续的 0x00 字节
        int zeros = 0;
        for (int j = buf.length - 1; j >= 0 && j >= buf.length - 3 && buf[j] == 0; j--) {
            zeros++;
        }
        if (zeros > 0) {
            scanResidue = new byte[zeros];
            System.arraycopy(buf, buf.length - zeros, scanResidue, 0, zeros);
        }
    }

    private static int findNextSC(byte[] buf, int from) {
        for (int i = from; i < buf.length - 2; i++) {
            if (buf[i] == 0 && buf[i + 1] == 0) {
                if (buf[i + 2] == 1) return i;
                if (i + 3 < buf.length && buf[i + 2] == 0 && buf[i + 3] == 1) return i;
            }
        }
        return buf.length;
    }

    // 解码器配置
    private void configureDecoder() {
        if (cachedSPS == null || cachedPPS == null) return;
        try {
            int w = 640, h = 480;
            try {
                byte[] rawSPS = stripStartCode(cachedSPS);
                int[] dims = parseSPS(rawSPS);
                w = dims[0]; h = dims[1];
                if (w <= 0 || h <= 0 || w > 4096 || h > 4096) { w = 640; h = 480; }
            } catch (Exception e) {
                Log.w(TAG, "SPS parse fallback to 640x480");
            }

            MediaFormat fmt = MediaFormat.createVideoFormat("video/avc", w, h);
            fmt.setByteBuffer("csd-0", ByteBuffer.wrap(cachedSPS));
            fmt.setByteBuffer("csd-1", ByteBuffer.wrap(cachedPPS));
            fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 1024 * 1024);

            codec = MediaCodec.createDecoderByType("video/avc");
            codec.configure(fmt, surface, null, 0);
            codec.start();
            decoderReady = true;

            Log.d(TAG, "Decoder configured: " + w + "x" + h + " [" + codec.getName() + "]");
        } catch (Exception e) {
            Log.e(TAG, "Configure failed: " + e.getMessage(), e);
            releaseDecoder();
        }
    }

    // 解码主循环
    private void decodeLoop() {
        Log.d(TAG, "Decode thread started");

        while (isRunning) {
            try {
                if (!decoderReady) {
                    Thread.sleep(5);
                    continue;
                }

                // 延迟控制: 队列积压时丢弃旧 chunk
                if (chunkQueue.size() > 10) {
                    int dropped = 0;
                    while (chunkQueue.size() > 2) {
                        chunkQueue.poll();
                        dropped++;
                    }
                    Log.d(TAG, "Dropped " + dropped + " chunks (latency control)");
                }

                byte[] chunk = chunkQueue.poll(10, TimeUnit.MILLISECONDS);
                if (chunk == null) {
                    drainOutput();
                    continue;
                }

                int idx = codec.dequeueInputBuffer(20_000);
                if (idx >= 0) {
                    ByteBuffer buf = codec.getInputBuffer(idx);
                    if (buf != null) {
                        buf.clear();
                        buf.put(chunk);
                        long pts = ptsCounter * 40_000;
                        codec.queueInputBuffer(idx, 0, chunk.length, pts, 0);
                        ptsCounter++;
                        inputCount++;
                    }
                }

                drainOutput();

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                Log.e(TAG, "Decode err: " + e.getMessage(), e);
                if (isRunning) {
                    decoderReady = false;
                    releaseDecoder();
                }
            }
        }
        Log.d(TAG, "Decode thread ended");
    }

    private void drainOutput() {
        if (codec == null) return;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        while (isRunning) {
            int out;
            try { out = codec.dequeueOutputBuffer(info, 0); }
            catch (Exception e) { break; }

            if (out >= 0) {
                codec.releaseOutputBuffer(out, true);
                outputCount++;
            } else if (out == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d(TAG, "Output format: " + codec.getOutputFormat());
            } else {
                break;
            }
        }
    }

    // 生命周期
    public void start() {
        isRunning = true;
        inputCount = outputCount = ptsCounter = 0;
        spsFound = ppsFound = decoderReady = false;
        cachedSPS = cachedPPS = scanResidue = null;
        chunkQueue.clear();

        decodeThread = new Thread(this::decodeLoop, "WsH264Decode");
        decodeThread.setDaemon(true);
        decodeThread.start();
    }

    public void stop() {
        isRunning = false;
        if (decodeThread != null) decodeThread.interrupt();
        decodeThread = null;
        chunkQueue.clear();
        releaseDecoder();
    }

    private void releaseDecoder() {
        decoderReady = false;
        if (codec != null) {
            try { codec.stop(); } catch (Exception ignored) {}
            try { codec.release(); } catch (Exception ignored) {}
            codec = null;
        }
    }

    // SPS 解析辅助
    private static byte[] stripStartCode(byte[] d) {
        if (d.length > 4 && d[0] == 0 && d[1] == 0 && d[2] == 0 && d[3] == 1) {
            byte[] r = new byte[d.length - 4];
            System.arraycopy(d, 4, r, 0, r.length);
            return r;
        }
        if (d.length > 3 && d[0] == 0 && d[1] == 0 && d[2] == 1) {
            byte[] r = new byte[d.length - 3];
            System.arraycopy(d, 3, r, 0, r.length);
            return r;
        }
        return d;
    }

    private static int[] parseSPS(byte[] sps) {
        if (sps.length < 4) return new int[]{640, 480};
        BitReader r = new BitReader(sps);
        r.readBits(8);
        int profile = r.readBits(8);
        r.readBits(8);
        r.readBits(8);
        r.readUE();
        int[] hp = {100, 110, 122, 244, 44, 83, 86, 118, 128, 138, 139, 134, 135};
        for (int p : hp) {
            if (profile == p) {
                int cf = r.readUE();
                if (cf == 3) r.readBits(1);
                r.readUE();
                r.readUE();
                r.readBits(1);
                if (r.readBits(1) == 1) {
                    int cnt = (cf != 3) ? 8 : 12;
                    for (int i = 0; i < cnt; i++)
                        if (r.readBits(1) == 1)
                            for (int j = 0; j < (i < 6 ? 16 : 64); j++) r.readUE();
                }
                break;
            }
        }
        r.readUE();
        int fo = r.readUE();
        if (fo == 0) r.readUE();
        else if (fo == 1) {
            r.readBits(1);
            r.readSE();
            r.readSE();
            int n = r.readUE();
            for (int i = 0; i < n; i++) r.readSE();
        }
        r.readUE();
        r.readBits(1);
        int w = (r.readUE() + 1) * 16;
        int h = (r.readUE() + 1) * 16 * (2 - r.readBits(1));
        return new int[]{w, h};
    }

    private static class BitReader {
        byte[] d;
        int bp = 0, bi = 0;

        BitReader(byte[] d) { this.d = d; }

        int readBit() {
            if (bp >= d.length) return 0;
            int b = (d[bp] >> (7 - bi)) & 1;
            bi++;
            if (bi == 8) { bi = 0; bp++; }
            return b;
        }

        int readBits(int n) {
            int r = 0;
            for (int i = 0; i < n; i++) r = (r << 1) | readBit();
            return r;
        }

        int readUE() {
            int z = 0;
            while (readBit() == 0 && z < 32) z++;
            return z == 0 ? 0 : (1 << z) - 1 + readBits(z);
        }

        int readSE() {
            int u = readUE();
            return u % 2 == 0 ? -(u / 2) : (u + 1) / 2;
        }
    }
}
