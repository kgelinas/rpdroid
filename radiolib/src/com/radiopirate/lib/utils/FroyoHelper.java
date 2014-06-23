package com.radiopirate.lib.utils;

import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Environment;

import com.radiopirate.lib.delegates.RadioSettingsDelegate;

/**
 * Helper class to wrap calls to Froyo API so older versions don't see them
 * 
 */
public class FroyoHelper {

    /**
     * 
     * @param context
     * @return True if the phone is in car mode, false otherwise
     */
    public static boolean isCarMode(Context context) {
        if (context == null) {
            return false;
        }

        if (RadioSettingsDelegate.getInstance().isGuest()) {
            return false;
        }

        UiModeManager uimode = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uimode != null && uimode.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            return true;
        }

        return false;
    }

    public static boolean isCarMode(Configuration config) {
        if (config.uiMode == Configuration.UI_MODE_TYPE_CAR) {
            return true;
        }

        return false;
    }

    public static String getPodcastFolder(Context context, String filename) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) + "/" + filename + ".mp3";
    }

    public static void registerMediaButtonEventReceiver(AudioManager audioManager, ComponentName receiverComponentName) {
        audioManager.registerMediaButtonEventReceiver(receiverComponentName);
    }

    public static void unregisterMediaButtonEventReceiver(AudioManager audioManager, ComponentName receiverComponentName) {
        audioManager.unregisterMediaButtonEventReceiver(receiverComponentName);
    }
}
