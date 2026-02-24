package com.obana.remotecar.JniView;

import android.content.Context;
import com.jovision.Jni;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.obana.remotecar.MainActivity;
import com.obana.remotecar.TcpSocket;
import com.obana.remotecar.utils.AppLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JniView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "JniView";

    private Channel mConnectChannel;
    private Device mDevice;

    private Context mContext;

    private Surface mSurface;
    private boolean apConnect = false;//if true, device ip will force to 10.10.0.1

    public JniView(Context context) {
        this(context, null, 0);
    }

    public JniView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JniView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
        setFocusable(true);

        mContext = context;
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mSurface = surfaceHolder.getSurface();

        AppLog.i(TAG, "surfaceCreated mSurface=" + mSurface);

        connect();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        //mSurface = surfaceHolder.getSurface();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    private int connect(Device device, Channel channel, Surface surface) {
        if (device == null || channel == null) {
            return -1;
        }
        if ("".equalsIgnoreCase(device.getIp()) || device.getPort() == 0) {
            return Jni.connect(channel.getIndex(), channel.getChannel(), device.getIp(), device.getPort(), device.getUser(), device.getPwd(), device.getNo(), device.getGid(), true, 1, true, 6, surface, false, false, this.apConnect, false, null);
        }
        return Jni.connect(channel.getIndex(), channel.getChannel(), device.getIp(), device.getPort(), device.getUser(), device.getPwd(), -1, device.getGid(), true, 1, true, 6, surface, false, false, this.apConnect, false, null);
    }

    public void connect() {
        AppLog.i(TAG,"connect... begin");

        Device device = mDevice;

        if (device == null) return;
        if (device.getChannelList() == null) return;
        mConnectChannel = device.getChannelList().get(0);

        if (mConnectChannel == null) return;

        if (mSurface == null) return;

        initDeviceAddr();

        AppLog.i(TAG,"connect... start, device=" + device);

        if (!mConnectChannel.isConnected()) {
            Log.v(TAG, "isConnected=ture, connect!");
            mConnectChannel.setParent(mDevice);
            connect(mDevice, mConnectChannel, mSurface);
        } else if (mConnectChannel.isConnected() && mConnectChannel.isPaused()) {
            boolean sendBytes = Jni.sendBytes(mConnectChannel.getIndex(), JVNetConst.JVN_CMD_VIDEO, new byte[0], 8);
            mConnectChannel.setPaused(false);
            Log.v(TAG, "onResume=" + sendBytes);
            if (sendBytes) {
                boolean resume = Jni.resume(mConnectChannel.getIndex(), mSurface);
                Log.v(TAG, "JNI-Play-Resume=" + resume);
                if (resume) {
                    //PlayActivity.this.linkState.setVisibility(8);
                }
            }
        }


    }

    private void initDeviceAddr() {
        boolean isLocal = "local".equals(getSharedPreference("networkType"));

        MainActivity activity = (MainActivity) mContext;
        TcpSocket sock = activity.getTcpSocket();
        String ip;
        int port;
        if (isLocal) {
            ip = AppConsts.LOCAL_IP;
            port = AppConsts.IPC_DEFAULT_PORT;
        } else {
            ip = sock.getJpegMediaHost();
            port = AppConsts.IPC_REMOTE_PORT;
        }

        mDevice = new Device(
                ip,/*10.10.0.1*/
                port,
                "", -1,
                AppConsts.IPC_DEFAULT_USER,
                AppConsts.IPC_DEFAULT_PWD,
                1);

        setHelperToDevice(mDevice);
    }
    public void initUi() {
        boolean init = Jni.init((Object)mContext, 9200, AppConsts.LOG_PATH);
        boolean enableLinkHelper = Jni.enableLinkHelper(true, 3, 10);
        Log.e("enableHeler", enableLinkHelper + "");
        if (init) {
            Jni.enableLog(false);
            Log.i("SDK_VERSION", Jni.getVersion());
        } else {
            Log.e("initSDK", "initSDK--failed");
        }



    }

    private void setHelperToDevice(Device device) {
        if (device == null) return;

        JSONArray jSONArray = new JSONArray();
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("gid", device.getGid());
            jSONObject.put("no", device.getNo());
            jSONObject.put("channel", 1);
            jSONObject.put("name", device.getUser());
            jSONObject.put("pwd", device.getPwd());
            jSONArray.put(jSONObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if ("".equalsIgnoreCase(jSONArray.toString())) {
            return;
        }
        Log.e(TAG, jSONArray.toString());
        Jni.setLinkHelper(jSONArray.toString());

    }

    public void onPause() {
        Channel channel = mConnectChannel;
        if (channel != null && channel.isConnected() && !mConnectChannel.isPaused()) {
            mConnectChannel.setPaused(true);
            boolean sendBytes = Jni.sendBytes(mConnectChannel.getIndex(), JVNetConst.JVN_CMD_VIDEOPAUSE, new byte[0], 8);
            Log.v(TAG, "onPause=" + sendBytes);
        }
        boolean pause = Jni.pause(mConnectChannel.getIndex());
        Log.v(TAG, "pauseRes=" + pause);
        //super.onPause();
    }

    private String getSharedPreference(String key) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
        return sp.getString(key, "");
    }
}