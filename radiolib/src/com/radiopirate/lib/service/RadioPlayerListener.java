package com.radiopirate.lib.service;

public interface RadioPlayerListener {
    PlaybackStatus getStatus();

    // Events
    void onPlayResponse(ServiceMessages.RequestResult result);
    void onPlaybackStarted(boolean notify);
    void onPlaybackStopped(boolean notify);
    boolean onError(boolean acc);
    void sendServiceStatus();
}
