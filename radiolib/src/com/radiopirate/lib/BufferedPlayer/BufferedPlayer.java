package com.radiopirate.lib.BufferedPlayer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.radiopirate.lib.service.RadioPlayerListener;
import com.radiopirate.lib.service.ServiceMessages;

// TODO: Add more error handling
// Stream player that saves the stream to a file and plays the file
// This allows adding a buffer to reduce the playback pauses
// It allows enabled Shoutcast playback on devices that don't support it as GoogleTV
// Inspired from https://github.com/Dawnthorn/nagare
public class BufferedPlayer implements OnCompletionListener {
    private static final String TAG = "BufferedPlayer";

    private URL m_url = null;
    private DownloadThread m_download_thread = null;
    private MediaPlayer m_media_player = null;
    private int m_current_position = 0;
    private State m_state;
    private RadioPlayerListener listener;
    private Context context;
    private ShoutcastFile m_shoutcast_file = null;
    private Executor executor = Executors.newSingleThreadExecutor();

    public enum State {
        STOPPED, PLAYING, BUFFERING
    }

    private static final int DEFAULT_BUFFER_LENGTH = 10000; // 10 sec

    private int _bufferLength;

    private final Runnable m_run_buffer = new Runnable() {
        public void run() {
            int delay = buffer();
            if (delay > 0) {
                m_handler.postDelayed(this, delay);
            }
        }
    };

    private final Handler m_handler = new Handler();

    public BufferedPlayer(Context context, RadioPlayerListener listener) {
        this.context = context;
        _bufferLength = DEFAULT_BUFFER_LENGTH;
        this.listener = listener;
        m_state = State.STOPPED;

        registerConnectivityReceiver();
    }

    public void destroy() {
        context.unregisterReceiver(connectivityReceiver);
        context = null;
    }

    public ServiceMessages.RequestResult play(String url_string, int bufferLength) {
        if (bufferLength > 0) {
            _bufferLength = bufferLength;
        }

        try {
            m_url = new URL(url_string);
        } catch (MalformedURLException e) {
            Log.e(TAG, "BufferedPlayer.download MalformedURLException", e);
            return ServiceMessages.RequestResult.FILE_NOT_FOUND;
        }

        m_shoutcast_file = new ShoutcastFile();
        m_download_thread = new DownloadThread(m_url, m_shoutcast_file);
        executor.execute(m_download_thread);
        m_current_position = 0;
        m_state = State.BUFFERING;
        if (m_media_player == null) {
            m_media_player = new MediaPlayer();
            m_media_player.setOnCompletionListener(this);

            m_media_player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {

                    if (BufferedPlayer.this.onError()) {
                        return true;
                    }

                    return false;
                }
            });
        }
        m_run_buffer.run();

        return ServiceMessages.RequestResult.SUCCEEDED;
    }

    public State getState() {
        return m_state;
    }

    public void stop() {
        if (m_shoutcast_file != null) {
            m_shoutcast_file.done();
        }
        if (m_media_player != null) {
            if (m_state == State.PLAYING) {
                m_media_player.stop();
            }
        }
        m_state = State.STOPPED;
    }

    private int buffer() {
        if (m_download_thread == null || m_shoutcast_file == null) {
            if (m_state == State.BUFFERING) {
                return 1000;
            } else {
                stop();
                onPlaybackStopped();
                listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
                return 0;
            }
        }

        if (m_shoutcast_file.isDone()) {
            stop();
            onPlaybackStopped();
            listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
            return 0;
        }

        if (m_shoutcast_file.getBufferedMsec() - m_current_position > _bufferLength) {
            try {
                m_media_player.reset();
                m_media_player.setDataSource(m_shoutcast_file.file_path());
                m_media_player.prepare();
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Failed to play buffered file - IllegalArgumentException", e);
                onPlaybackStopped();
                listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
                return 0;
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to play buffered file - SecurityException", e);
                onPlaybackStopped();
                listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
                return 0;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Failed to play buffered file - IllegalStateException", e);
                onPlaybackStopped();
                listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
                return 0;
            } catch (IOException e) {
                Log.e(TAG, "Failed to play buffered file - IOException", e);
                onPlaybackStopped();
                listener.onPlayResponse(ServiceMessages.RequestResult.FAILED);
                return 0;
            }

            m_media_player.seekTo(m_current_position);
            m_media_player.start();
            listener.onPlayResponse(ServiceMessages.RequestResult.SUCCEEDED);
            listener.onPlaybackStarted(true);
            m_state = State.PLAYING;
            return 0;
        } else {
            m_state = State.BUFFERING;
            return 1000;
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.e(TAG, "Reached the end of the buffer");
        // If we did not expect the stop, we will retry
        onError();
    }

    private boolean onError() {
        m_state = State.STOPPED;
        m_shoutcast_file.done();
        return listener.onError(false);
    }

    private void onPlaybackStopped() {
        m_state = State.STOPPED;
        m_shoutcast_file.done();
        listener.onPlaybackStopped(false);
    }

    private void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(connectivityReceiver, filter);
    }

    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().compareTo(ConnectivityManager.CONNECTIVITY_ACTION) != 0) {
                return;
            }

            Bundle data = intent.getExtras();
            boolean noConnectivity = data.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);

            if (m_download_thread != null) {
                Log.e(TAG, "On Connectivity action noConnectivity:" + noConnectivity + " m_state:" + m_state
                        + " downloadFailed:" + m_download_thread.didDownloadFail());
            } else {
                Log.e(TAG, "On Connectivity action noConnectivity:" + noConnectivity + " m_state:" + m_state
                        + " download thread null");
            }
            // If we are now connected, and user requested to play, we might want to re-start playback
            if (noConnectivity == false && m_state != State.STOPPED) {
                if (m_download_thread != null && m_download_thread.didDownloadFail()) {
                    Log.w(TAG, "Restarting download thread on reconnect");
                    executor.execute(m_download_thread);
                }
            }
        }
    };
}
