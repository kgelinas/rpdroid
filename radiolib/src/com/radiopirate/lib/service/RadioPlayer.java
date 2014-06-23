package com.radiopirate.lib.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.radiopirate.lib.service.compatibility.StreamProxy;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.VersionCheck;
import com.spoledge.aacdecoder.AACPlayer;
import com.spoledge.aacdecoder.PlayerCallback;

public class RadioPlayer {
    private static final String TAG = "RadioPlayer";

    private MediaPlayer player = new MediaPlayer();
    private StreamProxy proxy;
    private AACPlayer aacPlayer;
    private boolean isACCPlaying = false;
    private boolean ignoreNextACCStop = false;
    private RadioPlayerListener listener;

    PlayerCallback callback = new PlayerCallback() {

        @Override
        public void playerException(Throwable arg0) {
            Log.e(TAG, "playerException", arg0);
            isACCPlaying = false;

            ServiceMessages.RequestResult result = ServiceMessages.RequestResult.FAILED;
            if (arg0 instanceof FileNotFoundException) {
                result = ServiceMessages.RequestResult.FILE_NOT_FOUND;
            } else if (arg0 instanceof ConnectException) {
                result = ServiceMessages.RequestResult.NETWORK_UNREACHABLE;
            }

            // TODO Can we get information on the error?
            // 401 denied
            // 403 server busy
            // 500 server error
            // Other error => play read error.

            // Called when start fails
            listener.onPlaybackStopped(false);
            listener.onPlayResponse(result);
        }

        @Override
        public void playerPCMFeedBuffer(boolean arg0, int arg1, int arg2) {
        }

        @Override
        public void playerStarted() {
            Log.e(TAG, "playerStarted");

            isACCPlaying = true;
            listener.onPlaybackStarted(true);
            listener.onPlayResponse(ServiceMessages.RequestResult.SUCCEEDED);
        }

        @Override
        public void playerStopped(int arg0) {
            Log.e(TAG, "playerStopped arg0:" + arg0);
            isACCPlaying = false;

            if (ignoreNextACCStop) {
                // We stopped this stream to start another one, so ignore the event
                Log.d(TAG, "Ignoring stopped event");
                ignoreNextACCStop = false;
                return;
            }

            listener.onError(true);
        }

        @Override
        public void playerMetadata(String arg0, String arg1) {
            // TODO Auto-generated method stub

        }

    };

    public RadioPlayer(RadioPlayerListener listener, boolean isGoogleTV) {

        this.listener = listener;

        if (!isGoogleTV) {
            // Disabled on GoogleTV since it does not support the NDK
            aacPlayer = new AACPlayer(callback);
        }
    }

    public ServiceMessages.RequestResult playURL(String url, boolean isACC) {
        if (isACC) {
            aacPlayer.playAsync(url);
            return ServiceMessages.RequestResult.SUCCEEDED;
        } else {
            ServiceMessages.RequestResult result = playMp3Stream(url);

            if (result != ServiceMessages.RequestResult.NETWORK_UNREACHABLE) {
                // If this is a MP3, and play did not work with error NETWORK_UNREACHABLE,
                // then there might not be a retry, so notify now
                listener.onPlayResponse(result);
            }
            return result;
        }
    }

    // For the MediaPlayer only and only supported for podcasts
    public void resume() {
        player.start();
    }

    // For the MediaPlayer only and only supported for podcasts
    public void pause() {
        player.pause();
    }

    // For the MediaPlayer only and only supported for podcasts
    public void forward(int delay) {
        int location = player.getCurrentPosition() + delay;

        if (location > player.getDuration()) {
            return;
        }

        player.seekTo(location);
    }

    // For the MediaPlayer only
    public boolean isPlaying() {
        return player.isPlaying();
    }

    // For the MediaPlayer only
    public int getCurrentPosition() {
        return player.getCurrentPosition();
    }

    // For the MediaPlayer only and only supported for podcasts
    public void rewind(int delay) {
        int location = player.getCurrentPosition() - delay;
        if (location < 0) {
            location = 0;
        }
        player.seekTo(location);
    }

    // For the MediaPlayer only and only supported for podcasts
    public void seek(int duration) {
        player.seekTo(duration);
    }

