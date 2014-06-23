package com.radiopirate.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.PlaybackService;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceMessages;

/**
 * Sends the requests to the Playback service and receives the response
 * 
 */
public class RPPlayer {
    private static final String TAG = "RadioPirate";

    public static final int SEEK_DELAY = 8000; // 8 Seconds
    public static final int SEEK_DELAY_CAR_MODE = 30000; // 30 Seconds

    private Context context;
    private Messenger messenger = null;
    private RPPlayerHanlder client = null;
    private PlaybackServiceConnection connection = null;
    private ResponseHandler handler = new ResponseHandler();

    private class PlaybackServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, IBinder service) {
            messenger = new Messenger(service);
            sendMessage(ServiceMessages.MESSAGE_GET_SERVICE_STATUS, null);
        }

        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "onServiceDisconnected");
            messenger = null;
        }
    }

    public class ResponseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceMessages.MESSAGE_PLAY_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);

                    if (client != null) {
                        client.onPlayResponse(result);
                    }

                    if (result == ServiceMessages.RequestResult.FILE_NOT_FOUND) {
                        Toast.makeText(context, R.string.toast_play_file_not_found, Toast.LENGTH_SHORT).show();
                    } else if (result == ServiceMessages.RequestResult.NETWORK_UNREACHABLE) {
                        Toast.makeText(context, R.string.toast_play_network_unreachable, Toast.LENGTH_SHORT).show();
                    } else if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_play_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                case ServiceMessages.MESSAGE_STOP_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);

                    if (client != null) {
                        client.onStopResponse(result);
                    }

                    if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_stop_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                case ServiceMessages.MESSAGE_PAUSE_RESUME_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);
                    if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_pause_resume_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                case ServiceMessages.MESSAGE_FORWARD_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);
                    if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_forward_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                case ServiceMessages.MESSAGE_REWIND_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);
                    if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_rewind_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                case ServiceMessages.MESSAGE_SERVICE_STATUS :
                    PlaybackStatus status = (PlaybackStatus) msg.getData().getSerializable(ServiceMessages.KEY_STATUS);
                    if (status != null && client != null) {
                        client.onPlayStatus(status);
                    }
                    break;
                case ServiceMessages.MESSAGE_DOWNLOAD_PROGRESS :
                    DownloadProgress progress = (DownloadProgress) msg.getData().getSerializable(
                            ServiceMessages.KEY_PROGRESS);
                    if (progress != null && client != null) {
                        client.onDownloadProgress(progress);
                    }
                    break;
                case ServiceMessages.MESSAGE_SEEK_RESPONSE : {
                    ServiceMessages.RequestResult result = ServiceMessages.RequestResult.int2e(msg.arg1);
                    if (result != ServiceMessages.RequestResult.SUCCEEDED) {
                        Toast.makeText(context, R.string.toast_seek_failed, Toast.LENGTH_SHORT).show();
                    }
                }
                    break;
                default :
                    Log.e(TAG, "Received unknown response: " + msg.what);
                    break;
            }
        }
    }

    public RPPlayer(Context context, RPPlayerHanlder handler) {
        this.context = context;
        client = handler;
        init();
    }

    public void playChannel(final int channelId) {
        Bundle data = new Bundle();
        data.putInt(ServiceMessages.KEY_CHANNEL_ID, channelId);
        if (!sendMessage(ServiceMessages.MESSAGE_PLAY_CHANNEL, data)) {
            if (client != null) {
                client.onPlayResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_play_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void playURL(final String name, final String url, final boolean isACC, final PodcastSource source) {
        Bundle data = new Bundle();
        data.putString(ServiceMessages.KEY_NAME, name);
        data.putString(ServiceMessages.KEY_URL, url);
        data.putBoolean(ServiceMessages.KEY_IS_ACC, isACC);
        data.putInt(ServiceMessages.KEY_PODCAST_SOURCE, source.ordinal());
        if (!sendMessage(ServiceMessages.MESSAGE_PLAY_URL, data)) {
            if (client != null) {
                client.onPlayResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_play_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void playPodcast(PodcastEntry podcast) {
        Bundle data = new Bundle();
        data.putSerializable(ServiceMessages.KEY_PODCAST, podcast);
        if (!sendMessage(ServiceMessages.MESSAGE_PLAY_PODCAST, data)) {
            if (client != null) {
                client.onPlayResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_play_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 
     * @param podcast
     * @param autoPlay
     *            - If true, automatically play after the download
     */
    public void downloadPodcast(PodcastEntry podcast, boolean autoPlay) {
        Bundle data = new Bundle();
        data.putSerializable(ServiceMessages.KEY_PODCAST, podcast);
        data.putBoolean(ServiceMessages.KEY_AUTO_PLAY, autoPlay);
        if (!sendMessage(ServiceMessages.MESSAGE_DOWNLOAD, data)) {
            if (client != null) {
                client.onPlayResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_play_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void stopPlayback() {
        if (!sendMessage(ServiceMessages.MESSAGE_STOP, null)) {
            if (client != null) {
                client.onStopResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_stop_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void playPause() {
        if (!sendMessage(ServiceMessages.MESSAGE_PAUSE_RESUME, null)) {
            if (client != null) {
                client.onStopResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_pause_resume_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void forward(int delay) {
        Bundle data = new Bundle();
        data.putInt(ServiceMessages.KEY_DELAY, delay);

        if (!sendMessage(ServiceMessages.MESSAGE_FORWARD, data)) {
            if (client != null) {
                client.onStopResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_forward_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void rewind(int delay) {
        Bundle data = new Bundle();
        data.putInt(ServiceMessages.KEY_DELAY, delay);

        if (!sendMessage(ServiceMessages.MESSAGE_REWIND, data)) {
            if (client != null) {
                client.onStopResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_rewind_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void seek(int position) {
        Bundle data = new Bundle();
        data.putInt(ServiceMessages.KEY_DELAY, position);

        if (!sendMessage(ServiceMessages.MESSAGE_SEEK, data)) {
            if (client != null) {
                client.onStopResponse(ServiceMessages.RequestResult.FAILED);
            }
            Toast.makeText(context, R.string.toast_seek_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @param stop
     *            - true to stop playback and service
     */
    public boolean destoy(boolean stop) {

        if (stop) {
            context.unbindService(connection);

            Intent intent = new Intent(context, PlaybackService.class);
            return context.stopService(intent);
        } else {
            sendMessage(ServiceMessages.MESSAGE_UNREGISTER_CLIENT, null);
            context.unbindService(connection);
        }

        connection = null;
        messenger = null;
        client = null;

        return true;
    }

    private void init() {
        Intent intent = new Intent(context, PlaybackService.class);
        context.startService(intent);

        if (connection == null) {
            connection = new PlaybackServiceConnection();
        }
        context.bindService(intent, connection, 0);
    }

    private boolean sendMessage(int message, Bundle data) {

        if (handler == null) {
            Log.e(TAG, "sendMessage handler is null");
            return false;
        }

        // Send message
        try {
            Message msg = Message.obtain(null, message, 0, 0);
            if (data != null) {
                msg.setData(data);
            }
            msg.replyTo = new Messenger(handler);

            if (messenger != null) {
                messenger.send(msg);
            } else {
                Log.e(TAG, "sendMessage messenger is null");
                return false;
            }
        } catch (RemoteException e) {
            Log.e(TAG, "sendMessage - RemoteException", e);
            return false;
        }

        return true;
    }
}
