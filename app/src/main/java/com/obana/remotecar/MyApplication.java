package com.obana.remotecar;

import android.app.Application;
import android.view.WindowManager;

import com.obana.remotecar.utils.AppLog;

public class MyApplication extends Application {

  private WindowManager.LayoutParams wmParams = new WindowManager.LayoutParams();
  
  public WindowManager.LayoutParams getMywmParams() {
    return this.wmParams;
  }
}
