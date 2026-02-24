package com.obana.remotecar.JJRC;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.ViewGroup;

import com.fh.lib.Define;
import com.fh.lib.FHSDK;
import com.fh.lib.PlayInfo;
import com.obana.remotecar.MainActivity;
import com.obana.remotecar.utils.AppLog;

import java.io.IOException;

public class JjrcView implements Define.CbDataInterface,CommUtil.CbTcpConnected{
    private static String TAG = "JjrcView";
    private GLFrameSurface glFrameSurface;
    public GLFrameRenderer mFrameRender;
    private Context mContext;
    public JjrcView(Context context) {
        mContext = context;

        FHSDK.handle = FHSDK.createPlayHandle();
        FHSDK.registerNotifyCallBack(FHSDK.handle, this);
    }
    public void initView() {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        glFrameSurface = new GLFrameSurface(mContext);
        glFrameSurface.setEGLContextClientVersion(2);
        mFrameRender = new GLFrameRenderer(mContext, this.glFrameSurface, displayMetrics);
        glFrameSurface.setRenderer(mFrameRender);
        ((MainActivity)mContext).addContentView(glFrameSurface,new ViewGroup.LayoutParams(-1, -1));

        initMediaCodec();
        CommUtil.getInstance().init(mContext,this);
    }

    private void initMediaCodec()  {

        if (PlayInfo.decodeType == 4) {
            try {
                MyMediaCodec.getInstance().init(null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                MyMediaCodec.getInstance().init(mFrameRender);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override // this means socket connectted
    public void cb_connected() {
        AppLog.i(TAG,"cb_connected======> start playinfo:" + PlayInfo.targetIpAddr);
        FHSDK.registerNotifyCallBack(FHSDK.handle,this);

        FHSDK.setPlayInfo(FHSDK.handle,new PlayInfo());
    }

    @Override // com.p005fh.lib.Define.CbDataInterface
    public void cb_data(int i, byte[] bArr, int i2) {
        //LogUtils.i("cb_data======> start i1:" + i + " i2:" + i2);
        if (i != 0) {
            if (i != 4) {
                return;
            }
            CommUtil.getInstance().requestIFrame();
            return;
        }
    }
}
