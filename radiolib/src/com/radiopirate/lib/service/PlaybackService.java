package com.radiopirate.lib.service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;
import com.radiopirate.lib.BufferedPlayer.BufferedPlayer;
import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.delegates.ResourceDelegate;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.DownloadManager.DownloadManagerHandler;
import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.PlaybackStatus.RequestedStatus;
import com.radiopirate.lib.service.ServiceMessages.RequestResult;
import com.radiopirate.lib.utils.AudioFocusWrapper;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.EclairHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.RPUtil;
import com.radiopirate.lib.utils.RemoteControlClientWrapper;
import com.radiopirate.lib.utils.VersionCheck;

/**
 * The service is allowed to create threads that run in the background Threads created by activities can be killed, but
 * these will keep running unless memory really becomes low, so playback is less likely to stop while multi-tasking
 */
public class PlaybackService extends Service implements DownloadManagerHandler, RadioPlayerListener {

    private static final String TAG = "RP PlaybackService";
    private static final int MAX_RETRY = 2;
    private static final int MIN_RETRY_DELAY = 2000; // If retry count is 0, and we retried less than 2 sec ago, don't
                                                     // retry
    private static final String ILLEGAL_CHARACTERS = ":";
    private static final String ILLEGAL_FILLERS = "_";
    private static final int UPDATE_TITLE_DELAY = 5000; // Delay in ms at which to update the title
    private static final int NBR_PODCAST_DOWNLOAD = 4; // Download the 4 podcasts of the day

    private RadioPlayer radioPlayer;
    private BufferedPlayer bufferedPlayer;

    // Members
    private PlaybackNotification notification;
    private Executor executor = Executors.newSingleThreadExecutor();
    private Executor executorSongTitle = Executors.newSingleThreadExecutor(); // Run this on its own thread in case IO
                                                                              // takes a long time
    private Executor executorDownloader = Executors.newSingleThreadExecutor();

    private Handler handler = new AppMsgHandler();
    private Messenger messenger = new Messenger(handler);
    private Messenger clientMessenger = null;
    private PlaybackStatus status = null;
    private int retryCount = 0;
    private long lastRetryTime = 0;
    private AudioManager audioManager;
    private RemoteControlClientWrapper remoteControlClient;
    private AudioFocusWrapper audioFocus;

    private static Wifilock wifiLock;

    private Object retryLock = new Object();

    /**
     * Handle that handles messages from the UI task
     */
    private class AppMsgHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            if (msg.replyTo != null) {
                clientMessenger = msg.replyTo;
            }

