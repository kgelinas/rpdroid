package com.radiopirate.lib.service;

import java.io.Serializable;

import com.radiopirate.lib.model.PodcastSource;

public class PlaybackStatus implements Serializable {
    private static final long serialVersionUID = 1L;

    enum RequestedStatus {
        REQUESTED_PLAY, REQUESTED_STOP
    }
    private RequestedStatus requestedStatus = RequestedStatus.REQUESTED_STOP;
    private String url; // URL that is being played
    private String title; // Title of the channel or podcast that is played

    private boolean acc;
    private boolean playing;
    private boolean paused;
    private boolean live;
    private int duration;
    private int progress;
    private int downloadPercent; // How much of the podcast was downloaded
    private int channelId;
    private boolean isLocal;
    private boolean useProxy;
    private boolean useProxyOveride; // If true, don't use the proxy
    private PodcastSource podcastSource;

    // Now Playing info
    private String trackTitle = ""; // Title of the current song

    public PlaybackStatus(String url, String title, boolean isACC, boolean live, int channelId, boolean isLocal,
            PodcastSource podcastSource) {
        this.url = url;
        this.title = title;
        acc = isACC;
        playing = false;
        paused = false;
        this.live = live;
        this.channelId = channelId;
        duration = 0;
        progress = 0;
        downloadPercent = 0;
        this.isLocal = isLocal;
        useProxy = false;
        useProxyOveride = false;
        this.podcastSource = podcastSource;
    }

    public void setRequestedStatus(RequestedStatus requestedStatus) {
        this.requestedStatus = requestedStatus;
    }

    public RequestedStatus getRequestedStatus() {
        return requestedStatus;
    }

    public String getURL() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String value) {
        title = value;
    }

    public String getTrackTitle() {
        return trackTitle;
    }

    public void clearSongInfo() {
        setTrackTitle("");
    }

    public void setTrackTitle(String title) {
        trackTitle = title;
    }

    public boolean isACC() {
        return acc;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isLive() {
        return live;
    }

    public PodcastSource getPodcastSource() {
        return podcastSource;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public void setDuration(int value) {
        this.duration = value;
    }

    public void setProgress(int value) {
        this.progress = value;
    }

    public int getDuration() {
        return duration;
    }

    public int getProgress() {
        return progress;
    }

    public void setDownloadPercent(int value) {
        this.downloadPercent = value;
    }

    public int getDownloadPercent() {
        return downloadPercent;
    }

    public void setChannelId(int value) {
        this.channelId = value;
    }

    public int getChannelId() {
        return channelId;
    }

    /**
     * Return true of the stream supports pause/resume/forward/rewind
     */
    public boolean areAdvancedControlSupported() {
        // TODO: More test would be needed to support there for live MP3 streams, it breaks if we seek to far in the
        // future. Need to see what happens of we seek far back or pause for a long time

        // Disable seeking for streamed podcast, the server send a "downloaded too much" response if we access it too
        // often
        return !acc && !live && isLocal;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public boolean useProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean value) {
        useProxy = value;
    }

    public boolean useProxyOveride() {
        return useProxyOveride;
    }

    public void setUseProxyOveride(boolean value) {
        useProxyOveride = value;
    }
}
