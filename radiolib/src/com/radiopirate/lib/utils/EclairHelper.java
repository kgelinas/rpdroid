package com.radiopirate.lib.utils;

import android.app.Notification;
import android.app.Service;
import android.content.pm.PackageManager;

public class EclairHelper {

    public static void startForeground(Service service, int id, Notification notification) {
        service.startForeground(id, notification);
    }

    public static boolean doesSupportGoogleTV(PackageManager manager) {
        return manager.hasSystemFeature("com.google.android.tv");
    }
}
