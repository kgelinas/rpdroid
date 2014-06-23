package com.radiopirate.android;

import android.content.res.Resources;

/*
 * This class adds extra music channels on top of the public ones coming from the API.
 */
public class ExtraChannels {

    private static final String CHANNEL_URL_1 = "http://scfire-ntc-aa03.stream.aol.com:80/stream/1022"; // "http://yp.shoutcast.com/sbin/tunein-station.pls?id=37586
    private static final String CHANNEL_URL_2 = "http://173.192.16.203:8004";
    private static final String CHANNEL_URL_3 = "http://uplink.181.fm:8064";
    private static final String CHANNEL_URL_4 = "http://uplink.181.fm:8028";
    private static final String CHANNEL_URL_5 = "http://uplink.181.fm:8030";
    private static final String CHANNEL_URL_6 = "http://uplink.181.fm:8052";

    public static String getRPChannelName(Resources res, int channelId) {
        switch (channelId) {
            case 1 :
                return res.getString(R.string.channel_name_1);
            case 2 :
                return res.getString(R.string.channel_name_2);
            case 3 :
                return res.getString(R.string.channel_name_3);
            case 4 :
                return res.getString(R.string.channel_name_4);
            case 5 :
                return res.getString(R.string.channel_name_5);
            case 6 :
                return res.getString(R.string.channel_name_6);
            default :
                return "";
        }
    }

    public static String getDescription(Resources res, int channelId) {
        switch (channelId) {
            case 1 :
                return res.getString(R.string.channel_desc_1);
            case 2 :
                return res.getString(R.string.channel_desc_2);
            case 3 :
                return res.getString(R.string.channel_desc_3);
            case 4 :
                return res.getString(R.string.channel_desc_4);
            case 5 :
                return res.getString(R.string.channel_desc_5);
            case 6 :
                return res.getString(R.string.channel_desc_6);
            default :
                return "";
        }
    }

    /**
     * @param channel
     * @return URL of the channel including all needed parameters
     */
    public static String getChannelURL(int channel) {
        String strFormat = "";
        switch (channel) {
            case 1 :
                strFormat = CHANNEL_URL_1;
                break;
            case 2 :
                strFormat = CHANNEL_URL_2;
                break;
            case 3 :
                strFormat = CHANNEL_URL_3;
                break;
            case 4 :
                strFormat = CHANNEL_URL_4;
                break;
            case 5 :
                strFormat = CHANNEL_URL_5;
                break;
            case 6 :
                strFormat = CHANNEL_URL_6;
                break;
        }

        return strFormat;
    }

    public static boolean isACC(int channel) {
        return false;
    }

    public static int getRadioCount() {
        return 6;
    }
}
