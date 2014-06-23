package com.radiopirate.lib.utils;

import android.os.Build;

public class CupcakeHelper {

    public static boolean isCupcake() {
        return Build.VERSION.SDK.equals("3");
    }
}
