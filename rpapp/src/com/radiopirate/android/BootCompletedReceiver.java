package com.radiopirate.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.radiopirate.lib.service.PlaybackService;

public class BootCompletedReceiver extends BroadcastReceiver {

    private static final String TAG = "RadioPirate";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

            SharedPreferences myPref = PreferenceManager.getDefaultSharedPreferences(context);
            boolean autoStart = myPref.getBoolean("prefAutoLaunchService", false);

            if (autoStart) {
                Log.w(TAG, "Auto start the RP service");
                context.startService(new Intent(context, PlaybackService.class));
            }
        }
    }
}
