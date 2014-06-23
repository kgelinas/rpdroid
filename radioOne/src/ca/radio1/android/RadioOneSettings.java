package ca.radio1.android;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import ca.radio1.android.fragments.ChannelEntry;

import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.service.ServiceActions;
import com.radiopirate.lib.utils.CupcakeHelper;

/**
 * Holds the hard-coded settings To support another Radio, this should be the main class that needs to be changed, other
 * than the XML files
 */
public class RadioOneSettings extends RadioSettingsDelegate {

    public static final String BUG_SENSE_API_KEY = "253e221a";

    private static final String CHANNEL_URL_1 = "http://www.radiopirate.com/streams/listen/stream-radioone.mp3";

    public RadioOneSettings(Context context) {
    }

    public String getBugSenseAPIkey() {
        return BUG_SENSE_API_KEY;
    }

    @Override
    public int getChannelCount() {
        return 1;
    }

    public String getChannelName(Resources res, int channelId) {
        return RadioOneSettings.getRPChannelName(res, channelId);
    }

    public static String getRPChannelName(Resources res, int channelId) {
        return res.getString(R.string.channel_name_1);
    }

    public String getChannelDescription(Resources res, int channelId) {
        return RadioOneSettings.getDescription(res, channelId);
    }

    public static String getDescription(Resources res, int channelId) {
        return res.getString(R.string.channel_desc_1);
    }

    public boolean isACC(int channel) {
        return RadioOneSettings.isChannelACC(channel);
    }

    /**
     * 
     * @param channel
     * @return True if the channel is a ACC stream
     */
    public static boolean isChannelACC(int channel) {
        return false;
    }

    /**
     * This is needed because the RP channels limit the user to only one connection. If we establish a connection to get
     * the playing song, the audio connection drops
     * 
     * @return False if getting the stream metadata would cause the connection to drop.
     */
    public boolean canGetIcyStreamMeta(int channel) {
        return true;
    }

    public static boolean showChannelImage() {
        return false;
    }

    public static int getChannelImage(int channel) {
        return R.drawable.ic_launcher;
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
        return CHANNEL_URL_1;
    }

    public boolean isPodcastMP3() {
        return RadioOneSettings.areRPPodcastsMP3();
    }

    public static boolean areRPPodcastsMP3() {
        return true;
    }

    public ArrayList<ChannelEntry> getChannelList(Resources res) {
        ArrayList<ChannelEntry> channels = new ArrayList<ChannelEntry>();

        for (int i = 1; i < RadioOneSettings.getInstance().getChannelCount() + 1; ++i) {
            if (CupcakeHelper.isCupcake() && !RadioOneSettings.isChannelACC(i)) {
                // Android 1.5 does not support the 181.fm radio stations
                continue;
            }

            if (RadioOneSettings.isChannelActive(i)) {
                channels.add(new ChannelEntry(i, RadioOneSettings.getRPChannelName(res, i)));
            }
        }

        return channels;
    }

    @Override
    public ArrayList<PodcastEntry> getPodcasts() {
        return null;
    }

    @Override
    public boolean isGuest() {
        return false;
    }

    @Override
    public String getServiceAction(ServiceActions action) {
        switch (action) {
            case STOP :
                return "ca.radioone.android.ACTION_STOP";
            case PLAY :
                return "ca.radioone.android.ACTION_PLAY";
            case PAUSE :
                return "ca.radioone.android.ACTION_PAUSE";
            case UPDATE :
                return "ca.radioone.android.ACTION_UPDATE";
            case DOWNLOAD_PODCAST :
                return "ca.radioone.android.ACTION_DOWNLOAD_PODCAST";
            case STATUS_UPDATE :
                return "ca.radioone.android.STATUS_UPDATE";
            case PLAY_PAUSE :
                return "ca.radioone.android.ACTION_PLAY_PAUSE";
            case PREVIOUS :
                return "ca.radioone.android.ACTION_PREVIOUS";
            case NEXT :
                return "ca.radioone.android.ACTION_NEXT";
        }

        return "";
    }

    @Override
    public int getBufferLength(Context context) {
        Resources res = context.getResources();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int bufferLength;
        bufferLength = Integer.parseInt(preferences.getString(RadioOnePreferences.PREF_BUFFER,
                res.getStringArray(R.array.Buffer_entryValues)[0]));
        return bufferLength;
    }
}
