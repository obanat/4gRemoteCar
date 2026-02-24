package com.obana.remotecar.JJRC;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;

import com.fh.lib.Define;
import com.fh.lib.FHSDK;
import com.obana.remotecar.R;
import com.obana.remotecar.utils.AppLog;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/* loaded from: classes.dex */
public class GLFrameRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "GLFrameRenderer";
    private static final int DISPLAY_TYPE_IMAGE = 3;
    private static final int DISPLAY_TYPE_RGB = 1;
    private static final int DISPLAY_TYPE_YUV = 0;
    private static final int DISPLAY_TYPE_YUV420SP = 2;
    private static final float H_OFFSET_BASE = 1000.0f;
    private static final float STEP_BASE_FAST = 200.0f;
    private static final float STEP_BASE_SLOW = 100.0f;
    private static final float STEP_OFFSET = 5.0f;
    public static float circleR;
    public static float circleX;
    public static float circleY;
    public static float depth;
    public static int displayMode;
    public static int eyeMode;
    public static float hDegrees;
    public static float hOffset;
    private static GLFrameRenderer instance;
    public static final boolean isDebugMode = false;
    public static int mDrawHeight;
    public static int mDrawWidth;
    public static int mScreenHeight;
    public static int mScreenWidth;
    public static int modeOffset;
    private static OnYuvUpdateListener onYuvUpdateListener;
    public static float scrollStep;
    private static boolean startSnapshot;
    public static float vDegrees;
    private static float velocityX;
    private static float velocityY;
    private boolean bIsTime2Draw;
    private boolean bSurfaceChanged;
    private boolean bSurfaceCreate;
    private boolean bUpdated;
    public Define.YUVDataCallBackInterface dataFun;
    private int drawCount;
    private byte[] frameBuf;
    private float lastDepth;
    private float lastHDegrees;
    private float lastHOffset;
    private int lastShowMode;
    private float lastVDegrees;
    private Context mContext;
    private Handler mHandler;
    private SnapshotThread mSnapshotThread;
    private GLSurfaceView mTargetSurface;
    private int mVideoHeight;
    private int mVideoWidth;
 
    Runnable requestRender;
    Runnable scaleView;
    private boolean showSignImage;
    private int showSignImageHeight;
    private int showSignImageWidth;
    private int showSignImageX;
    private int showSignImageY;
    private long signImageBuffer;
    private int signImageHeight;
    private byte[] signImageRgbaData;
    private int signImageWidth;
    private long signImageWin;
    private boolean updateSignImage;
    private int view_h;
    private int view_w;
    private int view_x;
    private int view_y;
    private byte[] yuv;
    public static float[] hEyeDegrees = {0.0f, 90.0f, 180.0f, 270.0f};
    public static int curIndex = 0;
    public static boolean isDoubleClick = false;
    public static boolean isZoomIn = false;
    public static long hWin = 0;
    public static long hBuffer = 0;
    public static long[] hWinMixMode = new long[4];
    public static boolean bMixMode = false;
    public static int ctrlIndex = 0;
    public static boolean resChanged = false;
    public static boolean initSignImage = false;

    /* loaded from: classes.dex */
    public interface OnYuvUpdateListener {
        void onUpdate(byte[] bArr, int i, int i2);
    }

    /* loaded from: classes.dex */
    public class RGBRes {
        public int height;
        public byte[] rgb;
        public int width;

        public RGBRes() {
        }
    }

    public GLFrameRenderer(Context context, GLSurfaceView gLSurfaceView, DisplayMetrics displayMetrics) {
        this.bSurfaceCreate = false;
        this.bSurfaceChanged = false;
        this.lastShowMode = -1;
        this.mHandler = null;
        this.drawCount = 0;
        this.frameBuf = null;
        this.bUpdated = false;
        this.bIsTime2Draw = false;
        this.lastVDegrees = -1.0f;
        this.lastHDegrees = -1.0f;
        this.lastDepth = -1.0f;
        this.lastHOffset = -1.0f;
        this.signImageWin = 0L;
        this.signImageBuffer = 0L;
        this.signImageWidth = 0;
        this.signImageHeight = 0;
        this.updateSignImage = false;
        this.scaleView = new Runnable() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.1
            @Override // java.lang.Runnable
            public void run() {
                float abs = Math.abs(FHSDK.getMaxVDegress(GLFrameRenderer.hWin) / 50.0f);
                float abs2 = Math.abs(FHSDK.getMaxZDepth(GLFrameRenderer.hWin) / 50.0f);
                if (GLFrameRenderer.isZoomIn) {
                    GLFrameRenderer.vDegrees -= abs;
                    GLFrameRenderer.hDegrees += 1.8f;
                    GLFrameRenderer.depth += abs2;
                } else {
                    GLFrameRenderer.vDegrees += abs * 4.0f;
                    GLFrameRenderer.hDegrees -= 7.2f;
                    GLFrameRenderer.depth -= abs2 * 4.0f;
                }
                if (GLFrameRenderer.vDegrees < FHSDK.getMaxVDegress(GLFrameRenderer.hWin)) {
                    GLFrameRenderer.vDegrees = FHSDK.getMaxVDegress(GLFrameRenderer.hWin);
                } else if (GLFrameRenderer.vDegrees > 0.0f) {
                    GLFrameRenderer.vDegrees = 0.0f;
                }
                if (GLFrameRenderer.depth < FHSDK.getMaxZDepth(GLFrameRenderer.hWin)) {
                    GLFrameRenderer.depth = FHSDK.getMaxZDepth(GLFrameRenderer.hWin);
                } else if (GLFrameRenderer.depth > 0.0f) {
                    GLFrameRenderer.depth = 0.0f;
                }
                if ((!GLFrameRenderer.isZoomIn || GLFrameRenderer.depth == 0.0f) && (GLFrameRenderer.isZoomIn || GLFrameRenderer.depth == FHSDK.getMaxZDepth(GLFrameRenderer.hWin))) {
                    return;
                }
                GLFrameRenderer.this.mHandler.postDelayed(GLFrameRenderer.this.scaleView, 40L);
            }
        };
        this.requestRender = new Runnable() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.2
            @Override // java.lang.Runnable
            public void run() {
                if (GLFrameRenderer.displayMode != 0 && 6 != GLFrameRenderer.displayMode) {
                    GLFrameRenderer.eyeMode = 0;
                }
                if (GLFrameRenderer.isDoubleClick && ((GLFrameRenderer.displayMode == 0 && GLFrameRenderer.eyeMode == 0) || 6 == GLFrameRenderer.displayMode)) {
                    GLFrameRenderer.isDoubleClick = false;
                    if (GLFrameRenderer.depth != FHSDK.getMaxZDepth(GLFrameRenderer.hWin)) {
                        GLFrameRenderer.isZoomIn = false;
                    } else {
                        GLFrameRenderer.isZoomIn = true;
                    }
                    GLFrameRenderer.this.mHandler.post(GLFrameRenderer.this.scaleView);
                }
                if (GLFrameRenderer.velocityX > 0.0f) {
                    GLFrameRenderer.velocityX -= GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityX < 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused = GLFrameRenderer.velocityX = 0.0f;
                    }
                    if (GLFrameRenderer.displayMode == 0 || 6 == GLFrameRenderer.displayMode) {
                        if (GLFrameRenderer.eyeMode == 0 || 1 == GLFrameRenderer.eyeMode) {
                            GLFrameRenderer.hDegrees -= (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                        } else if (2 == GLFrameRenderer.eyeMode) {
                            float[] fArr = GLFrameRenderer.hEyeDegrees;
                            int i = GLFrameRenderer.curIndex;
                            fArr[i] = fArr[i] - ((GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f);
                        }
                    } else {
                        GLFrameRenderer.hOffset -= GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                } else if (GLFrameRenderer.velocityX < 0.0f) {
                    GLFrameRenderer.velocityX += GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityX > 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused2 = GLFrameRenderer.velocityX = 0.0f;
                    }
                    if (GLFrameRenderer.displayMode == 0 || 6 == GLFrameRenderer.displayMode) {
                        if (GLFrameRenderer.eyeMode == 0 || 1 == GLFrameRenderer.eyeMode) {
                            GLFrameRenderer.hDegrees += (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                        } else if (2 == GLFrameRenderer.eyeMode) {
                            float[] fArr2 = GLFrameRenderer.hEyeDegrees;
                            int i2 = GLFrameRenderer.curIndex;
                            fArr2[i2] = fArr2[i2] + ((GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f);
                        }
                    } else {
                        GLFrameRenderer.hOffset += GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                }
                if (GLFrameRenderer.velocityY > 0.0f) {
                    GLFrameRenderer.velocityY -= GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityY < 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused3 = GLFrameRenderer.velocityY = 0.0f;
                    }
                    if (6 == GLFrameRenderer.displayMode) {
                        GLFrameRenderer.vDegrees -= (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                    } else {
                        GLFrameRenderer.hOffset -= GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                } else if (GLFrameRenderer.velocityY < 0.0f) {
                    GLFrameRenderer.velocityY += GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityY > 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused4 = GLFrameRenderer.velocityY = 0.0f;
                    }
                    if (6 == GLFrameRenderer.displayMode) {
                        GLFrameRenderer.vDegrees += (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                    } else {
                        GLFrameRenderer.hOffset += GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                }
                if (GLFrameRenderer.scrollStep > 0.0f) {
                    GLFrameRenderer.scrollStep -= GLFrameRenderer.STEP_OFFSET;
                }
                GLFrameRenderer.this.mHandler.postDelayed(GLFrameRenderer.this.requestRender, 40L);
            }
        };
        this.dataFun = new Define.YUVDataCallBackInterface() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.3
            @Override // com.p005fh.lib.Define.YUVDataCallBackInterface
            public void update(int i, int i2) {
                if (i <= 0 || i2 <= 0 || i == GLFrameRenderer.this.mVideoWidth || i2 == GLFrameRenderer.this.mVideoHeight) {
                    return;
                }
                GLFrameRenderer.this.mVideoWidth = i;
                GLFrameRenderer.this.mVideoHeight = i2;
                GLFrameRenderer.this.yuv = new byte[((i * i2) * 3) / 2];
            }

            @Override // com.p005fh.lib.Define.YUVDataCallBackInterface
            public void update(byte[] bArr) {
                if (GLFrameRenderer.this.yuv != null) {
                    System.arraycopy(bArr, 0, GLFrameRenderer.this.yuv, 0, GLFrameRenderer.this.yuv.length);
                    GLFrameRenderer.this.bUpdated = true;
                    if (GLFrameRenderer.onYuvUpdateListener != null) {
                        GLFrameRenderer.onYuvUpdateListener.onUpdate(GLFrameRenderer.this.yuv, GLFrameRenderer.this.mVideoWidth, GLFrameRenderer.this.mVideoHeight);
                    }
                }
            }
        };
        this.mContext = context;
        this.mTargetSurface = gLSurfaceView;
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        FHSDK.registerUpdateCallBack(FHSDK.handle, this.dataFun);
        this.mHandler = new Handler();
        this.mHandler.post(this.requestRender);
        this.mSnapshotThread = new SnapshotThread();
    }

    public GLFrameRenderer() {
        this.bSurfaceCreate = false;
        this.bSurfaceChanged = false;
        this.lastShowMode = -1;
        this.mHandler = null;
        this.drawCount = 0;
        this.frameBuf = null;
        this.bUpdated = false;
        this.bIsTime2Draw = false;
        this.lastVDegrees = -1.0f;
        this.lastHDegrees = -1.0f;
        this.lastDepth = -1.0f;
        this.lastHOffset = -1.0f;
        this.signImageWin = 0L;
        this.signImageBuffer = 0L;
        this.signImageWidth = 0;
        this.signImageHeight = 0;
        this.updateSignImage = false;
        this.scaleView = new Runnable() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.1
            @Override // java.lang.Runnable
            public void run() {
                float abs = Math.abs(FHSDK.getMaxVDegress(GLFrameRenderer.hWin) / 50.0f);
                float abs2 = Math.abs(FHSDK.getMaxZDepth(GLFrameRenderer.hWin) / 50.0f);
                if (GLFrameRenderer.isZoomIn) {
                    GLFrameRenderer.vDegrees -= abs;
                    GLFrameRenderer.hDegrees += 1.8f;
                    GLFrameRenderer.depth += abs2;
                } else {
                    GLFrameRenderer.vDegrees += abs * 4.0f;
                    GLFrameRenderer.hDegrees -= 7.2f;
                    GLFrameRenderer.depth -= abs2 * 4.0f;
                }
                if (GLFrameRenderer.vDegrees < FHSDK.getMaxVDegress(GLFrameRenderer.hWin)) {
                    GLFrameRenderer.vDegrees = FHSDK.getMaxVDegress(GLFrameRenderer.hWin);
                } else if (GLFrameRenderer.vDegrees > 0.0f) {
                    GLFrameRenderer.vDegrees = 0.0f;
                }
                if (GLFrameRenderer.depth < FHSDK.getMaxZDepth(GLFrameRenderer.hWin)) {
                    GLFrameRenderer.depth = FHSDK.getMaxZDepth(GLFrameRenderer.hWin);
                } else if (GLFrameRenderer.depth > 0.0f) {
                    GLFrameRenderer.depth = 0.0f;
                }
                if ((!GLFrameRenderer.isZoomIn || GLFrameRenderer.depth == 0.0f) && (GLFrameRenderer.isZoomIn || GLFrameRenderer.depth == FHSDK.getMaxZDepth(GLFrameRenderer.hWin))) {
                    return;
                }
                GLFrameRenderer.this.mHandler.postDelayed(GLFrameRenderer.this.scaleView, 40L);
            }
        };
        this.requestRender = new Runnable() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.2
            @Override // java.lang.Runnable
            public void run() {
                if (GLFrameRenderer.displayMode != 0 && 6 != GLFrameRenderer.displayMode) {
                    GLFrameRenderer.eyeMode = 0;
                }
                if (GLFrameRenderer.isDoubleClick && ((GLFrameRenderer.displayMode == 0 && GLFrameRenderer.eyeMode == 0) || 6 == GLFrameRenderer.displayMode)) {
                    GLFrameRenderer.isDoubleClick = false;
                    if (GLFrameRenderer.depth != FHSDK.getMaxZDepth(GLFrameRenderer.hWin)) {
                        GLFrameRenderer.isZoomIn = false;
                    } else {
                        GLFrameRenderer.isZoomIn = true;
                    }
                    GLFrameRenderer.this.mHandler.post(GLFrameRenderer.this.scaleView);
                }
                if (GLFrameRenderer.velocityX > 0.0f) {
                    GLFrameRenderer.velocityX -= GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityX < 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused = GLFrameRenderer.velocityX = 0.0f;
                    }
                    if (GLFrameRenderer.displayMode == 0 || 6 == GLFrameRenderer.displayMode) {
                        if (GLFrameRenderer.eyeMode == 0 || 1 == GLFrameRenderer.eyeMode) {
                            GLFrameRenderer.hDegrees -= (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                        } else if (2 == GLFrameRenderer.eyeMode) {
                            float[] fArr = GLFrameRenderer.hEyeDegrees;
                            int i = GLFrameRenderer.curIndex;
                            fArr[i] = fArr[i] - ((GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f);
                        }
                    } else {
                        GLFrameRenderer.hOffset -= GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                } else if (GLFrameRenderer.velocityX < 0.0f) {
                    GLFrameRenderer.velocityX += GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityX > 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused2 = GLFrameRenderer.velocityX = 0.0f;
                    }
                    if (GLFrameRenderer.displayMode == 0 || 6 == GLFrameRenderer.displayMode) {
                        if (GLFrameRenderer.eyeMode == 0 || 1 == GLFrameRenderer.eyeMode) {
                            GLFrameRenderer.hDegrees += (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                        } else if (2 == GLFrameRenderer.eyeMode) {
                            float[] fArr2 = GLFrameRenderer.hEyeDegrees;
                            int i2 = GLFrameRenderer.curIndex;
                            fArr2[i2] = fArr2[i2] + ((GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f);
                        }
                    } else {
                        GLFrameRenderer.hOffset += GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                }
                if (GLFrameRenderer.velocityY > 0.0f) {
                    GLFrameRenderer.velocityY -= GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityY < 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused3 = GLFrameRenderer.velocityY = 0.0f;
                    }
                    if (6 == GLFrameRenderer.displayMode) {
                        GLFrameRenderer.vDegrees -= (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                    } else {
                        GLFrameRenderer.hOffset -= GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                } else if (GLFrameRenderer.velocityY < 0.0f) {
                    GLFrameRenderer.velocityY += GLFrameRenderer.scrollStep;
                    if (GLFrameRenderer.velocityY > 0.0f || GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE < 0.005f) {
                        float unused4 = GLFrameRenderer.velocityY = 0.0f;
                    }
                    if (6 == GLFrameRenderer.displayMode) {
                        GLFrameRenderer.vDegrees += (GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE) * 50.0f;
                    } else {
                        GLFrameRenderer.hOffset += GLFrameRenderer.scrollStep / GLFrameRenderer.H_OFFSET_BASE;
                    }
                }
                if (GLFrameRenderer.scrollStep > 0.0f) {
                    GLFrameRenderer.scrollStep -= GLFrameRenderer.STEP_OFFSET;
                }
                GLFrameRenderer.this.mHandler.postDelayed(GLFrameRenderer.this.requestRender, 40L);
            }
        };
        this.dataFun = new Define.YUVDataCallBackInterface() { // from class: com.vison.baselibrary.opengles.GLFrameRenderer.3
            @Override // com.p005fh.lib.Define.YUVDataCallBackInterface
            public void update(int i, int i2) {
                if (i <= 0 || i2 <= 0 || i == GLFrameRenderer.this.mVideoWidth || i2 == GLFrameRenderer.this.mVideoHeight) {
                    return;
                }
                GLFrameRenderer.this.mVideoWidth = i;
                GLFrameRenderer.this.mVideoHeight = i2;
                GLFrameRenderer.this.yuv = new byte[((i * i2) * 3) / 2];
            }

            @Override // com.p005fh.lib.Define.YUVDataCallBackInterface
            public void update(byte[] bArr) {
                if (GLFrameRenderer.this.yuv != null) {
                    System.arraycopy(bArr, 0, GLFrameRenderer.this.yuv, 0, GLFrameRenderer.this.yuv.length);
                    GLFrameRenderer.this.bUpdated = true;
                    if (GLFrameRenderer.onYuvUpdateListener != null) {
                        GLFrameRenderer.onYuvUpdateListener.onUpdate(GLFrameRenderer.this.yuv, GLFrameRenderer.this.mVideoWidth, GLFrameRenderer.this.mVideoHeight);
                    }
                }
            }
        };
    }

    public static GLFrameRenderer getInstance() {
        if (instance == null) {
            instance = new GLFrameRenderer();
        }
        return instance;
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 gl10, EGLConfig eGLConfig) {
        MyMediaCodec.getInstance().startPlay(null);
        this.bSurfaceCreate = true;
        displayMode = 9;
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 gl10, int i, int i2) {
        Log.i("TAG" ,"onSurfaceChanged:" + Integer.valueOf(i)+ Integer.valueOf(i2));
        this.bIsTime2Draw = false;
        mScreenWidth = i;
        mScreenHeight = i2;
        if (mScreenWidth < mScreenHeight) {
            int i3 = mScreenWidth;
            mDrawHeight = i3;
            mDrawWidth = i3;
        } else {
            mDrawWidth = mScreenWidth;
            mDrawHeight = mScreenHeight;
        }
        this.bSurfaceChanged = true;
        this.view_x = (mScreenWidth - mDrawWidth) / 2;
        this.view_y = (mScreenHeight - mDrawHeight) / 2;
        this.view_w = mDrawWidth;
        this.view_h = mDrawHeight;
        FHSDK.init(mDrawWidth, mDrawHeight);
        if (0 == hBuffer) {
            hBuffer = FHSDK.createBuffer(0);
        }
        if (0 == hWin) {
            hWin = FHSDK.createWindow(displayMode);
        }
        FHSDK.unbind(hWin);
        FHSDK.bind(hWin, hBuffer);
        depth = FHSDK.getMaxZDepth(hWin);
        if (initSignImage) {
            Bitmap decodeResource = BitmapFactory.decodeResource(this.mContext.getResources(), R.drawable.h264);
            this.signImageWidth = decodeResource.getWidth();
            this.signImageHeight = decodeResource.getHeight();
            if (0 != this.signImageBuffer) {
                FHSDK.destroyBuffer(this.signImageBuffer);
            }
            this.signImageBuffer = FHSDK.createBuffer(3);
            if (0 != this.signImageWin) {
                FHSDK.destroyWindow(this.signImageWin);
            }
            this.signImageWin = FHSDK.createWindow(9);
            FHSDK.bind(this.signImageWin, this.signImageBuffer);
            ByteBuffer allocate = ByteBuffer.allocate(decodeResource.getWidth() * decodeResource.getHeight() * 4);
            decodeResource.copyPixelsToBuffer(allocate);
            this.signImageRgbaData = allocate.array();
            FHSDK.update(this.signImageBuffer, this.signImageRgbaData, this.signImageWidth, this.signImageHeight);
            decodeResource.recycle();
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 gl10) {
        synchronized (this) {
            if (this.updateSignImage) {
                FHSDK.update(this.signImageBuffer, this.signImageRgbaData, this.signImageWidth, this.signImageHeight);
                this.updateSignImage = false;
            }
            if (displayMode != FHSDK.getDisplayMode(hWin)) {
                FHSDK.unbind(hWin);
                FHSDK.destroyWindow(hWin);
                hWin = FHSDK.createWindow(displayMode);
                FHSDK.bind(hWin, hBuffer);
            }
            FHSDK.setStandardCircle(hWin, circleX, circleY, circleR);
            if (this.yuv != null && this.bUpdated) {
                FHSDK.update(hBuffer, this.yuv, this.mVideoWidth, this.mVideoHeight);
                this.bUpdated = false;
                this.bIsTime2Draw = true;
            }
            if (this.bIsTime2Draw) {
                FHSDK.clear();
                if (bMixMode) {
                    int[] iArr = {this.view_x, this.view_x + (this.view_w / 2), this.view_x, this.view_x + (this.view_w / 2)};
                    int[] iArr2 = {this.view_y, this.view_y, this.view_y + (this.view_h / 2), this.view_y + (this.view_h / 2)};
                    for (int i = 0; i < 4; i++) {
                        FHSDK.setStandardCircle(hWinMixMode[i], circleX, circleY, circleR);
                        FHSDK.viewport(iArr[i], iArr2[i], this.view_w / 2, this.view_h / 2);
                        if (1 == i) {
                            FHSDK.setImagingType(hWinMixMode[i], 1);
                        }
                        FHSDK.draw(hWinMixMode[i]);
                    }
                } else {
                    if (displayMode != 0 && 6 != displayMode) {
                        if (MyMediaCodec.SHOW_MODE_3D == MyMediaCodec.getInstance().getShowMode()) {
                            int i2 = this.view_w / 2;
                            int i3 = (int) ((this.view_w / 2.0f) * (this.mVideoHeight / this.mVideoWidth));
                            int[] iArr3 = {this.view_x, this.view_x + (this.view_w / 2), this.view_x, this.view_x + (this.view_w / 2)};
                            int[] iArr4 = {(this.view_h - i3) / 2, (this.view_h - i3) / 2, this.view_y + (this.view_h / 2), this.view_y + (this.view_h / 2)};
                            for (int i4 = 0; i4 < 2; i4++) {
                                FHSDK.viewport(iArr3[i4], iArr4[i4], i2, i3);
                                FHSDK.draw(hWin);
                            }
                        } else {
                            FHSDK.viewport(this.view_x, this.view_y, this.view_w, this.view_h);
                            FHSDK.expandLookAt(hWin, hOffset);
                            FHSDK.draw(hWin);
                            if (this.showSignImage) {
                                FHSDK.viewport(this.showSignImageX, this.showSignImageY, this.showSignImageWidth, this.showSignImageHeight);
                                FHSDK.draw(this.signImageWin);
                                FHSDK.viewport(this.view_x, this.view_y, this.view_w, this.view_h);
                            }
                        }
                    }
                    if (eyeMode == 0) {
                        FHSDK.viewport(this.view_x, this.view_y, this.view_w, this.view_h);
                        FHSDK.eyeLookAt(hWin, vDegrees, hDegrees, depth);
                        FHSDK.draw(hWin);
                    } else if (1 == eyeMode) {
                        int[] iArr5 = {this.view_x, this.view_x + (this.view_w / 2), this.view_x, this.view_x + (this.view_w / 2)};
                        int[] iArr6 = {this.view_y, this.view_y, this.view_y + (this.view_h / 2), this.view_y + (this.view_h / 2)};
                        for (int i5 = 0; i5 < 4; i5++) {
                            FHSDK.viewport(iArr5[i5], iArr6[i5], this.view_w / 2, this.view_h / 2);
                            FHSDK.eyeLookAt(hWin, FHSDK.getMaxVDegress(hWin), hDegrees + (i5 * 90), 0.0f);
                            FHSDK.draw(hWin);
                        }
                    } else if (2 == eyeMode) {
                        int[] iArr7 = {this.view_x, this.view_x + (this.view_w / 2), this.view_x, this.view_x + (this.view_w / 2)};
                        int[] iArr8 = {this.view_y, this.view_y, this.view_y + (this.view_h / 2), this.view_y + (this.view_h / 2)};
                        for (int i6 = 0; i6 < 4; i6++) {
                            FHSDK.viewport(iArr7[i6], iArr8[i6], this.view_w / 2, this.view_h / 2);
                            FHSDK.eyeLookAt(hWin, FHSDK.getMaxVDegress(hWin), hEyeDegrees[i6], 0.0f);
                            FHSDK.draw(hWin);
                        }
                    } else if (3 == eyeMode) {
                        int[] iArr9 = {this.view_x, this.view_x + (this.view_w / 2)};
                        int[] iArr10 = {this.view_y, this.view_y};
                        for (int i7 = 0; i7 < 2; i7++) {
                            FHSDK.viewport(iArr9[i7], iArr10[i7], this.view_w / 2, this.view_h);
                            FHSDK.eyeLookAt(hWin, vDegrees, hDegrees, 0.0f);
                            this.lastVDegrees = vDegrees;
                            this.lastHDegrees = hDegrees;
                            FHSDK.draw(hWin);
                        }
                    }
                }
                if (startSnapshot) {
                    startSnapshot = false;
                    this.frameBuf = FHSDK.snapshot(this.view_x, this.view_y, this.view_w, this.view_h);
                    this.mSnapshotThread.start();
                }
            }
        }
    }

    public void setvelocityX(float f) {
        velocityX = f;
        if (Math.abs(f) > 3000.0f) {
            scrollStep = STEP_BASE_FAST;
        } else {
            scrollStep = STEP_BASE_SLOW;
        }
    }

    public void setvelocityY(float f) {
        velocityY = f;
        if (Math.abs(f) > 3000.0f) {
            scrollStep = STEP_BASE_FAST;
        } else {
            scrollStep = STEP_BASE_SLOW;
        }
    }

    public void setOnYuvUpdateListener(OnYuvUpdateListener onYuvUpdateListener2) {
        onYuvUpdateListener = onYuvUpdateListener2;
    }

    public void setSignBitmap(Bitmap bitmap) {
        this.signImageWidth = bitmap.getWidth();
        this.signImageHeight = bitmap.getHeight();
        ByteBuffer allocate = ByteBuffer.allocate(bitmap.getWidth() * bitmap.getHeight() * 4);
        bitmap.copyPixelsToBuffer(allocate);
        this.signImageRgbaData = allocate.array();
        bitmap.recycle();
        AppLog.i(TAG,"setSignBitmap "+ Long.valueOf(this.signImageWin));
        this.updateSignImage = true;
    }

    public void setShowSignImage(boolean z, int i, int i2, int i3, int i4) {
        this.showSignImage = z;
        this.showSignImageX = i;
        this.showSignImageY = i2;
        this.showSignImageWidth = i3;
        this.showSignImageHeight = i4;
    }



    /* loaded from: classes.dex */
    private class SnapshotThread implements Runnable {
        private SnapshotThread() {
        }

        public void start() {
            new Thread(this).start();
        }

        @Override // java.lang.Runnable
        public void run() {
            
        }
    }
}