    // For the MediaPlayer only and only supported for podcasts
    public int getDuration() {
        return player.getDuration();
    }

    // Call this if calling stop then will call start immediately
    public void willStopAndPlay() {
        if (isACCPlaying) {
            // ACC notifies us asynchronously, so ignore stop event since we do not want a retry
            Log.d(TAG, "Ignoring next acc stop");
            ignoreNextACCStop = true;
        }
    }

    public boolean stop() {

        if (player.isPlaying()) {
            player.stop();
        }

        player.reset();

        if (aacPlayer != null) {
            aacPlayer.stop();
        }

        return true;
    }

    private ServiceMessages.RequestResult playMp3Stream(String url) {

        player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.w(TAG, "onError MP3 what:" + what + " extra:" + extra);

                if (listener.onError(false)) {
                    return true;
                }

                return false;
            }
        });

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.w(TAG, "onCompletion MP3");

                if (!listener.getStatus().isLive() && listener.getStatus().getDuration() - getCurrentPosition() < 1000) {
                    // If within 1sec of the end of a podcast, don't restart
                    Log.w(TAG, "onCompletion MP3 - End of stream reached");
                    listener.onPlaybackStopped(true);
                    return;
                }

                // If we did not expect the stop, we will retry
                listener.onError(false);
            }
        });

        player.setOnInfoListener(new MediaPlayer.OnInfoListener() {

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, "onInfo MP3 what:" + what + " extra:" + extra);
                return false;
            }
        });

        player.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (listener.getStatus() == null)
                    return;
                if (listener.getStatus().getDownloadPercent() == percent) // Filter out duplicated notifications
                    return;
                if (percent == 100) // Ignore as 100 can mean stopped buffering because connection was
                                    // lost
                    return;

                if (!listener.getStatus().isPlaying()) {
                    // Not fully started, we might want to seek, but have not done so yet
                    return;
                }

                listener.getStatus().setDownloadPercent(percent);
                listener.sendServiceStatus();
            }
        });

        url = proxyIfNeeded(url, listener.getStatus().isLive());

        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            player.setDataSource(url);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to set data source", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to set data source", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        } catch (IOException e) {
            Log.e(TAG, "Failed to set data source", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        }

        // TODO Can we get information on the error?
        // 401 denied
        // 403 server busy
        // 404 source not found
        // 500 server error
        // autre error => play read error.
        try {
            player.prepare();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to prepare", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to prepare", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        } catch (IOException e) {
            Log.e(TAG, "Failed to prepare", e);

            // Retry if we failed to contact server
            if (!listener.onError(false)) {
                // Not retrying, so notify the client that play failed
                listener.onPlayResponse(ServiceMessages.RequestResult.NETWORK_UNREACHABLE);
            }

            return ServiceMessages.RequestResult.NETWORK_UNREACHABLE;
        }

        try {
            player.start();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Failed to start", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to start", e);
            listener.onPlaybackStopped(false);
            return ServiceMessages.RequestResult.FAILED;
        }

        // We are started, set status before the seek because we could reach the end of stream immediately
        listener.onPlaybackStarted(false);

        // All started now, notify the task
        listener.sendServiceStatus();

        return ServiceMessages.RequestResult.SUCCEEDED;
    }

    /**
     * From: http://stackoverflow.com/questions/4385752/android-shoutcast-want-help-to-play-shoutcast-streaming-in-1-6
     * "Shoutcast streams are natively supported only on 2.2(some device is still using Opencore with Froyo like
     * Motorola Droid 2 Global , Milestone). For earlier versions you must create local proxy that changes the response
     * protocol from ICY (shoutcast) to HTTP, which the mediaplayer will support."
     * 
     * @param url
     * @param isLive
     * @return
     */
    private String proxyIfNeeded(String url, boolean isLive) {
        if ((CupcakeHelper.isCupcake() || VersionCheck.isLessThanGingerbread()) && isLive
                && !listener.getStatus().useProxyOveride()) {

            if (proxy == null) {
                proxy = new StreamProxy();
                proxy.init();
                proxy.start();
            }
            listener.getStatus().setUseProxy(true);
            String proxyUrl = String.format("http://127.0.0.1:%d/%s", proxy.getPort(), url);
            return proxyUrl;
        }

        return url;
    }
}
