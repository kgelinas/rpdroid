package com.radiopirate.lib.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.radiopirate.lib.delegates.RadioSettingsDelegate;

public class MediaControlEventReceiver extends BroadcastReceiver {
    private static final String TAG = "RadioPirate";

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            /* handle media button intent here by reading contents */
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int action = event.getAction();
            if (action == KeyEvent.ACTION_DOWN) {
                Log.e(TAG, "Key pressed KeyCode:" + event.getKeyCode());
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_HEADSETHOOK :
                    case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE :
                        sendAction(context, ServiceActions.PLAY_PAUSE);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_PREVIOUS :
                        sendAction(context, ServiceActions.PREVIOUS);
                        break;
                    case KeyEvent.KEYCODE_MEDIA_NEXT :
                        sendAction(context, ServiceActions.NEXT);
                        break;
                }
            }
        } else {
            Log.e(TAG, "MediaControlEventReceiver - Unknown action:" + intent.getAction());
        }
    }

    private void sendAction(Context context, ServiceActions action) {
        Intent intent = new Intent(RadioSettingsDelegate.getInstance().getServiceAction(action));
        context.sendBroadcast(intent);
    }
}
