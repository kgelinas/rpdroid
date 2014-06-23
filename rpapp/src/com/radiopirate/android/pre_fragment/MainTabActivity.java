package com.radiopirate.android.pre_fragment;

import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TabHost;

import com.radiopirate.android.Login;
import com.radiopirate.android.Preferences;
import com.radiopirate.android.R;
import com.radiopirate.android.RPPlayer;
import com.radiopirate.android.RPPlayerHanlder;
import com.radiopirate.android.RPSettings;
import com.radiopirate.android.StartUp;
import com.radiopirate.android.fragments.OnPlaybackControlsListener;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceMessages;

/**
 * Main activity of the app for Android versions that don't support fragments.
 */
public class MainTabActivity extends TabActivity implements OnPlaybackControlsListener {

    private static final String TAG = "RadioPirate";

    private RPPlayer player;
    private ProgressDialog dialog = null;
    private BroadcastReceiver receiver = null;

    private RPPlayerHanlder handler = new RPPlayerHanlder() {

        @Override
        public void onPlayResponse(ServiceMessages.RequestResult result) {
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        }

        @Override
        public void onStopResponse(ServiceMessages.RequestResult result) {
        }

        @Override
        public void onPlayStatus(PlaybackStatus status) {
            setPlayStatus(status);
        }

        @Override
        public void onDownloadProgress(DownloadProgress progress) {

        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_tabbed);

        player = new RPPlayer(this, handler);

        RPSettings settings = (RPSettings) RPSettings.getInstance();
        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, LivePlay.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("live").setIndicator(res.getString(R.string.title_live)).setContent(intent);
        tabHost.addTab(spec);

        if (!settings.isGuest()) {
            // Do the same for the other tabs
            intent = new Intent().setClass(this, Podcasts.class);
            spec = tabHost.newTabSpec("podcast").setIndicator(res.getString(R.string.title_podcast)).setContent(intent);
            tabHost.addTab(spec);
            /*
             * spec = tabHost.newTabSpec("podcast_marto").setIndicator(res.getString(R.string.title_podcast_marto))
             * .setContent(intent); tabHost.addTab(spec);
             */
        }

        tabHost.setCurrentTab(0);

        ImageButton button = (ImageButton) findViewById(R.id.StopBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onStopPlayback();
            }

        });

        button = (ImageButton) findViewById(R.id.RWBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onRewindPlayback();
            }

        });

        button = (ImageButton) findViewById(R.id.PlayBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onPauseResumePlayback();
            }
        });

        button = (ImageButton) findViewById(R.id.FFBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                onForwardPlayback();
            }
        });

        if (!settings.isGuest()) {
            View guestGroup = findViewById(R.id.guest_welcome);
            if (guestGroup != null) {
                guestGroup.setVisibility(View.GONE);
            }
        } else {
            final Button login = (Button) findViewById(R.id.bLogin);

            login.setOnClickListener(new OnClickListener() {

                public void onClick(View view) {
                    Intent intent = new Intent(MainTabActivity.this, Login.class);

                    startActivity(intent);
                    finish();
                }
            });
        }

        registerLogoutReceiver();
    }

    public void onContentChanged() {
        super.onContentChanged();
        Log.d(TAG, "MainTabActivity.onContentChanged");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "MainTabActivity.onDestroy");
        super.onDestroy();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        if (player != null) {
            player.destoy(false);
        }

        unregisterReceiver(receiver);
    }

    @Override
    public void onPlayChannel(int channelId) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(MainTabActivity.this, "", getResources().getString(R.string.buffering_channel),
                true);
        player.playChannel(channelId);
    }

    @Override
    public void onPlayURL(final String name, final String url, boolean isACC, PodcastSource source) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog
                .show(MainTabActivity.this, "", getResources().getString(R.string.buffering_track), true);
        player.playURL(name, url, isACC, source);
    }

    @Override
    public void onPlayPodcast(PodcastEntry podcast) {
        onPlayURL(podcast.getPodcastDisplayName(), podcast.getURL(), !RPSettings.areRPPodcastsMP3(),
                podcast.getPodcastSource());
    }

    @Override
    public void onDownloadPodcast(PodcastEntry podcast, boolean autoPlay) {

    }

    @Override
    public void onStopPlayback() {
        player.stopPlayback();
    }

    @Override
    public void onPauseResumePlayback() {
        player.playPause();
    }

    @Override
    public void onForwardPlayback() {
        player.forward(RPPlayer.SEEK_DELAY);
    }

    @Override
    public void onRewindPlayback() {
        player.rewind(RPPlayer.SEEK_DELAY);
    }

    @Override
    public void onSeek(int position) {
        player.seek(position);
    }

    private void setPlayStatus(PlaybackStatus status) {
        if (status.isPlaying()) {
            findViewById(R.id.FrameControl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.FrameControl).setVisibility(View.GONE);
        }

        showAdvancedControls(status.areAdvancedControlSupported());

        if (status.isPaused()) {
            ((ImageButton) findViewById(R.id.PlayBtn)).setImageResource(R.drawable.play_large_blue);
        } else {
            ((ImageButton) findViewById(R.id.PlayBtn)).setImageResource(R.drawable.pause_large_blue);
        }
    }

    public void showAdvancedControls(boolean show) {
        ImageButton button;

        button = (ImageButton) findViewById(R.id.RWBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        button = (ImageButton) findViewById(R.id.PlayBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        button = (ImageButton) findViewById(R.id.FFBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** Menu handling **/

    /**
     * Opens menu by clicking on "Menu" hard button
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.main_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.exit :
                player.stopPlayback();
                player.destoy(true);
                player = null;
                finish();
                break;
            case R.id.preference :
                Intent intent = new Intent(this, Preferences.class);
                startActivity(intent);
                break;
            case R.id.bug :
                String url = "http://www.radiopirate.com/support/";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
        }

        return true;
    }

    private void registerLogoutReceiver() {
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                player.stopPlayback();
                player.destoy(true);
                player = null;
                startActivity(new Intent(MainTabActivity.this, StartUp.class));
                finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.radiopirate.android.ACTION_LOGOUT");
        registerReceiver(receiver, intentFilter);
    }
}
