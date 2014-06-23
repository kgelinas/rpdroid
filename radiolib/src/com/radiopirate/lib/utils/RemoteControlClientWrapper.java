package com.radiopirate.lib.utils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;

import com.radiopirate.lib.delegates.ResourceDelegate;

// ICS Only
public class RemoteControlClientWrapper {
    private RemoteControlClient remoteControlClient;

    Bitmap defaultArtwork;

    public RemoteControlClientWrapper(Context context) {

        Resources res = context.getResources();
        defaultArtwork = BitmapFactory.decodeResource(res, ResourceDelegate.getInstance()
                .getDrawableRemoteControlBackground());
    }

    public void register(Context context, ComponentName receiverComponentName) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        // build the PendingIntent for the remote control client
        final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(receiverComponentName);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context, 0, mediaButtonIntent, 0);

        // create the remote control client
        remoteControlClient = new RemoteControlClient(mediaPendingIntent);
        audioManager.registerRemoteControlClient(remoteControlClient);

        // These are the only 3 buttons that work.
        // Tested on GN 4.0.1 and Nexus S 4.0.3
        remoteControlClient.setTransportControlFlags(RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
                | RemoteControlClient.FLAG_KEY_MEDIA_NEXT | RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
    }

    public void unregister(Context context) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.unregisterRemoteControlClient(remoteControlClient);
        remoteControlClient = null;
    }

    public void setIsBuffering() {
        if (remoteControlClient == null) {
            return;
        }

        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_BUFFERING);
    }

    public void setIsPlaying(Context context) {
        if (remoteControlClient == null) {
            return;
        }

        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
    }

    public void setIsPaused() {
        if (remoteControlClient == null) {
            return;
        }

        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
    }

    public void setIsStopped() {
        if (remoteControlClient == null) {
            return;
        }

        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
    }

    public void setMetadata(String channel, String songTitle, String artist) {

        if (remoteControlClient == null) {
            return;
        }

        RemoteControlClient.MetadataEditor editor = remoteControlClient.editMetadata(true);
        editor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, defaultArtwork);

        // Layout on GN 4.0.1
        // Displayed in White: METADATA_KEY_TITLE
        // Displayed in small gray: METADATA_KEY_ALBUM, METADATA_KEY_ALBUMARTIST
        // Not displayed: METADATA_KEY_ARTIST, METADATA_KEY_AUTHOR, METADATA_KEY_COMPILATION, METADATA_KEY_COMPOSER,
        // METADATA_KEY_DATE, METADATA_KEY_GENRE, METADATA_KEY_WRITER.

        if (songTitle != null && songTitle.length() > 0) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, songTitle);
        }

        if (artist != null && artist.length() > 0) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST, artist);
        }

        if (channel != null && channel.length() > 0) {
            editor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, channel);
        }
        editor.apply();
    }
}
