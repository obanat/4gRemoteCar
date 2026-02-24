package com.obana.remotecar.JJRC;

import android.content.Context;

import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

import com.fh.lib.FHSDK;

/* loaded from: classes.dex */
public class GLFrameSurface extends GLSurfaceView {

    float[] accelerometerValues;
 
    private float hDegrees;

    private GLFrameRenderer mFrameRender;

    float[] magneticFieldValues;


    private float vDegrees;


    public GLFrameSurface(Context context) {
        super(context);

        this.hDegrees = -1.0f;
        this.vDegrees = -1.0f;
      
        this.accelerometerValues = new float[3];
        this.magneticFieldValues = new float[3];

        
        setEGLContextClientVersion(2);

        this.mFrameRender = GLFrameRenderer.getInstance();

    }

  
    public GLFrameSurface(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.hDegrees = -1.0f;
        this.vDegrees = -1.0f;

        this.accelerometerValues = new float[3];
        this.magneticFieldValues = new float[3];

    }

    @Override // android.opengl.GLSurfaceView, android.view.SurfaceView, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }


    public void update(float f, float f2, float f3, float f4) {

        if (3 == GLFrameRenderer.eyeMode) {

            if (GLFrameRenderer.displayMode == 0) {
                this.vDegrees = f - f3;
                this.hDegrees = f2 - f4;
                float f5 = this.vDegrees;

                if (f5 < FHSDK.getMaxVDegress(GLFrameRenderer.hWin)) {

                    this.vDegrees = FHSDK.getMaxVDegress(GLFrameRenderer.hWin);
                } else if (this.vDegrees > 0.0f) {
                    this.vDegrees = 0.0f;
                }

                GLFrameRenderer.vDegrees = this.vDegrees;

                GLFrameRenderer.hDegrees = this.hDegrees;
                return;
            }

            if (6 == GLFrameRenderer.displayMode) {
                this.vDegrees = (f - f3) + 90.0f;
                this.hDegrees = f2 - f4;
                float f6 = this.vDegrees;

                if (f6 < FHSDK.getMaxVDegress(GLFrameRenderer.hWin)) {
   
                    this.vDegrees = FHSDK.getMaxVDegress(GLFrameRenderer.hWin);
                } else {
                    float f7 = this.vDegrees;
    
                    if (f7 > FHSDK.getMinVDegress(GLFrameRenderer.hWin)) {

                        this.vDegrees = FHSDK.getMinVDegress(GLFrameRenderer.hWin);
                    }
                }

                GLFrameRenderer.vDegrees = this.vDegrees;

                GLFrameRenderer.hDegrees = this.hDegrees;
            }
        }
    }
}