package com.radiopirate.lib.model;

import java.io.Serializable;
import java.util.Date;

import com.radiopirate.lib.utils.RPUtil;

/**
 * Data object that represents a podcast
 */
public class PodcastEntry implements Serializable, Comparable<PodcastEntry> {
    private static final long serialVersionUID = 1L;

    private String title;
    private String displayName;
    private String url;

    private String localPath;
    private int currentPosition;
    private int duration;
    private boolean localOnly = false; // True if the podcast is only stored locally
    private boolean isDownloading = false;
    private Date publishDate;
    private PodcastSource source;

    public PodcastEntry(String podcastTitle, String url, String pubDate, PodcastSource source) {
        title = podcastTitle;
        displayName = podcastTitle;
        this.url = url;
        this.publishDate = RPUtil.parseDate(pubDate);
        this.source = source;

        localPath = "";
        currentPosition = 0;
        duration = 0;
    }

    public String getPodcastTitle() {
        return title;
    }

    public String getPodcastDisplayName() {
        return displayName;
    }

    public void setPodcastDisplayName(String value) {
        displayName = value;
    }

    public String getURL() {
        return url;
    }

    public String getLocalPath() {
        return localPath;
    }

    public boolean isDownloaded() {
        return localPath.length() > 0;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getDuration() {
        return duration;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setCachedValue(String localPath, int currentPosition, int duration) {
        this.localPath = localPath;
        this.currentPosition = currentPosition;
        this.duration = duration;
    }

    public String getPubDateStr() {
        return RPUtil.formatDate(publishDate);
    }

    public Date getPubDate() {
        return publishDate;
    }

    public boolean isLocalOnly() {
        return localOnly;
    }

    public void setLocalOnly(boolean value) {
        localOnly = value;
    }

    public void setDownloading(boolean value) {
        isDownloading = value;
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public PodcastSource getPodcastSource() {
        return source;
    }

    @Override
    public int compareTo(PodcastEntry compareObject) {

        return getPubDate().compareTo(compareObject.getPubDate()) * -1;
    }
}
