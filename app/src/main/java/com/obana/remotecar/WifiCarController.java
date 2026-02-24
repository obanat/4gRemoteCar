package com.obana.remotecar;

import android.view.KeyEvent;
import android.view.View;
import com.obana.remotecar.JoystickView;
import com.obana.remotecar.utils.Constant;

import java.net.Socket;


/* loaded from: classes.dex */
public class WifiCarController {
    private boolean moveTashRunning = true;
    private int moveFlag = 0;
    private TcpSocket socket;
    private Thread movingTask = new Thread() { // from class: com.bigeye.WifiCarController.1
        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (WifiCarController.this.moveTashRunning) {
                try {
                    if (WifiCarController.this.moveFlag > 0 && socket != null) {
                        switch (WifiCarController.this.moveFlag) {
                            case 1:
                                socket.sendCmd(Constant.CMD_MOVE_STOP);
                                break;
                            case 2:
                                socket.sendCmd(Constant.CMD_MOVE_UP);
                                break;
                            case 3:
                                socket.sendCmd(Constant.CMD_MOVE_DOWN);
                                break;
                            case 4:
                                socket.sendCmd(Constant.CMD_MOVE_LEFT);
                                break;
                            case 5:
                                socket.sendCmd(Constant.CMD_MOVE_RIGHT);
                                break;
                            default:
                                socket.sendCmd(Constant.CMD_MOVE_STOP);
                                break;
                        }
                        WifiCarController.this.moveFlag = 0;
                    }
                    Thread.sleep(300L);
                } catch (Exception iOException) {

                    return;
                }
            }
        }
    };
    int speed = 10;
    JoystickView.JoystickMovedListener _listener = new JoystickView.JoystickMovedListener() { // from class: com.bigeye.WifiCarController.2
        @Override // com.bigeye.JoystickView.JoystickMovedListener
        public void OnMoved(int x, int y) {
            WifiCarController.this.moveToPoint(x, y);
        }

        @Override // com.bigeye.JoystickView.JoystickMovedListener
        public void OnReleased() {
            WifiCarController.this.backToInit();
        }

        @Override // com.bigeye.JoystickView.JoystickMovedListener
        public void OnReturnedToCenter() {
            WifiCarController.this.backToInit();
        }
    };

    public WifiCarController(MainActivity activity) {

        //socket = activity!= null ? activity.getTcpSocket():null;
        new Thread(this.movingTask).start();
    }

    public void updateCmdSocket(TcpSocket sk) {
        socket = sk;
    }

    public void init(View view) {
        if (view != null) {
            JoystickView joyView = (JoystickView) view;
            joyView.setOnJostickMovedListener(this._listener);
        }
    }

    public void backToInit() {
        try {
            this.moveFlag = 1;
        } catch (Exception e) {
        }
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        int action = event.getAction();
        if (action == 1) {
            if (keyCode == 102) {
                this.speed++;
                if (this.speed > 10) {
                    this.speed = 10;
                }
            } else if (keyCode == 104) {
                this.speed--;
                if (this.speed < 2) {
                    this.speed = 2;
                }
            }
        }
    }

    public void moveToPoint(int x, int y) {
        try {
            int distance = (int) Math.round(calculateDistance(x, y));
            if (distance >= 1) {
                double theta = CalculateAngle(x, y);
                if (MOVEMENT_ANGLES.FOWARD.isInDirection(theta)) {
                    this.moveFlag = 3;
                } else if (MOVEMENT_ANGLES.RIGHT.isInDirection(theta)) {
                    this.moveFlag = 5;
                } else if (MOVEMENT_ANGLES.LEFT.isInDirection(theta)) {
                    this.moveFlag = 4;
                } else if (MOVEMENT_ANGLES.BACKWARD.isInDirection(theta)) {
                    this.moveFlag = 2;
                }
            }
            //this.moveFlag = 1;
        } catch (Exception e) {
        }
    }

    private double CalculateAngle(int x, int y) {
        if (x == 0 && y > 0) {
            return 90.0d;
        }
        if (x == 0 && y < 0) {
            return 270.0d;
        }
        double theta = ((Math.atan(y / x) * 360.0d) / 2.0d) / 3.141592653589793d;
        if (x >= 0 && y >= 0) {
            if (theta < 22.5d) {
                return theta + 360.0d;
            }
            return theta;
        } else if (x < 0 && y >= 0) {
            return theta + 180.0d;
        } else {
            if (x < 0 && y < 0) {
                return theta + 180.0d;
            }
            if (x > 0 && y < 0) {
                return theta + 360.0d;
            }
            return theta;
        }
    }

    private double calculateDistance(int x, int y) {
        return Math.sqrt(Math.pow(x, 2.0d) + Math.pow(y, 2.0d));
    }

    /* loaded from: classes.dex */
    public enum MOVEMENT_ANGLES {
        FOWARD(135.0d, 46.0d),
        LEFT(225.0d, 136.0d),
        BACKWARD(315.0d, 226.0d),
        RIGHT(405.0d, 316.0d);

        private final double leftAngle;
        private final double rightAngle;



        MOVEMENT_ANGLES(double leftAngle, double rightAngle) {
            this.rightAngle = rightAngle;
            this.leftAngle = leftAngle;
        }

        public boolean isInDirection(double angle) {
            return this.leftAngle >= angle && angle >= this.rightAngle;
        }
    }
}