            switch (msg.what) {
                case ServiceMessages.MESSAGE_PLAY_CHANNEL :
                    int channelId = msg.getData().getInt(ServiceMessages.KEY_CHANNEL_ID);

                    playUrl(msg.replyTo, RadioSettingsDelegate.getInstance().getChannelName(getResources(), channelId),
                            "", RadioSettingsDelegate.getInstance().isACC(channelId), true, channelId,
                            PodcastSource.Unknown);
                    break;
                case ServiceMessages.MESSAGE_PLAY_URL :
                    String name = msg.getData().getString(ServiceMessages.KEY_NAME);
                    String url = msg.getData().getString(ServiceMessages.KEY_URL);
                    boolean acc = msg.getData().getBoolean(ServiceMessages.KEY_IS_ACC, false);
                    PodcastSource source = PodcastSource.int2e(msg.getData().getInt(ServiceMessages.KEY_PODCAST_SOURCE,
                            PodcastSource.RP.ordinal()));
                    playUrl(msg.replyTo, name, url, acc, false, -1, source);
                    break;
                case ServiceMessages.MESSAGE_PLAY_PODCAST : {
                    PodcastEntry podcast = (PodcastEntry) msg.getData().getSerializable(ServiceMessages.KEY_PODCAST);
                    playPodcast(msg.replyTo, podcast);
                }
                    break;
                case ServiceMessages.MESSAGE_STOP :
                    stopPlayback(msg.replyTo);
                    break;
                case ServiceMessages.MESSAGE_GET_SERVICE_STATUS :
                    sendServiceStatus(msg.replyTo);
                    break;
                case ServiceMessages.MESSAGE_PAUSE_RESUME :
                    pauseResume(msg.replyTo);
                    break;
                case ServiceMessages.MESSAGE_FORWARD : {
                    int delay = msg.getData().getInt(ServiceMessages.KEY_DELAY);
                    forward(msg.replyTo, delay);
                }
                    break;
                case ServiceMessages.MESSAGE_REWIND : {
                    int delay = msg.getData().getInt(ServiceMessages.KEY_DELAY);
                    rewind(msg.replyTo, delay);
                }
                    break;
                case ServiceMessages.MESSAGE_SEEK : {
                    int position = msg.getData().getInt(ServiceMessages.KEY_DELAY);
                    seek(msg.replyTo, position);
                }
                    break;
                case ServiceMessages.MESSAGE_UNREGISTER_CLIENT :
                    clientMessenger = null;
                    break;
                case ServiceMessages.MESSAGE_DOWNLOAD : {
                    PodcastEntry podcast = (PodcastEntry) msg.getData().getSerializable(ServiceMessages.KEY_PODCAST);
                    boolean autoPlay = msg.getData().getBoolean(ServiceMessages.KEY_AUTO_PLAY);
                    onDownloadPodcast(podcast, autoPlay, false);
                }
                    break;
                default :
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    private void onDownloadPodcast(PodcastEntry podcast, boolean autoPlay, boolean autoDownloaded) {
        String filename;

        if (CupcakeHelper.isCupcake() || VersionCheck.isLessThanFroyo()) {
            File podcstFolder = new File(Environment.getExternalStorageDirectory() + "/Podcasts/");
            podcstFolder.mkdirs();

            filename = podcstFolder + "/"
                    + podcast.getPodcastDisplayName().replaceAll(ILLEGAL_CHARACTERS, ILLEGAL_FILLERS) + ".mp3";
        } else {
            filename = FroyoHelper.getPodcastFolder(PlaybackService.this,
                    podcast.getPodcastDisplayName().replaceAll(ILLEGAL_CHARACTERS, ILLEGAL_FILLERS));
        }

        new DownloadManager(PlaybackService.this, podcast.getPodcastTitle(), podcast.getPubDateStr(),
                PlaybackService.this, autoPlay, autoDownloaded, podcast.getPodcastSource()).execute(podcast.getURL(),
                filename);
    }

    private void updatePodcast() {

        boolean usingWifi = isOnWifi();
        wifiLock.lock(usingWifi);
        executorDownloader.execute(new Runnable() {
            public void run() {
                ArrayList<PodcastEntry> array = RadioSettingsDelegate.getInstance().getPodcasts();

                if (array.size() > 0) {
                    // TODO: Should we download the 4 files in a row?
                    for (int i = Math.min(array.size() - 1, NBR_PODCAST_DOWNLOAD - 1); i >= 0; i--) {

                        final PodcastEntry entry = array.get(i);
                        PodcastOpenHelper database = new PodcastOpenHelper(PlaybackService.this);
                        String localPath = database.getLocalPath(entry.getPodcastTitle());
                        if (localPath.length() == 0) {
                            // The newest file is not downloaded yet
                            Log.d(TAG, "Download latest file");

                            // Need to post to main thread since we cannot call an AsyncTask from a worker thread
                            // reliably
                            handler.post(new Runnable() {
                                public void run() {
                                    onDownloadPodcast(entry, false, true);
                                }
                            });

                            return; // We will release the lock once the download completes
                        }
                    }
                }

                if (status == null || !status.isPlaying()) {
                    wifiLock.release();
                }
            }
        });
    };

    /**
     * Called when the service is created
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "PlaybackService.onCreate");
        super.onCreate();

        BugSenseHandler.setup(this, RadioSettingsDelegate.getInstance().getBugSenseAPIkey());

        radioPlayer = new RadioPlayer(this, RPUtil.doesSupportGoogleTV(this));
        bufferedPlayer = new BufferedPlayer(this, this);

        notification = new PlaybackNotification(this);
        wifiLock = new Wifilock(getBaseContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater()) {
            remoteControlClient = new RemoteControlClientWrapper(this);
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            audioFocus = new AudioFocusWrapper();
        }

        if (AutoDownloader.isAutoPodcastDownload(this)) {
            AutoDownloader.startPodcastDownloadTimer(this);
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isGreaterThanEclair()) {
            // Let the OS know we will play music and the user will notice if it kills us
            EclairHelper.startForeground(this, PlaybackNotification.PLAY_NOTIFICATION_ID,
                    notification.createNotification("", "", ""));
        }

        registerHeadsetPlugReceiver();
        registerConnectivityReceiver();
        registerCommandReceiver();
        registerPhoneStateListener();
        registerRemoteControlClient();

        loadPlaybackStatus();
    }

    /**
     * Called when a task connects to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(TAG, "onBind");
        return messenger.getBinder();
    }

    /**
     * Called when the service is about to be terminated
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "PlaybackService.onDestroy");
        super.onDestroy();

        updateProgress(); // TODO Save periodically while playing podcast so we don't loose Podcast progress

        if (radioPlayer.isPlaying()) {
            if (!status.isLive()) {
                sendServiceStatus(clientMessenger);
            }
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            audioFocus.abandonAudioFocus(audioManager);
        }

        stop();
        notification.clearNotification();
        unregisterReceiver(headsetPlugReceiver);
        unregisterReceiver(connectivityReceiver);
        unregisterReceiver(commandReceiver);
        unregisterPhoneStateListener();
        unregisterRemoteControlClient();
        handler.removeCallbacks(updateStreamTitleTask);

        clientMessenger = null;
        if (status != null) {
            status.setPlaying(false);
        }
    }

    @Override
    public void onDownloadProgress(DownloadProgress progress) {
        if (clientMessenger == null) {
            Log.e(TAG, "onDownloadProgress - Messenger is NULL");
            return;
        }

        try {
            Bundle data = new Bundle();
            data.putSerializable(ServiceMessages.KEY_PROGRESS, progress);
            Message message = Message.obtain(null, ServiceMessages.MESSAGE_DOWNLOAD_PROGRESS, 0, 0);
            message.setData(data);
            clientMessenger.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "onDownloadProgress - RemoteException", e);
        }
    }

    @Override
    public void onDownloadCompleted(final DownloadProgress progress, final boolean autoPlay, boolean autoDownloaded) {
        Log.e(TAG, "onDownloadCompleted");
        onDownloadProgress(progress);
        if (autoPlay) {
            executor.execute(new Runnable() {

                @Override
                public void run() {
                    Log.d(TAG, "Auto start on download completed");
                    status = new PlaybackStatus(progress.getLocalPath(), progress.getTitle(), !RadioSettingsDelegate
                            .getInstance().isPodcastMP3(), false, -1, true, progress.getPodcastSource());
                    playURL(progress.getLocalPath(), !RadioSettingsDelegate.getInstance().isPodcastMP3());
                }
            });
        }

        if (autoDownloaded) {
            if (status == null || !status.isPlaying()) {
                wifiLock.release();
            }
        }
    }

    private void loadPlaybackStatus() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        String url = preferences.getString("PlaybackStatus_url", "");
        if (url.length() > 0) {
            // If we saved a url, load the PlaybackStatus
            String title = preferences.getString("PlaybackStatus_title", "");
            boolean acc = preferences.getBoolean("PlaybackStatus_acc", false);
            boolean live = preferences.getBoolean("PlaybackStatus_live", false);
            int duration = preferences.getInt("PlaybackStatus_duration", 0);
            int progress = preferences.getInt("PlaybackStatus_progress", 0);
            int channelId = preferences.getInt("PlaybackStatus_channelId", -1);
            boolean isLocal = preferences.getBoolean("PlaybackStatus_isLocal", false);
            int podcastSource = preferences.getInt("PlaybackStatus_podcastSource", PodcastSource.RP.ordinal());

            status = new PlaybackStatus(url, title, acc, live, channelId, isLocal, PodcastSource.int2e(podcastSource));
            status.setDuration(duration);
            status.setProgress(progress);
        }
    }

    private void savePlaybackStatus() {
        if (status != null && status.getURL().length() > 0) {
            Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();

            editor.putString("PlaybackStatus_url", status.getURL());
            editor.putString("PlaybackStatus_title", status.getTitle());
            editor.putBoolean("PlaybackStatus_acc", status.isACC());
            editor.putBoolean("PlaybackStatus_live", status.isLive());
            editor.putInt("PlaybackStatus_duration", status.getDuration());
            editor.putInt("PlaybackStatus_progress", status.getProgress());
            editor.putInt("PlaybackStatus_channelId", status.getChannelId());
            editor.putBoolean("PlaybackStatus_isLocal", status.isLocal());
            editor.putInt("PlaybackStatus_podcastSource", status.getPodcastSource().ordinal());

            editor.commit();
        }
    }

    private void playUrl(final Messenger msg, final String name, final String urlIn, final boolean acc,
            final boolean live, final int channelId, final PodcastSource source) {
        playUrl(msg, name, urlIn, acc, live, channelId, false, source);
    }

    private void playUrl(final Messenger msg, final String name, final String urlIn, final boolean acc,
            final boolean live, final int channelId, final boolean isLocal, final PodcastSource source) {
        executor.execute(new Runnable() {
            public void run() {
                synchronized (retryLock) {
                    retryCount = 0;
                }

                updateProgress(); // Update progress of current Podcast if any

                String url = urlIn;

                if (channelId >= 0) {
                    url = RadioSettingsDelegate.getInstance().getChannelURL(channelId);
                }

                status = new PlaybackStatus(url, name, acc, live, channelId, isLocal, source);

                playURL(url, acc);
                savePlaybackStatus(); // Save now in case we don't stop normally
            }
        });
    }

    /**
     * If we downloaded the podcast and the file exists, play the local file
     * 
     * @param msg
     * @param podcast
     */
    private void playPodcast(final Messenger msg, final PodcastEntry podcast) {
        executor.execute(new Runnable() {
            public void run() {
                PodcastOpenHelper database = new PodcastOpenHelper(PlaybackService.this);
                String strLocalPath = database.getLocalPath(podcast.getPodcastTitle());
                String url = podcast.getURL();
                boolean isLocal = false;

                if (strLocalPath.length() > 0) {
                    if ((new File(strLocalPath)).exists()) {
                        url = strLocalPath;
                        isLocal = true;
                    }
                }

                playUrl(msg, podcast.getPodcastTitle(), url, !RadioSettingsDelegate.getInstance().isPodcastMP3(),
                        false, -1, isLocal, podcast.getPodcastSource());
            }
        });
    }

    private void stopPlayback(final Messenger msg) {
        executor.execute(new Runnable() {
            public void run() {
                if (radioPlayer.isPlaying()) {
                    if (!status.isLive()) {
                        sendServiceStatus(clientMessenger);
                    }
                }

                boolean sendStatus = false;
                if (status != null && status.isPaused()) {
                    sendStatus = true;
                }
                stop();

                if (sendStatus) {
                    // If we stopped a paused stream, send notification as none would be sent
                    onPlaybackStopped(true);
                }
            }
        });
    }

    private void pauseResume(final Messenger msg) {
        executor.execute(new Runnable() {
            public void run() {
                pauseResume();
            }
        });
    }

    private void forward(final Messenger msg, final int delay) {
        executor.execute(new Runnable() {
            public void run() {
                forward(delay);
            }
        });
    }

    private void rewind(final Messenger msg, final int delay) {
        executor.execute(new Runnable() {
            public void run() {
                rewind(delay);
            }
        });
    }

    private void seek(final Messenger msg, final int duration) {
        executor.execute(new Runnable() {
            public void run() {
                seek(duration);
            }
        });
    }

    private void sendResponse(final Messenger msg, int responseId, RequestResult result) {
        if (msg == null) {
            return;
        }

        try {
            Message message = Message.obtain(null, responseId, result.ordinal(), 0);
            msg.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendResponse - RemoteException", e);
        }
    }

    private void updateProgress() {
        if (status != null && !status.isACC() && status.isPlaying()) {
            int progress = radioPlayer.getCurrentPosition();
            if (progress == 0) // Can receive these while doing the initial seek, so ignore
                return;

            status.setProgress(progress);

            savePlaybackStatus(); // Save new progress in case we stop unexpectedly

            if (!status.isLive() && status.getProgress() != 0) {
                PodcastOpenHelper database = new PodcastOpenHelper(this);
                database.updateCurrentPosition(status.getTitle(), status.getProgress(), status.getDuration(),
                        status.getPodcastSource());
            }
        }
    }

    @Override
    public void sendServiceStatus() {
        sendServiceStatus(clientMessenger);
    }

    /**
     * synchronized to ensure we maintain the order even when called from 2 threads
     */
    synchronized private void sendServiceStatus(final Messenger msg) {

        updateProgress();

        Bundle data = new Bundle();
        data.putSerializable(ServiceMessages.KEY_STATUS, status);

        // Send broadcast event for the widget
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.STATUS_UPDATE));
        broadcastIntent.putExtras(data);
        sendBroadcast(broadcastIntent);

