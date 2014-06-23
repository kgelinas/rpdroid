package com.radiopirate.lib.utils;

import android.os.Build;

/**
 * This class is needd because 1.5 does not support SDK_INT, and using a file that contains it causes a VerifyError
 */
public class VersionCheck {

    public static boolean isSDKSmallerThanDunut() {
        return CupcakeHelper.isCupcake() || Build.VERSION.SDK_INT <= Build.VERSION_CODES.DONUT;
    }

    public static boolean isGreaterThanEclair() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR;
    }

    public static boolean isFroyoOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean isLessThanFroyo() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO;
    }

    public static boolean isLessThanGingerbread() {
        // Gingerbread >= 2.3 , 2.3.1 , 2.3.2
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean isHoneycombOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isICSOrGreater() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }
}
