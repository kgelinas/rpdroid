package com.radiopirate.android;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import com.radiopirate.android.RPAPI.LoginResult;
import com.radiopirate.android.RPAPI.StreamFormat;
import com.radiopirate.android.fragments.ChannelEntry;
import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.ServiceActions;

/**
 * Holds the hard-coded settings To support another Radio, this should be the main class that needs to be changed, other
 * than the XML files
 */
public class RPSettings extends RadioSettingsDelegate {

    public static final String BUG_SENSE_API_KEY = "c1970594";

    private static final boolean SHOW_CHANNEL_IMAGE = false; // Set to true to allow users to see channel image
    private static final boolean ENABLE_FAVORITES = false;
    public static final boolean ENABLE_EXTRA_CHANNELS = false;
    private static boolean MP3_PODCASTS = true; // true to use MP3 podcast, false to use ACC
    public static final int REMAINING_WARNING = 30;

    private String username;
    private RPAPI api;
    private Context context;

    public RPSettings(Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        username = preferences.getString(RPPreferences.PREF_USERNAME, "");
        api = new RPAPI(getSiteURL(), getAPIURL());
        this.context = context;
    }

    public LoginResult fetchConfig(final String username, final String password) {
        this.username = username;
        return api.fetchConfig(username, password);
    }

    public LoginResult fetchGuestConfig() {
        username = "";
        return api.fetchConfig();
    }

    public String getBugSenseAPIkey() {
        return BUG_SENSE_API_KEY;
    }

    public String getRemainingDays() {
        return api.getRemainingDays();
    }

    public int getChannelCount() {
        return api.getStreamCount();
    }

    public String getChannelName(Resources res, int channelId) {
        return api.getStreamTitle(res, channelId - 1);
    }

    public String getChannelDescription(Resources res, int channelId) {
        return api.getStreamDescription(res, channelId - 1);
    }

    /**
     * 
     * @param channel
     * @return True if the channel is a ACC stream
     */
    public boolean isACC(int channel) {
        return api.getStreamFormat(channel - 1) == StreamFormat.ACC;
    }

    /**
     * This is needed because the RP channels limit the user to only one connection. If we establish a connection to get
     * the playing song, the audio connection drops
     * 
     * @return False if getting the stream metadata would cause the connection to drop.
     */
    public boolean canGetIcyStreamMeta(int channel) {
        // Getting the metadata from a MP3 stream from RP makes the playback stop
        return channel >= api.getBaseStreamCount() || isACC(channel);
    }

    public static boolean showChannelImage() {
        return SHOW_CHANNEL_IMAGE;
    }

    public static boolean areFavoritesEnabled() {
        return ENABLE_FAVORITES;
    }

    public static int getChannelImage(int channel) {
        switch (channel) {
            case 1 :
            case 3 :
            case 7 :
            case 8 :
            case 9 :
            case 10 :
            case 12 :
            case 13 :
                return R.drawable.icon;
            case 2 :
            case 4 :
            case 5 :
            case 6 :
            case 14 :
            case 15 :
            case 16 :
            case 17 :
                return R.drawable.logo_181;
            case 11 :
                return R.drawable.logo_metal;
        }

        return R.drawable.icon;
    }

    public static boolean isChannelActive(int channel) {
        return true;
    }

    public String getChannelName(Resources res, int channelId, boolean isWifi) {
        return getChannelName(res, channelId);
    }

    public String getChannelURL(int channel, boolean isWifi) {
        return getChannelURL(channel);
    }

    /**
     * @param channel
     * @return URL of the channel including all needed parameters
     */
    public String getChannelURL(int channel) {
        return api.getStreamURL(channel - 1);
    }

    public boolean isPodcastMP3() {
        return RPSettings.areRPPodcastsMP3();
    }

    @Override
    public ArrayList<PodcastEntry> getPodcasts() {
        return getPodcasts(PodcastSource.RP);
    }

    public ArrayList<PodcastEntry> getPodcasts(PodcastSource source) {
        ArrayList<PodcastEntry> podcasts = api
                .getPodcasts(isPodcastMP3() ? StreamFormat.MP3 : StreamFormat.ACC, source);

        PodcastOpenHelper database = new PodcastOpenHelper(context);
        database.loadCachedData(podcasts, source);

        return podcasts;
    }

    public static boolean areRPPodcastsMP3() {
        return MP3_PODCASTS;
    }

    public static String getSiteURL() {
        return "http://www.radiopirate.com";
    }

    public String getAPIURL() {

        return getSiteURL() + "/mobile/api/2/";
    }

    public ArrayList<ChannelEntry> getChannelList(Resources res, boolean isGoogleTV) {
        ArrayList<ChannelEntry> channels = new ArrayList<ChannelEntry>();

        for (int i = 1; i < getChannelCount() + 1; ++i) {

            if (isGoogleTV && isACC(i)) {
                // GoogleTV does not support the NDK so we can't play ACC channels
                continue;
            }

            if (RPSettings.isChannelActive(i)) {
                channels.add(new ChannelEntry(i, getChannelName(res, i)));
            }
        }

        return channels;
    }

    public boolean isGuest() {
        return username == null || username.length() == 0;
    }

    @Override
    public String getServiceAction(ServiceActions action) {
        switch (action) {
            case STOP :
                return "com.radiopirate.android.ACTION_STOP";
            case PLAY :
                return "com.radiopirate.android.ACTION_PLAY";
            case PAUSE :
                return "com.radiopirate.android.ACTION_PAUSE";
            case UPDATE :
                return "com.radiopirate.android.ACTION_UPDATE";
            case DOWNLOAD_PODCAST :
                return "com.radiopirate.android.ACTION_DOWNLOAD_PODCAST";
            case STATUS_UPDATE :
                return "com.radiopirate.android.STATUS_UPDATE";
            case PLAY_PAUSE :
                return "com.radiopirate.android.ACTION_PLAY_PAUSE";
            case PREVIOUS :
                return "com.radiopirate.android.ACTION_PREVIOUS";
            case NEXT :
                return "com.radiopirate.android.ACTION_NEXT";
        }

        return "";
    }

    @Override
    public int getBufferLength(Context context) {
        Resources res = context.getResources();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int bufferLength;
        bufferLength = Integer.parseInt(preferences.getString(RPPreferences.PREF_BUFFER,
                res.getStringArray(R.array.Buffer_entryValues)[0]));
        return bufferLength;
    }
}
