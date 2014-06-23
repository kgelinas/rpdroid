package com.radiopirate.lib.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.utils.RadioPreferences;

public class AutoDownloader {

    private static final int FETCH_PODCAST_DELAY = 15 * 60 * 1000; // Check for downloads 15min after boot

    public static boolean isAutoPodcastDownload(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(RadioPreferences.PREF_AUTO_DOWNLOAD_PODCAST, false);
    }

    public static void startPodcastDownloadTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingIntent = getDownloadIntent(context);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime()
                + FETCH_PODCAST_DELAY, AlarmManager.INTERVAL_HOUR, pendingIntent);
    }

    public static void cancelPodcastDownloadTimer(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PendingIntent pendingIntent = getDownloadIntent(context);
        alarmManager.cancel(pendingIntent);
    }

    private static PendingIntent getDownloadIntent(Context context) {
        Intent intent = new Intent(RadioSettingsDelegate.getInstance()
                .getServiceAction(ServiceActions.DOWNLOAD_PODCAST));
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}