        if (msg == null) {
            return;
        }

        // Notify the application
        try {
            Message message = Message.obtain(null, ServiceMessages.MESSAGE_SERVICE_STATUS, 0, 0);
            message.setData(data);
            msg.send(message);
        } catch (RemoteException e) {
            Log.e(TAG, "sendResponse - RemoteException", e);
        }
    }

    private boolean stop() {
        if (status != null) {
            status.setRequestedStatus(RequestedStatus.REQUESTED_STOP);
        }

        onPlaybackStopped(true);

        radioPlayer.stop();
        bufferedPlayer.stop();

        return true;
    }

    private void pauseResume() {
        Log.d(TAG, "pause resume");
        if (status != null && !status.isACC() && status.isPlaying()) {
            if (status.isPaused()) {
                resume();
            } else {
                pause();
            }

            sendResponse(clientMessenger, ServiceMessages.MESSAGE_PAUSE_RESUME_RESPONSE,
                    ServiceMessages.RequestResult.SUCCEEDED);
        } else {
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_PAUSE_RESUME_RESPONSE,
                    ServiceMessages.RequestResult.FAILED);
        }

        sendServiceStatus(clientMessenger);
    }

    private void resume() {
        radioPlayer.resume();
        status.setPaused(false);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setIsPlaying(this);
        }

        // Acquire the lock on resume since we release it on pause
        boolean usingWifi = isOnWifi();
        Log.d(TAG, "Playback resumed usingWifi:" + usingWifi);
        wifiLock.lock(usingWifi && !status.isLocal());

        sendServiceStatus(clientMessenger);
    }

    private void pause() {
        radioPlayer.pause();
        status.setPaused(true);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setIsPaused();
        }

        // Release the lock when pause to minimize battery usage if playback is paused for a long time
        wifiLock.release();

        sendServiceStatus(clientMessenger);
    }

    private void forward(int delay) {
        if (status != null && !status.isACC() && status.isPlaying()) {
            radioPlayer.forward(delay);
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_FORWARD_RESPONSE,
                    ServiceMessages.RequestResult.SUCCEEDED);
        } else {
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_FORWARD_RESPONSE,
                    ServiceMessages.RequestResult.FAILED);
        }

        sendServiceStatus(clientMessenger);
    }

    private void rewind(int delay) {
        if (status != null && !status.isACC() && status.isPlaying()) {
            radioPlayer.rewind(delay);
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_REWIND_RESPONSE,
                    ServiceMessages.RequestResult.SUCCEEDED);
        } else {
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_REWIND_RESPONSE, ServiceMessages.RequestResult.FAILED);
        }

        sendServiceStatus(clientMessenger);
    }

    private void seek(int duration) {
        if (status != null && !status.isACC() && status.isPlaying()) {
            radioPlayer.seek(duration);
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_SEEK_RESPONSE,
                    ServiceMessages.RequestResult.SUCCEEDED);
        } else {
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_SEEK_RESPONSE, ServiceMessages.RequestResult.FAILED);
        }

        sendServiceStatus(clientMessenger);
    }

    private ServiceMessages.RequestResult playURL(String url, boolean isACC) {
        Log.d(TAG, "PlayURL");

        // Ignore this stop event
        status.setRequestedStatus(RequestedStatus.REQUESTED_STOP);
        radioPlayer.willStopAndPlay();

        stop(); // Stop any previous play

        Resources res = getResources();
        String playUrl = url;

        if (status.getChannelId() == 1) {
            // Channel 1 supports network specific quality
            boolean usingWifi = isOnWifi();
            playUrl = RadioSettingsDelegate.getInstance().getChannelURL(status.getChannelId(), usingWifi);
            status.setTitle(RadioSettingsDelegate.getInstance().getChannelName(res, status.getChannelId(), usingWifi));
        }

        String buffering = res.getString(ResourceDelegate.getInstance().getStringNotificationBuffering());
        notification.displayPlaybackStatus(status.getTitle(), buffering);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setMetadata(status.getTitle(), buffering, "");
        }

        status.setTrackTitle(buffering);
        sendServiceStatus(clientMessenger);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setIsBuffering();
        }

        if (!status.isLocal()) {
            playUrl = playUrl;
        }

        Log.d(TAG, "PlayURL url:" + playUrl + " ACC:" + isACC);
        status.setRequestedStatus(RequestedStatus.REQUESTED_PLAY);

        int bufferLength = RadioSettingsDelegate.getInstance().getBufferLength(this);
        if ((bufferLength > 0 || RPUtil.doesSupportGoogleTV(this) || CupcakeHelper.isCupcake()) && status.isLive()
                && !isACC) {
            // GoogleTV and CupCake do not support Shoutcast streaming, so always us a buffer as a workarround
            return bufferedPlayer.play(playUrl, bufferLength * 1000);
        } else {
            return radioPlayer.playURL(playUrl, isACC);
        }
    }

    /**
     * 
     * @param acc
     *            True if the stop/error was received by ACC player, false if MP3
     * @return
     */
    @Override
    public boolean onError(boolean acc) {
        boolean retry = false;

        Log.e(TAG, "onError RequestedStatus:" + status.getRequestedStatus() + " retryCount:" + retryCount);
        updateProgress(); // Update progress of current Podcast if any

        synchronized (retryLock) {
            if (status.getRequestedStatus() == RequestedStatus.REQUESTED_PLAY && acc == status.isACC()) {

                if (retryCount > 0 && status.useProxy()) {
                    // If we are using the proxy, and we already retried, try next time without the proxy
                    // This is needed on 2.2 as the proxy does not work on all phones
                    Log.w(TAG, "Overriding use of the proxy.");
                    status.setUseProxyOveride(true);
                    status.setUseProxy(false);
                    retry = true;
                } else if (retryCount < MAX_RETRY) {
                    // If retry count is 0 and we retry recently don't retry to prevent retry loops
                    if (!(retryCount == 0 && lastRetryTime > System.currentTimeMillis() - MIN_RETRY_DELAY)) {
                        ++retryCount;
                        lastRetryTime = System.currentTimeMillis();
                        retry = true;
                    }
                }
            }
        }

        if (retry) {
            Log.w(TAG, "Will retry to play...");
            executor.execute(new Runnable() {
                public void run() {
                    Log.w(TAG, "Retrying to play...");
                    ServiceMessages.RequestResult result = playURL(status.getURL(), status.isACC());

                    if (!status.isACC() && result != ServiceMessages.RequestResult.SUCCEEDED) {
                        onPlaybackStopped(true);
                    }
                }
            });
        } else {
            Log.w(TAG, "onError - onPlaybackStopped");
            onPlaybackStopped(true);
        }

        return retry;
    }

    private boolean isOnWifi() {
        boolean usingWifi = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo network = connectivityManager.getActiveNetworkInfo();

        if (network != null) {
            usingWifi = network.getType() == ConnectivityManager.TYPE_WIFI;
        }

        return usingWifi;
    }

    @Override
    public void onPlayResponse(ServiceMessages.RequestResult result) {
        sendResponse(clientMessenger, ServiceMessages.MESSAGE_PLAY_RESPONSE, result);
    }

    @Override
    public void onPlaybackStarted(boolean notify) {
        synchronized (retryLock) {
            retryCount = 0;
        }

        boolean usingWifi = isOnWifi();
        Log.d(TAG, "Playback started usingWifi:" + usingWifi);
        wifiLock.lock(usingWifi && !status.isLocal());

        notification.displayPlaybackStatus(status.getTitle(), "", ""); // No ticker, we displayed it while buffering

        status.setTrackTitle("");
        status.setPlaying(true);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            audioFocus.requestAudioFocus(audioManager);
        }

        registerRemoteControlClient();

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setIsPlaying(this);
        }

        if (notify) {
            sendServiceStatus(clientMessenger);
        }

        if (RadioSettingsDelegate.getInstance().canGetIcyStreamMeta(status.getChannelId()) && status.isLive()) {
            try {
                URL metaURL = new URL(status.getURL());
                meta = new IcyStreamMeta(metaURL);
                handler.post(updateStreamTitleTask);
            } catch (MalformedURLException e) {
                Log.e(TAG, "onPlaybackStarted - MalformedURLException", e);
            }
        } else {
            status.setTrackTitle("");
            handler.removeCallbacks(updateStreamTitleTask);
        }

        if (!status.isLive()) {

            int duration = radioPlayer.getDuration();
            status.setDuration(duration);

            if (status.getProgress() == 0) {
                // If progress not set because we were just playing this podcast, load it from the dB
                PodcastOpenHelper database = new PodcastOpenHelper(this);
                status.setProgress(database.getCurrentPosition(status.getTitle()));
            }

            if (status.getProgress() > 0) {
                // We were playing this podcast before, start from where we were

                if (duration - status.getProgress() > 1000) {
                    // Only seek if there is at least 1 sec left to the MP3
                    radioPlayer.seek(status.getProgress());
                } else {
                    Log.d(TAG, "Don't seek and start from beginning, the MP3 was already fully played.");
                    status.setProgress(0);
                }
            } else {
                int progress = radioPlayer.getCurrentPosition();
                status.setProgress(progress);
            }
        }
    }

    @Override
    public PlaybackStatus getStatus() {
        return status;
    }

    @Override
    public void onPlaybackStopped(boolean notify) {
        updateProgress(); // Update progress of current Podcast if any

        wifiLock.release();
        notification.displayPlaybackStatus("", "");

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setMetadata(status.getTitle(), "", "");
        }

        handler.removeCallbacks(updateStreamTitleTask);

        if (status != null) {
            status.setPlaying(false);
            status.clearSongInfo();
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.setIsStopped();
        }

        if (notify) {
            sendResponse(clientMessenger, ServiceMessages.MESSAGE_STOP_RESPONSE,
                    ServiceMessages.RequestResult.SUCCEEDED);
        }
        sendServiceStatus(clientMessenger);
    }

    private BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {

        /**
         * Called when a wired headset is plugged or unplugged while the service is running. This action cannot be
         * received while there is no process running.
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG, "HeadsetPlugReceiver.onReceive - action:" + intent.getAction() + " state:"
                    + intent.getExtras().getInt("state"));

            final int state = intent.getExtras().getInt("state");
            executor.execute(new Runnable() {

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlaybackService.this);
                final boolean autoStart = preferences.getBoolean("prefAutoStart", false);
                final boolean autoStop = preferences.getBoolean("prefAutoStop", false);

                public void run() {
                    if (state == 0) {
                        if (autoStop) {
                            Log.d(TAG, "Auto stop on headset disconnected");
                            if (radioPlayer.isPlaying()) {
                                if (!status.isLive()) {
                                    sendServiceStatus(clientMessenger);
                                }
                            }
                            stop();
                        }
                    } else {
                        if (autoStart && status != null && status.getURL() != null && status.getURL().length() > 0
                                && !status.isPlaying()) {
                            TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                            if (telephony.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                                Log.d(TAG, "Auto start on headset connected");
                                playURL(status.getURL(), status.isACC());
                            }
                        }
                    }
                }
            });
        }
    };

    private void registerHeadsetPlugReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(headsetPlugReceiver, filter);
    }

    private BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().compareTo(ConnectivityManager.CONNECTIVITY_ACTION) != 0) {
                return;
            }

            Bundle data = intent.getExtras();
            boolean noConnectivity = data.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);

            // Also available
            // String info = data.getString(ConnectivityManager.EXTRA_EXTRA_INFO);
            // String reason = data.getString(ConnectivityManager.EXTRA_EXTRA_INFO);
            // boolean isFailover = data.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
            // NetworkInfo networkInfo = data.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
            // NetworkInfo otherNetworkInfo = data.getParcelable(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);

            // If we are now connected, and user requested to play, we might want to re-start playback
            if (noConnectivity == false && status != null
                    && status.getRequestedStatus() == RequestedStatus.REQUESTED_PLAY) {
                executor.execute(new Runnable() {
                    public void run() {
                        if (status != null) {
                            if (status.getRequestedStatus() == RequestedStatus.REQUESTED_PLAY && !status.isPlaying()) {
                                TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                                if (telephony.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                                    Log.w(TAG, "Retrying to play after connection re-established...");
                                    playURL(status.getURL(), status.isACC());
                                }
                            }
                        }
                    }
                });
            }
        }
    };

    private void registerConnectivityReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(connectivityReceiver, filter);
    }

    private BroadcastReceiver commandReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.DOWNLOAD_PODCAST))) {
                // Need to run this one synchronously else the device might go back to sleep
                if (AutoDownloader.isAutoPodcastDownload(PlaybackService.this)) {
                    updatePodcast();
                }
            }

            executor.execute(new Runnable() {
                public void run() {
                    if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.STOP))) {
                        stop();
                    } else if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PLAY))) {
                        if (status != null && status.getURL() != null && status.getURL().length() > 0) {
                            if (status.isPaused()) {
                                resume();
                            } else if (!status.isPlaying()) {
                                playURL(status.getURL(), status.isACC());
                            }
                        } else {
                            // Play the default channel
                            playChannel(1);
                        }
                    } else if (action
                            .equals(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PAUSE))) {
                        pause();
                    } else if (action.equals(RadioSettingsDelegate.getInstance()
                            .getServiceAction(ServiceActions.UPDATE))) {
                        sendServiceStatus(clientMessenger);
                    } else if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(
                            ServiceActions.PLAY_PAUSE))) {

                        if (status != null && !status.isACC() && status.isPlaying() && !status.isLive()) {
                            // If a MP3 Podcast is playing
                            if (status.isPaused()) {
                                resume();
                            } else {
                                pause();
                            }
                        } else if (status != null && status.isPlaying()) {
                            stop();
                        } else if (status != null && status.getURL() != null && status.getURL().length() > 0) {
                            playURL(status.getURL(), status.isACC());
                        } else {
                            // Play the default channel
                            playChannel(1);
                        }
                    } else if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(
                            ServiceActions.PREVIOUS))) {
                        if (RadioSettingsDelegate.getInstance().getChannelCount() <= 1) {
                            return;
                        }

                        int channelId = 1;
                        if (status != null && status.getChannelId() > 0) {
                            channelId = status.getChannelId() - 1;
                            if (channelId == 0) {
                                channelId = RadioSettingsDelegate.getInstance().getChannelCount();
                            }
                        }

                        playChannel(channelId);

                    } else if (action.equals(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.NEXT))) {

                        if (RadioSettingsDelegate.getInstance().getChannelCount() <= 1) {
                            return;
                        }

                        int channelId = 1;
                        if (status != null && status.getChannelId() > 0) {
                            channelId = status.getChannelId() + 1;
                            if (channelId > RadioSettingsDelegate.getInstance().getChannelCount()) {
                                channelId = 1;
                            }
                        }

                        playChannel(channelId);
                    }
                }
            });
        }
    };

    private void playChannel(int channelId) {
        Resources res = getResources();
        String name = RadioSettingsDelegate.getInstance().getChannelName(res, channelId);
        String url = RadioSettingsDelegate.getInstance().getChannelURL(channelId);
        boolean acc = RadioSettingsDelegate.getInstance().isACC(channelId);
        status = new PlaybackStatus(url, name, acc, true, channelId, false, PodcastSource.Unknown);
        playURL(url, acc);
    }

    private void registerCommandReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.STOP));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PLAY));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PAUSE));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.UPDATE));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.DOWNLOAD_PODCAST));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PLAY_PAUSE));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.PREVIOUS));
        filter.addAction(RadioSettingsDelegate.getInstance().getServiceAction(ServiceActions.NEXT));
        registerReceiver(commandReceiver, filter);
    }

    private PhoneStateListener phoneStateListener = new PhoneStateListener() {
        private boolean stoppedAudioOnCall = false;
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE :
                    executor.execute(new Runnable() {
                        public void run() {
                            if (stoppedAudioOnCall) {
                                if (!status.isACC() && status.isPaused()) {
                                    // If a paused MP3, just restart it
                                    Log.w(TAG, "Resuming playback after call...");
                                    resume();
                                } else {
                                    Log.w(TAG, "Restarting playback after call...");
                                    playURL(status.getURL(), status.isACC());
                                }

                                stoppedAudioOnCall = false;
                            }
                        }
                    });
                    break;
                case TelephonyManager.CALL_STATE_RINGING :
                case TelephonyManager.CALL_STATE_OFFHOOK :
                    executor.execute(new Runnable() {
                        public void run() {
                            if (status != null && status.isPlaying() && !status.isPaused()) {
                                if (status.areAdvancedControlSupported()) {
                                    Log.w(TAG, "Pause playback for call...");
                                    pause();
                                } else {
                                    Log.w(TAG, "Stop playback for call...");
                                    stop();
                                }

                                stoppedAudioOnCall = true;
                            }
                        }
                    });
                    break;
            }
        }
    };

    private void registerPhoneStateListener() {
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void unregisterPhoneStateListener() {
        TelephonyManager telephony = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephony.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
    }

    private ComponentName getMediaButtonEventReceiver() {
        return new ComponentName(this, MediaControlEventReceiver.class);
    }

    private void registerRemoteControlClient() {
        final ComponentName receiverComponentName = getMediaButtonEventReceiver();
        this.getPackageManager().setComponentEnabledSetting(receiverComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        // TODO: Call this when playback starts and when app is brought to foreground
        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            FroyoHelper.registerMediaButtonEventReceiver(audioManager, receiverComponentName);
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.register(this, receiverComponentName);
        }
    }

    private void unregisterRemoteControlClient() {
        final ComponentName receiverComponentName = getMediaButtonEventReceiver();

        if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater() && remoteControlClient != null) {
            remoteControlClient.unregister(this);
        }

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            FroyoHelper.unregisterMediaButtonEventReceiver(audioManager, receiverComponentName);
        }
    }

    private IcyStreamMeta meta;
    private Runnable updateStreamTitleTask = new Runnable() {
        public void run() {
            handler.removeCallbacks(updateStreamTitleTask); // Ensure we do not have 2 queued up.
            executorSongTitle.execute(new Runnable() {
                public void run() {

                    String title = "";
                    String artist = "";
                    String song = "";
                    boolean stop = false;

                    try {
                        if (meta != null) {
                            meta.refreshMeta();
                            title = meta.getStreamTitle();
                            artist = meta.getArtist();
                            song = meta.getTitle();
                        } else {
                            stop = true;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "updateStreamTitleTask - IOException url:" + meta.getStreamUrl(), e);
                    }

                    if (status.isPlaying() && !stop) {
                        if (title.length() > 0 && !title.equals(status.getTrackTitle())) {
                            status.setTrackTitle(title);

                            notification.displayPlaybackStatus(status.getTitle(), title, title);
                            Log.d(TAG, "updateStreamTitleTask - title:" + title);

                            if (!CupcakeHelper.isCupcake() && VersionCheck.isICSOrGreater()
                                    && remoteControlClient != null) {
                                remoteControlClient.setMetadata(status.getTitle(), song, artist);
                            }

                            sendServiceStatus(clientMessenger);
                        }

                        handler.postDelayed(updateStreamTitleTask, UPDATE_TITLE_DELAY);
                    } else {
                        Log.e(TAG, "updateStreamTitleTask - Not playing anymore");
                    }
                }
            });
        }
    };
}
