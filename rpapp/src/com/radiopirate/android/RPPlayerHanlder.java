package com.radiopirate.android;

import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceMessages;

public interface RPPlayerHanlder {
    void onPlayResponse(ServiceMessages.RequestResult result);
    void onStopResponse(ServiceMessages.RequestResult result);
    void onPlayStatus(PlaybackStatus status);
    void onDownloadProgress(DownloadProgress progress);
}
