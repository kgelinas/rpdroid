package com.radiopirate.android.delegate;

import com.radiopirate.android.R;
import com.radiopirate.lib.delegates.ResourceDelegate;

public class RPResource extends ResourceDelegate {

    public int getIdTitle() {
        return R.id.title;
    }

    public int getIdStream() {
        return R.id.stream;
    }

    public int getIdDescription() {
        return R.id.description;
    }

    public int getIdImage() {
        return R.id.image;
    }

    public int getStringNotificationTitle() {
        return R.string.notification_title;
    }

    public int getStringNotificationBuffering() {
        return R.string.notification_buffering;
    }

    public int getLayoutOngoingNotification() {
        return R.layout.ongoing_notification;
    }

    public int getDrawableIcon() {
        return R.drawable.icon;
    }

    public int getDrawableic_status() {
        return R.drawable.ic_status;
    }

    public String getNotificationPackageName() {
        return "com.radiopirate.android";
    }

    public String getNotificationClassName() {
        return "com.radiopirate.android.StartUp";
    }

    public int getDrawableRemoteControlBackground() {
        return R.drawable.banner_rp;
    }
}
