package com.example.gnssanalyzerplus;

import android.app.Application;
import android.content.Context;
import android.os.PowerManager;
import android.util.Log;

public class App extends Application {

//    private static final String TAG = App.class.getSimpleName();
//    private PowerManager.WakeLock mWakeLock = null;

    @Override
    public void onCreate() {
        super.onCreate();

//        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
//        mWakeLock.acquire();
//        Log.d(TAG, "WAKE LOCK ACQUIRED");
    }


    @Override
    public void onTerminate() {
//        if (mWakeLock.isHeld())
//            mWakeLock.release();
//        Log.d(TAG, "WAKE LOCK RELEASED");

        super.onTerminate();
    }
}
