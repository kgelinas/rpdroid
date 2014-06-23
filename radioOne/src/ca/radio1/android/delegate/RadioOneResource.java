package ca.radio1.android.delegate;

import ca.radio1.android.R;

import com.radiopirate.lib.delegates.ResourceDelegate;

public class RadioOneResource extends ResourceDelegate {

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
        return 0;
    }

    public int getDrawableIcon() {
        return R.drawable.ic_launcher;
    }

    public int getDrawableic_status() {
        return R.drawable.ic_stat_radioone;
    }

    public String getNotificationPackageName() {
        return "ca.radio1.android";
    }

    public String getNotificationClassName() {
        return "ca.radio1.android.Main";
    }

    public int getDrawableRemoteControlBackground() {
        return R.drawable.ic_launcher;
    }
}
