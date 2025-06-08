package com.radwrld.wami;

import android.app.Application;
public class LoggerApplication extends Application {
   public void onCreate() {
       super.onCreate();
Logger.initialize(this);
;
   }
}