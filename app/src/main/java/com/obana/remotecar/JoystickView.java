package com.obana.remotecar;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.view.MotionEventCompat;

/* loaded from: classes.dex */
public class JoystickView extends View {
    public static final int CONSTRAIN_BOX = 0;
    public static final int CONSTRAIN_CIRCLE = 1;
    public static final int COORDINATE_CARTESIAN = 0;
    public static final int COORDINATE_DIFFERENTIAL = 1;
    public static final int INVALID_POINTER_ID = -1;
    private final boolean D;
    String TAG;
    private double angle;
    private boolean autoReturnToCenter;
    private Paint bgPaint;
    private int bgRadius;
    private int cX;
    private int cY;
    private int cartX;
    private int cartY;
    private JoystickClickedListener clickListener;
    private float clickThreshold;
    private boolean clicked;
    private Paint dbgPaint1;
    private Paint dbgPaint2;
    private int dimX;
    private int dimY;
    private int handleInnerBoundaries;
    public Paint handlePaint;
    private int handleRadius;
    private float handleX;
    private float handleY;
    private int innerPadding;
    private JoystickMovedListener moveListener;
    private float moveResolution;
    private int movementConstraint;
    private int movementRadius;
    private float movementRange;
    private int offsetX;
    private int offsetY;
    private int pointerId;
    private double radial;
    private float reportX;
    private float reportY;
    private float touchPressure;
    private float touchX;
    private float touchY;
    private int userCoordinateSystem;
    private int userX;
    private int userY;
    private boolean yAxisInverted;

    /* loaded from: classes.dex */
    public interface JoystickClickedListener {
        void OnClicked();

        void OnReleased();
    }

    /* loaded from: classes.dex */
    public interface JoystickMovedListener {
        void OnMoved(int i, int i2);

        void OnReleased();

        void OnReturnedToCenter();
    }

    public JoystickView(Context context) {
        super(context);
        this.D = false;
        this.TAG = "JoystickView";
        this.pointerId = -1;
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.D = false;
        this.TAG = "JoystickView";
        this.pointerId = -1;
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.D = false;
        this.TAG = "JoystickView";
        this.pointerId = -1;
        initJoystickView();
    }

    private void initJoystickView() {
        setFocusable(true);
        this.dbgPaint1 = new Paint(1);
        this.dbgPaint1.setColor(Color.rgb(0, 153, 204));
        this.dbgPaint1.setStrokeWidth(1.0f);
        this.dbgPaint1.setStyle(Paint.Style.STROKE);
        this.dbgPaint2 = new Paint(1);
        this.dbgPaint2.setColor(-16711936);
        this.dbgPaint2.setStrokeWidth(1.0f);
        this.dbgPaint2.setStyle(Paint.Style.STROKE);
        this.bgPaint = new Paint(1);
        this.bgPaint.setColor(-7829368);
        this.bgPaint.setAlpha(100);
        this.bgPaint.setStrokeWidth(1.0f);
        this.bgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.handlePaint = new Paint(1);
        this.handlePaint.setColor(-12303292);
        this.handlePaint.setStrokeWidth(1.0f);
        this.handlePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        this.innerPadding = 10;
        setMovementRange(10.0f);
        setMoveResolution(1.0f);
        setClickThreshold(0.4f);
        setYAxisInverted(true);
        setUserCoordinateSystem(0);
        setAutoReturnToCenter(true);
    }

    public void setAutoReturnToCenter(boolean autoReturnToCenter) {
        this.autoReturnToCenter = autoReturnToCenter;
    }

    public boolean isAutoReturnToCenter() {
        return this.autoReturnToCenter;
    }

    public void setUserCoordinateSystem(int userCoordinateSystem) {
        if (userCoordinateSystem < 0 || this.movementConstraint > 1) {
            Log.e(this.TAG, "invalid value for userCoordinateSystem");
        } else {
            this.userCoordinateSystem = userCoordinateSystem;
        }
    }

    public int getUserCoordinateSystem() {
        return this.userCoordinateSystem;
    }

    public void setMovementConstraint(int movementConstraint) {
        if (movementConstraint < 0 || movementConstraint > 1) {
            Log.e(this.TAG, "invalid value for movementConstraint");
        } else {
            this.movementConstraint = movementConstraint;
        }
    }

    public int getMovementConstraint() {
        return this.movementConstraint;
    }

