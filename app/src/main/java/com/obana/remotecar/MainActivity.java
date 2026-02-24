package com.obana.remotecar;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.net.ConnectivityManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;

import com.obana.remotecar.JJRC.JjrcView;
import com.obana.remotecar.JniView.JniView;
import com.obana.remotecar.mjpeg.MjpegView;
import com.obana.remotecar.utils.AppLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends Activity implements View.OnClickListener {
    public static final String TAG = "MainActivity";

    public static final int MESSAGE_CONNECT_TO_CAMERA_FAIL = 1002;
    public static final int MESSAGE_RECONNECT_TO_CAMERA = 1003;
    public static final int MESSAGE_MAKE_TOAST = 6001;
    public static final boolean SHOW_DEBUG_MESSAGE = true;
    public static final String BUNDLE_KEY_TOAST_MSG = "Tmessage";

    //private static final boolean USE_H264_VIEW = true;
    public static final int RECONNECT_DELAY_MS = 5000;

    public static final int MEDIA_MODE_H264 = 10;
    public static final int MEDIA_MODE_MJPG = 11;
    public static final int MEDIA_MODE_JNI = 12;
    public static final int MEDIA_MODE_JJRC = 13;
    private Handler handler = null;
    private H264SurfaceView mH264View;
    private MjpegView mJpegView;
    private JniView mJniView;

    private JjrcView mJjrcView;
    private SurfaceHolder mPlaySurfaceHoler;
    private ImageButton mSetttingsBtn;
    private ImageButton mRecordVideoBtn;
    private ImageView mRecordVideoView;
    private TcpSocket mTcpSocket;

    private RandomAccessFile mH264DataFile = null;
    private boolean mBRecording = false;

    private PowerManager.WakeLock mWakeLock;
    private VerticalSeekBar verticalSeekBar;
    WifiCarController controller = null;
    private int mMediaMode = MEDIA_MODE_H264;


    protected void onCreate(Bundle paramBundle) {
        super.onCreate(paramBundle);
        //requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置横屏
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        // 设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.main);

        LayoutInflater mInflater = LayoutInflater.from(this);

        View overView = mInflater.inflate(R.layout.controller, (ViewGroup) null);
        addContentView(overView, new ViewGroup.LayoutParams(-1, -1));
        this.controller = new WifiCarController(this);
        this.controller.init(findViewById(R.id.joystickView));

        String mode = getSharedPreference("mediaType", "h264");
        if ("mjpg".equals(mode)) {
            mMediaMode = MEDIA_MODE_MJPG;
        } else if ("jni".equals(mode)) {
            mMediaMode = MEDIA_MODE_JNI;
        } else if ("h264".equals(mode)){
            mMediaMode = MEDIA_MODE_H264;
        } else {
            mMediaMode = MEDIA_MODE_JJRC;
        }
        AppLog.i(TAG, "mMediaMode =" + mode);

        mH264View = findViewById(R.id.h264View);
        mJpegView = findViewById(R.id.jpegView);
        mJniView = findViewById(R.id.jniView);

        if (mMediaMode == MEDIA_MODE_JJRC) {
            mJjrcView = new JjrcView(this);
            mJjrcView.initView();
        }

        mSetttingsBtn = findViewById(R.id.setting_button);
        mRecordVideoBtn = findViewById(R.id.record_button);
        mRecordVideoView = findViewById(R.id.recording_view);

        mTcpSocket = new TcpSocket(this);

        this.handler = new Handler() {
            public void handleMessage(Message param1Message) {
                if (!handleMessageinUI(param1Message)) {
                    super.handleMessage(param1Message);
                }
            }
        };

        PowerManager powerManager = (PowerManager)getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "NEW:WakeLock");
        }

        // 设置滑块监听器
        verticalSeekBar = findViewById(R.id.verticalSeekBar);
        setupSeekBarListener();

        mSetttingsBtn.setOnClickListener(this);
        mRecordVideoBtn.setOnClickListener(this);
    }

    private void initJniViewUi() {


    }

    public synchronized void onJniNotify(int i, int i2, int i3, Object obj) {
        AppLog.i(TAG,"onJniNotify");
    }

    @Override
    public void onClick(View view) {
        if (view == mSetttingsBtn) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (view == mRecordVideoBtn) {
            mBRecording = mBRecording? false : true;

            if (mTcpSocket!=null) mTcpSocket.startWriteToFile(mBRecording);
            enableRecordingButtonBlinking(mBRecording);
            if (mBRecording) {
                prepareH264File();
            } else {
                stopWriteH264File();
            }
        }
    }
    public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
        Log.i(TAG, "onKeyDown key=" + paramInt + " event=" + paramKeyEvent);
        Toast.makeText(this, "k:" + paramInt + " k:" + paramKeyEvent.getKeyCode(), Toast.LENGTH_SHORT).show();
        if (paramInt == 4) {
            finish();
        }
        return super.onKeyDown(paramInt, paramKeyEvent);
    }

    public boolean onKeyUp(int paramInt, KeyEvent paramKeyEvent) {
        return super.onKeyUp(paramInt, paramKeyEvent);
    }

    private void requestSpecifyNetwork() {
        ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (conMgr == null) return;

        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        NetworkRequest build = builder.build();
        AppLog.i(TAG, "---> start request network ");

        conMgr.requestNetwork(build, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                AppLog.i(TAG, "---> request network OK! start connectRunnable...");
                (new Thread(connectRunnable)).start();

            }
        });
    }

    Runnable connectRunnable = new Runnable() {
        public void run() {
            AppLog.i(TAG, "--->connectRunnable. connecting to local or p2p car.....");

            int ret = mTcpSocket.connect();
            if (ret <= 0) {
                sendToastMessage("Socket Connect Failed, retry in 5s ...");

                AppLog.i(TAG, "--->connect to car failed, reconneting after 5s ....");
                Message message = new Message();
                message.what = MESSAGE_RECONNECT_TO_CAMERA;
                MainActivity.this.handler.sendMessageDelayed(message, RECONNECT_DELAY_MS);
            } else {
                controller.updateCmdSocket(mTcpSocket);
                AppLog.i(TAG,"mMediaMode =" + mMediaMode);
                switch (mMediaMode) {
                    case MEDIA_MODE_H264:
                        mH264View.initMediaCodec();
                        break;
                    case MEDIA_MODE_MJPG:
                        mJpegView.startPlayback(mTcpSocket);
                        break;
                    case MEDIA_MODE_JNI:
                        mJniView.initUi();
                        mJniView.connect();
                        break;
                    default:
                        AppLog.e(TAG, "unknow media mode!");
                        break;

                }

                AppLog.d(TAG, "---> connect to car Succuess!");
                sendToastMessage("Car Connect Succuess!");
            }
        }
    };

    protected void onResume() {
        super.onResume();

        AppLog.i(TAG, "on Resume");

        if (mWakeLock != null) {
            mWakeLock.acquire();
        }

        //request network, then start camera socket
        if (!mTcpSocket.isConnected() && !handler.hasMessages(MESSAGE_RECONNECT_TO_CAMERA)) {
            requestSpecifyNetwork();
        }
    }

    protected void onPause() {
        super.onPause();

        if (mWakeLock != null) {
            mWakeLock.release();
        }
        AppLog.i(TAG, "on onPause");
    }


    protected void onStop() {
        super.onStop();
    }

    protected void onDestroy() {
        super.onDestroy();
        AppLog.d(TAG, "on destory");
    }


    public boolean handleMessageinUI(Message param1Message) {
        boolean handled = false;
        switch (param1Message.what) {
            case MESSAGE_CONNECT_TO_CAMERA_FAIL:
                if (SHOW_DEBUG_MESSAGE)
                    Toast.makeText(MainActivity.this, "failed to connect!", Toast.LENGTH_LONG).show();
                handled = true;
                break;
            case MESSAGE_MAKE_TOAST:
                if (SHOW_DEBUG_MESSAGE) {
                    String msg = param1Message.getData().getString(BUNDLE_KEY_TOAST_MSG);
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                }
                handled = true;
                break;
            case MESSAGE_RECONNECT_TO_CAMERA:
                if (!mTcpSocket.isConnected() && !handler.hasMessages(MESSAGE_RECONNECT_TO_CAMERA)) {
                    (new Thread(connectRunnable)).start();
                }
                break;
            default:
                return false;
        }
        return handled;
    }

    private void sendToastMessage(String str) {
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_KEY_TOAST_MSG, str);

        Message msg = handler.obtainMessage(MESSAGE_MAKE_TOAST);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public void drawH264View(byte[] data, int len) {
        mH264View.decodeOneFrame(data, len);
    }

    public void writeH264File(byte[] data, int len) {
        try {
            mH264DataFile.write(data, 0, len);
        } catch (IOException e) {
            AppLog.e(TAG, "writeH264File failed!");
        }
    }

    private void stopWriteH264File() {
        if (mTcpSocket != null) mTcpSocket.startWriteToFile(false);
        mH264DataFile = null;
    }

    private static String getDateTimeStr() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH_mm_ss");
        Date date = new Date();
        String dateStr = simpleDateFormat.format(date);
        return dateStr;
    }

    private void prepareH264File() {
        File file = new File(this.getExternalFilesDir(null)
                + "/" + getDateTimeStr() + "_.mp4");
        try {
            mH264DataFile = new RandomAccessFile(file, "rw");
        } catch (FileNotFoundException e) {
            AppLog.e(TAG, "prepareH264File failed!");
        }

    }

    private void enableRecordingButtonBlinking(boolean blinking) {
        if (mRecordVideoView == null) return;
        if (blinking) {
            final Animation animation = new AlphaAnimation(1, 0); // Change alpha from fully visible to invisible
            animation.setDuration(800); // duration - half a second
            animation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
            animation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
            animation.setRepeatMode(Animation.REVERSE); //
            mRecordVideoView.setAnimation(animation);
            mRecordVideoView.setImageResource(R.drawable.recording);

            mRecordVideoBtn.setImageResource(R.drawable.video_record_start);
        } else {
            mRecordVideoView.setAnimation(null);
            mRecordVideoView.setImageResource(0);

            mRecordVideoBtn.setImageResource(R.drawable.video_record_stop);
        }
    }

    private void setupSeekBarListener() {
        verticalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新显示的值
                //valueTextView.setText("值: " + progress);

                // 操控舵机
                updateProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时的操作
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止拖动时的操作
            }
        });
    }

    private void updateProgress(int progress) {
        // 根据滑块进度调整背景效果
        float alpha = 0.5f + (progress / 200.0f); // 透明度在0.5-1.0之间变化
        //backgroundImage.setAlpha(alpha);

        // 可以根据进度添加更多效果，比如亮度、颜色等
    }

    public void handleCmd(byte[] data, int len) {

    }

    public TcpSocket getTcpSocket() {
        return mTcpSocket;
    }

    private String getSharedPreference(String key, String def) {
        //return AndRovio.getSharedPreference(key);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getString(key, def);
    }

}
