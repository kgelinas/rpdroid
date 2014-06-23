package com.radiopirate.lib.service;

/**
 * Definition of service messages and responses
 */
public class ServiceMessages {

    // Messages
    public static final int MESSAGE_PLAY_CHANNEL = 1;
    public static final int MESSAGE_PLAY_URL = 2;
    public static final int MESSAGE_PLAY_PODCAST = 3;
    public static final int MESSAGE_STOP = 4;
    public static final int MESSAGE_GET_SERVICE_STATUS = 5;
    public static final int MESSAGE_PAUSE_RESUME = 6;
    public static final int MESSAGE_FORWARD = 7;
    public static final int MESSAGE_REWIND = 8;
    public static final int MESSAGE_UNREGISTER_CLIENT = 9;
    public static final int MESSAGE_DOWNLOAD = 10;
    public static final int MESSAGE_SEEK = 11;

    // Responses
    public static final int MESSAGE_PLAY_RESPONSE = 101;
    public static final int MESSAGE_STOP_RESPONSE = 102;
    public static final int MESSAGE_SERVICE_STATUS = 103;
    public static final int MESSAGE_PAUSE_RESUME_RESPONSE = 104;
    public static final int MESSAGE_FORWARD_RESPONSE = 105;
    public static final int MESSAGE_REWIND_RESPONSE = 106;
    public static final int MESSAGE_DOWNLOAD_PROGRESS = 107;
    public static final int MESSAGE_SEEK_RESPONSE = 108;

    // Keys
    public static final String KEY_CHANNEL_ID = "keyChannelId";
    public static final String KEY_URL = "keyURL";
    public static final String KEY_NAME = "keyName"; // Name of the feed, or podcast
    public static final String KEY_IS_ACC = "keyIsAcc";
    public static final String KEY_STATUS = "keyStatus";
    public static final String KEY_PODCAST = "keyPodcast";
    public static final String KEY_DELAY = "keyDelay";
    public static final String KEY_PROGRESS = "keyProgress";
    public static final String KEY_AUTO_PLAY = "keyAutoPlay";
    public static final String KEY_PODCAST_SOURCE = "keyPodcastSource";

    public enum RequestResult {
        SUCCEEDED, FAILED, FILE_NOT_FOUND, NETWORK_UNREACHABLE;

        public static RequestResult int2e(int i) {
            for (RequestResult current : values()) {
                if (current.ordinal() == i) {
                    return current;
                }
            }
            return FAILED;
        }
    }
}
