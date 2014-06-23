package com.radiopirate.lib.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.radiopirate.lib.delegates.ResourceDelegate;

//TODO Music control in notification

class PlaybackNotification {

    private Context context;
    public static final int PLAY_NOTIFICATION_ID = 100;

    public PlaybackNotification(Context context) {
        this.context = context;
    }

    public void clearNotification() {
        getNotificationManager().cancel(PLAY_NOTIFICATION_ID);
    }

    public void displayPlaybackStatus(String stream, String description, String ticker) {
        getNotificationManager().notify(PLAY_NOTIFICATION_ID, createNotification(stream, description, ticker));
    }

    public void displayPlaybackStatus(String stream, String description) {
        displayPlaybackStatus(stream, description, stream);
    }

    public Notification createNotification(String stream, String description, String ticker) {
        ResourceDelegate res = ResourceDelegate.getInstance();
        String title = context.getResources().getString(res.getStringNotificationTitle());

        Notification notification = new Notification(res.getDrawableIcon(), ticker, 0);

        if (res.getLayoutOngoingNotification() != 0) {
            RemoteViews contentView = new RemoteViews(context.getPackageName(), res.getLayoutOngoingNotification());
            contentView.setImageViewResource(res.getIdImage(), res.getDrawableIcon());
            contentView.setTextViewText(res.getIdTitle(), title);
            contentView.setTextViewText(res.getIdStream(), stream);
            contentView.setTextViewText(res.getIdDescription(), description);

            notification.contentView = contentView;
        } else {
            notification.icon = res.getDrawableIcon();
            notification.setLatestEventInfo(context, stream, description, null);
        }

        notification.icon = res.getDrawableic_status();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        notification.tickerText = ticker;

        // Launch app as it is when it is launched from the home screen
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName(res.getNotificationPackageName(), res.getNotificationClassName()));

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        notification.contentIntent = pendingIntent;

        return notification;
    }

    private NotificationManager getNotificationManager() {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }
}
