package com.radiopirate.lib.service;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class Wifilock {
    private WifiLock mWifiLock;
    private WakeLock mWakeLock;
    private static final String TAG = "RP Lock";

    public Wifilock() {
    }

    public Wifilock(Context context) {

        ContextWrapper wrapper = new ContextWrapper(context);
        PowerManager pm = (PowerManager) wrapper.getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RadioPirate.com Player");
        WifiManager wm = (WifiManager) wrapper.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock("RadioPirate.com Player");

    }

    public void lock(boolean usingWifi) {
        mWakeLock.acquire();

        if (usingWifi) {
            mWifiLock.acquire();
            Log.e(TAG, "Wifi Acquired");
        }
        Log.e(TAG, "Lock Started");
    }

    public void release() {
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
            Log.e(TAG, "Lock Released");
        }

        if (mWifiLock.isHeld()) {
            mWifiLock.release();
            Log.e(TAG, "Wifi Released");
        }
    }
}