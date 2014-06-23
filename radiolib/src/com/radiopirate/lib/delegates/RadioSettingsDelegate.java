package com.radiopirate.lib.delegates;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Resources;

import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.service.ServiceActions;

public abstract class RadioSettingsDelegate {

    private static RadioSettingsDelegate _delegate = null;

    public static void setDelegate(RadioSettingsDelegate delegate) {
        _delegate = delegate;
    }

    public static RadioSettingsDelegate getInstance() {
        return _delegate;
    }

    abstract public String getBugSenseAPIkey();
    abstract public ArrayList<PodcastEntry> getPodcasts();
    abstract public boolean isGuest();
    abstract public String getChannelDescription(Resources res, int channelId);
    abstract public boolean isACC(int channel);
    abstract public int getChannelCount();
    abstract public String getChannelURL(int channel, boolean isWifi);
    abstract public String getChannelURL(int channel);
    abstract public String getChannelName(Resources res, int channelId);
    abstract public String getChannelName(Resources res, int channelId, boolean isWifi);
    abstract public boolean canGetIcyStreamMeta(int channel);
    abstract public boolean isPodcastMP3();
    abstract public String getServiceAction(ServiceActions action);

    /*
     * Return the length of the buffer in seconds
     */
    abstract public int getBufferLength(Context context);
}
