package ca.radio1.android.fragments;

import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;

public interface OnPlaybackControlsListener {
    void onPlayURL(final String name, final String url, boolean isACC, PodcastSource source);
    void onPlayChannel(int channelId);
    void onPlayPodcast(PodcastEntry podcast);
    void onDownloadPodcast(PodcastEntry podcast, boolean autoPlay);
    void onStopPlayback();
    void onPauseResumePlayback();
    void onForwardPlayback();
    void onRewindPlayback();
    void onSeek(int position);
}
