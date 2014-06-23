package com.radiopirate.android.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.bugsense.trace.BugSenseHandler;
import com.radiopirate.android.R;
import com.radiopirate.android.RPSettings;
import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceActions;
import com.radiopirate.lib.service.ServiceMessages;

public class RPAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "RP - Widget";

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        BugSenseHandler.setup(context, RPSettings.BUG_SENSE_API_KEY);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(
                RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.STATUS_UPDATE))) {

            Bundle extras = intent.getExtras();
            if (extras != null) {
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
                        RPAppWidgetProvider.class.getName());
                int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

                final int N = appWidgetIds.length;
                for (int i = 0; i < N; i++) {
                    onStatusUpdate(context, appWidgetManager, appWidgetIds[i], extras);
                }
            } else {
                Log.e(TAG, "onReceive - received no extra");
            }
        } else if (intent.getAction().equals("android.appwidget.action.APPWIDGET_UPDATE")) {
            // Called when a widget is added
            Intent broadcastIntent = new Intent(RadioSettingsDelegate.getInstance().getServiceAction(
                    ServiceActions.UPDATE));
            context.sendBroadcast(broadcastIntent);
        }
    }

    private void onStatusUpdate(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle data) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.rp_widget);
        PlaybackStatus status = (PlaybackStatus) data.getSerializable(ServiceMessages.KEY_STATUS);
        if (status == null) {
            Log.e(TAG, "onStatusUpdate - received empty status");
            return;
        }

        views.setTextViewText(R.id.stream, status.getTitle());
        views.setTextViewText(R.id.track, status.getTrackTitle());

        setClickListerner(context, views, RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.STOP),
                R.id.StopBtn);
        setClickListerner(context, views, RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PLAY),
                R.id.PlayBtn);
        setClickListerner(context, views, RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PAUSE),
                R.id.PauseBtn);

        if (status.isPlaying() && !status.isPaused()) {
            if (status.areAdvancedControlSupported()) {
                views.setViewVisibility(R.id.StopBtn, View.GONE);
                views.setViewVisibility(R.id.PlayBtn, View.GONE);
                views.setViewVisibility(R.id.PauseBtn, View.VISIBLE);
            } else {
                views.setViewVisibility(R.id.StopBtn, View.VISIBLE);
                views.setViewVisibility(R.id.PlayBtn, View.GONE);
                views.setViewVisibility(R.id.PauseBtn, View.GONE);
            }
        } else {
            if (status.getURL().length() > 0) {
                views.setViewVisibility(R.id.StopBtn, View.GONE);
                views.setViewVisibility(R.id.PlayBtn, View.VISIBLE);
                views.setViewVisibility(R.id.PauseBtn, View.GONE);
            } else {
                views.setViewVisibility(R.id.StopBtn, View.GONE);
                views.setViewVisibility(R.id.PlayBtn, View.GONE);
                views.setViewVisibility(R.id.PauseBtn, View.GONE);
            }
        }

        // Tell the AppWidgetManager to perform an update on the current app widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    private void setClickListerner(Context context, RemoteViews views, String action, int viewId) {
        Intent intent = new Intent(action);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }
}
