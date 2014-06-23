package com.radiopirate.lib.delegates;

public abstract class ResourceDelegate {

    private static ResourceDelegate _delegate = null;

    public static void setDelegate(ResourceDelegate delegate) {
        _delegate = delegate;
    }

    public static ResourceDelegate getInstance() {
        return _delegate;
    }

    abstract public int getIdTitle();
    abstract public int getIdStream();
    abstract public int getIdDescription();
    abstract public int getIdImage();

    abstract public int getStringNotificationTitle();
    abstract public int getStringNotificationBuffering();

    abstract public int getLayoutOngoingNotification();

    abstract public int getDrawableIcon();
    abstract public int getDrawableic_status();
    abstract public int getDrawableRemoteControlBackground();

    abstract public String getNotificationPackageName();
    abstract public String getNotificationClassName();
}