    public boolean isYAxisInverted() {
        return this.yAxisInverted;
    }

    public void setYAxisInverted(boolean yAxisInverted) {
        this.yAxisInverted = yAxisInverted;
    }

    public void setClickThreshold(float clickThreshold) {
        if (clickThreshold < 0.0f || clickThreshold > 1.0f) {
            Log.e(this.TAG, "clickThreshold must range from 0...1.0f inclusive");
        } else {
            this.clickThreshold = clickThreshold;
        }
    }

    public float getClickThreshold() {
        return this.clickThreshold;
    }

    public void setMovementRange(float movementRange) {
        this.movementRange = movementRange;
    }

    public float getMovementRange() {
        return this.movementRange;
    }

    public void setMoveResolution(float moveResolution) {
        this.moveResolution = moveResolution;
    }

    public float getMoveResolution() {
        return this.moveResolution;
    }

    public void setOnJostickMovedListener(JoystickMovedListener listener) {
        this.moveListener = listener;
    }

    public void setOnJostickClickedListener(JoystickClickedListener listener) {
        this.clickListener = listener;
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = measure(widthMeasureSpec);
        int measuredHeight = measure(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    @Override // android.view.View
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        int d = Math.min(getMeasuredWidth(), getMeasuredHeight());
        this.dimX = d;
        this.dimY = d;
        this.cX = d / 2;
        this.cY = d / 2;
        this.bgRadius = (this.dimX / 2) - this.innerPadding;
        this.handleRadius = (int) (d * 0.3d);
        this.handleInnerBoundaries = this.handleRadius;
        this.movementRadius = Math.min(this.cX, this.cY) - this.handleInnerBoundaries;
    }

    private int measure(int measureSpec) {
        int specMode = View.MeasureSpec.getMode(measureSpec);
        int specSize = View.MeasureSpec.getSize(measureSpec);
        if (specMode == 0) {
            return 200;
        }
        return specSize;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.drawCircle(this.cX, this.cY, this.bgRadius, this.bgPaint);
        this.handleX = this.touchX + this.cX;
        this.handleY = this.touchY + this.cY;
        canvas.drawCircle(this.handleX, this.handleY, this.handleRadius, this.handlePaint);
        canvas.restore();
    }

    private void constrainBox() {
        this.touchX = Math.max(Math.min(this.touchX, this.movementRadius), -this.movementRadius);
        this.touchY = Math.max(Math.min(this.touchY, this.movementRadius), -this.movementRadius);
    }

    private void constrainCircle() {
        float diffX = this.touchX;
        float diffY = this.touchY;
        double radial = Math.sqrt((diffX * diffX) + (diffY * diffY));
        if (radial > this.movementRadius) {
            this.touchX = (int) ((diffX / radial) * this.movementRadius);
            this.touchY = (int) ((diffY / radial) * this.movementRadius);
        }
    }

    public void setPointerId(int id) {
        this.pointerId = id;
    }

    public int getPointerId() {
        return this.pointerId;
    }

    @Override // android.view.View
    public boolean onGenericMotionEvent(MotionEvent event) {
        Log.e("wificar", "onGenericMotionEvent:" + event);
        int action = event.getAction();
        switch (action & 255) {
            case 2:
                int pointerIndex = event.findPointerIndex(0);
                float x = event.getX(pointerIndex);
                this.touchX = this.movementRadius * x;
                float y = event.getY(pointerIndex);
                this.touchY = this.movementRadius * y;
                reportOnMoved();
                invalidate();
                this.touchPressure = event.getPressure(pointerIndex);
                reportOnPressure();
                break;
        }
        return true;
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent ev) {
        int x;
        int action = ev.getAction();
        switch (action & 255) {
            case 0:
                if (this.pointerId == -1 && (x = (int) ev.getX()) >= this.offsetX && x < this.offsetX + this.dimX) {
                    setPointerId(ev.getPointerId(0));
                    return true;
                }
                break;
            case 1:
            case 3:
                if (this.pointerId != -1) {
                    returnHandleToCenter();
                    setPointerId(-1);
                    break;
                }
                break;
            case 2:
                return processMoveEvent(ev);
            case 5:
                if (this.pointerId == -1) {
                    int pointerIndex = (action & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
                    int pointerId = ev.getPointerId(pointerIndex);
                    int x2 = (int) ev.getX(pointerId);
                    if (x2 >= this.offsetX && x2 < this.offsetX + this.dimX) {
                        setPointerId(pointerId);
                        return true;
                    }
                }
                break;
            case 6:
                if (this.pointerId != -1) {
                    int pointerIndex2 = (action & MotionEventCompat.ACTION_POINTER_INDEX_MASK) >> 8;
                    if (ev.getPointerId(pointerIndex2) == this.pointerId) {
                        returnHandleToCenter();
                        setPointerId(-1);
                        return true;
                    }
                }
                break;
        }
        return false;
    }

    private boolean processMoveEvent(MotionEvent ev) {
        if (this.pointerId != -1) {
            int pointerIndex = ev.findPointerIndex(this.pointerId);
            float x = ev.getX(pointerIndex);
            this.touchX = (x - this.cX) - this.offsetX;
            float y = ev.getY(pointerIndex);
            this.touchY = (y - this.cY) - this.offsetY;
            reportOnMoved();
            invalidate();
            this.touchPressure = ev.getPressure(pointerIndex);
            reportOnPressure();
            return true;
        }
        return false;
    }

    public void reportOnMoved() {
        if (this.movementConstraint == 1) {
            constrainCircle();
        } else {
            constrainBox();
        }
        calcUserCoordinates();
        if (this.moveListener != null) {
            boolean rx = Math.abs(this.touchX - this.reportX) >= this.moveResolution;
            boolean ry = Math.abs(this.touchY - this.reportY) >= this.moveResolution;
            if (rx || ry) {
                this.reportX = this.touchX;
                this.reportY = this.touchY;
                this.moveListener.OnMoved(this.userX, this.userY);
            }
        }
    }

    private void calcUserCoordinates() {
        this.cartX = (int) ((this.touchX / this.movementRadius) * this.movementRange);
        this.cartY = (int) ((this.touchY / this.movementRadius) * this.movementRange);
        this.radial = Math.sqrt((this.cartX * this.cartX) + (this.cartY * this.cartY));
        this.angle = Math.atan2(this.cartY, this.cartX);
        if (!this.yAxisInverted) {
            this.cartY *= -1;
        }
        if (this.userCoordinateSystem == 0) {
            this.userX = this.cartX;
            this.userY = this.cartY;
        } else if (this.userCoordinateSystem == 1) {
            this.userX = this.cartY + (this.cartX / 4);
            this.userY = this.cartY - (this.cartX / 4);
            if (this.userX < (-this.movementRange)) {
                this.userX = (int) (-this.movementRange);
            }
            if (this.userX > this.movementRange) {
                this.userX = (int) this.movementRange;
            }
            if (this.userY < (-this.movementRange)) {
                this.userY = (int) (-this.movementRange);
            }
            if (this.userY > this.movementRange) {
                this.userY = (int) this.movementRange;
            }
        }
    }

    private void reportOnPressure() {
        if (this.clickListener != null) {
            if (this.clicked && this.touchPressure < this.clickThreshold) {
                this.clickListener.OnReleased();
                this.clicked = false;
                invalidate();
            } else if (!this.clicked && this.touchPressure >= this.clickThreshold) {
                this.clicked = true;
                this.clickListener.OnClicked();
                invalidate();
                performHapticFeedback(1);
            }
        }
    }

    private void returnHandleToCenter() {
        if (this.autoReturnToCenter) {
            final double intervalsX = (0.0f - this.touchX) / 5.0f;
            final double intervalsY = (0.0f - this.touchY) / 5.0f;
            for (int i = 0; i < 5; i++) {
                final int j = i;
                postDelayed(new Runnable() { // from class: com.bigeye.JoystickView.1
                    @Override // java.lang.Runnable
                    public void run() {
                        JoystickView joystickView = JoystickView.this;
                        joystickView.touchX = (float) (joystickView.touchX + intervalsX);
                        JoystickView joystickView2 = JoystickView.this;
                        joystickView2.touchY = (float) (joystickView2.touchY + intervalsY);
                        JoystickView.this.reportOnMoved();
                        JoystickView.this.invalidate();
                        if (JoystickView.this.moveListener != null && j == 4) {
                            JoystickView.this.moveListener.OnReturnedToCenter();
                        }
                    }
                }, i * 40);
            }
            if (this.moveListener != null) {
                this.moveListener.OnReleased();
            }
        }
    }

    public void setTouchOffset(int x, int y) {
        this.offsetX = x;
        this.offsetY = y;
    }
}