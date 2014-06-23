package com.radiopirate.android;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.radiopirate.android.database.FavoriteOpenHelper;
import com.radiopirate.android.fragments.ChannelListFrag;
import com.radiopirate.android.fragments.ChannelListFrag.FavoriteChannelListener;
import com.radiopirate.android.fragments.NowPlayingFrag;
import com.radiopirate.android.fragments.OnPlaybackControlsListener;
import com.radiopirate.android.fragments.PlaybackControlsFrag;
import com.radiopirate.android.fragments.PodcastListFrag;
import com.radiopirate.android.fragments.PodcastListFrag.PodcastListListener;
import com.radiopirate.android.fragments.RadioPagerAdapter;
import com.radiopirate.android.fragments.RadioPagerAdapterCarMode;
import com.radiopirate.android.fragments.RadioPagerAdapterGuest;
import com.radiopirate.android.fragments.RadioPagerAdapterNoFav;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.DownloadManager.DownloadStatus;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceMessages;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.HoneycombHelper;
import com.radiopirate.lib.utils.VersionCheck;
import com.zylinc.view.ViewPagerIndicator;

/**
 * Main activity of the app.
 */
public class Main extends FragmentActivity
        implements
            OnPlaybackControlsListener,
            FavoriteChannelListener,
            PodcastListListener {

    private static final int START_POS_DEFAULT = 1;
    private static final int START_POS_HAS_FAVORITES = 0; // Start view if the user has favorites
    private static final String TAG = "RadioPirate";
    private static final String KEY_POSITION = "position";

    private RPPlayer player;
    private ProgressDialog dialog = null;
    private BroadcastReceiver receiver = null;
    private ArrayList<ChannelListFrag> channelFragments = new ArrayList<ChannelListFrag>();
    private ArrayList<PodcastListFrag> podcastFragments = new ArrayList<PodcastListFrag>();
    boolean isCarMode = false;
    RPSettings setting;

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
            if (dialog != null && status.isPlaying()) {
                dialog.dismiss();
                dialog = null;
            }

            setPlayStatus(status);
        }

        @Override
        public void onDownloadProgress(DownloadProgress progress) {

            for (PodcastListFrag fragment : podcastFragments) {
                if (progress.getStatus() == DownloadStatus.DOWNLOADING) {
                    fragment.onDownloadProgress(progress.getTitle(), progress.getProgress());
                } else if (progress.getStatus() == DownloadStatus.COMPLETED) {
                    fragment.onDownloadCompleted(progress.getTitle(), progress.getLocalPath());
                } else if (progress.getStatus() == DownloadStatus.FAILED) {
                    fragment.onDownloadFailed(progress.getTitle());
                } else {
                    Log.e(TAG, "onDownloadProgress - Unknown status: " + progress.getStatus());
                }
            }
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setting = (RPSettings) RPSettings.getInstance();

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            isCarMode = FroyoHelper.isCarMode(this);
        }

        PagerAdapter adapter = null;
        ViewPager pager = null;

        int startPosition = START_POS_DEFAULT;
        if (setting.isGuest()) {
            setContentView(R.layout.main);
            adapter = new RadioPagerAdapterGuest(getSupportFragmentManager(), this);
            pager = (ViewPager) findViewById(R.id.pager);
            startPosition = 0;
        } else if (isCarMode) {
            setContentView(R.layout.main_carmode);

            if (RPSettings.areFavoritesEnabled()) {
                adapter = new RadioPagerAdapterCarMode(getSupportFragmentManager(), this);
            } else {
                adapter = new RadioPagerAdapterNoFav(getSupportFragmentManager(), this);
            }

            pager = (ViewPager) findViewById(R.id.pagerCarMode);

            // In car mode, start at the leftmost page
            startPosition = 0;

            if (!CupcakeHelper.isCupcake() && VersionCheck.isHoneycombOrGreater()) {
                HoneycombHelper.hideActionBar(this);
            }
        } else {
            setContentView(R.layout.main);

            if (RPSettings.areFavoritesEnabled()) {
                adapter = new RadioPagerAdapter(getSupportFragmentManager(), this);
            } else {
                adapter = new RadioPagerAdapterNoFav(getSupportFragmentManager(), this);
            }
            pager = (ViewPager) findViewById(R.id.pager);

            FavoriteOpenHelper database = new FavoriteOpenHelper(this);

            if (!RPSettings.areFavoritesEnabled()) {
                startPosition = 0;
            } else if (database.hasFavorites()) {
                startPosition = START_POS_HAS_FAVORITES;
            }
        }

        player = new RPPlayer(this, handler);
        pager.setAdapter(adapter);

        if (savedInstanceState != null) {
            startPosition = savedInstanceState.getInt(KEY_POSITION, startPosition);
        }

        if (startPosition > adapter.getCount() - 1) {
            // Ensure the start position is not out of bound
            startPosition = adapter.getCount() - 1;
        }

        // Start at a custom position
        pager.setCurrentItem(startPosition);

        // Find the indicator from the layout
        ViewPagerIndicator indicator = (ViewPagerIndicator) findViewById(R.id.indicator);

        // Set the indicator as the pageChangeListener
        pager.setOnPageChangeListener(indicator);

        // Initialize the indicator.
        Resources res = getResources();
        Drawable prev = res.getDrawable(R.drawable.indicator_prev_arrow);
        Drawable next = res.getDrawable(R.drawable.indicator_next_arrow);
        indicator.setArrows(prev, next);
        indicator.init(startPosition, adapter.getCount(), (ViewPagerIndicator.PageInfoProvider) adapter);

        if (!setting.isGuest()) {
            Fragment guestFrag = getSupportFragmentManager().findFragmentById(R.id.guest_welcome);
            if (guestFrag != null && guestFrag.getView() != null) {
                guestFrag.getView().setVisibility(View.GONE);
            }
        }

        if (!setting.isGuest()) {
            String remaining = setting.getRemainingDays();
            if (remaining != null && remaining.length() > 0) {
                int remainingDays = Integer.parseInt(remaining);
                if (remainingDays < RPSettings.REMAINING_WARNING) {
                    TextView lable = (TextView) findViewById(R.id.textRemaining);
                    if (lable != null) {
                        String remainingLable = String.format(res.getString(R.string.remaining_lable), remaining);
                        lable.setText(remainingLable);
                        lable.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

        registerLogoutReceiver();
    }

    /**
     * Called when we are terminated by the OS because it needs memory Save the state so the user does not notice we
     * were stopped
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ViewPagerIndicator indicator = (ViewPagerIndicator) findViewById(R.id.indicator);
        int position = indicator.getPosition();

        outState.putInt(KEY_POSITION, position);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Main.onDestroy");
        super.onDestroy();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }

        if (player != null) {
            player.destoy(false);
            player = null;
        }

        unregisterReceiver(receiver);
        receiver = null;
        setting = null;
        channelFragments.clear();
        podcastFragments.clear();
    }

    @Override
    public void onPlayChannel(int channelId) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_channel), true);

        if (player != null) {
            player.playChannel(channelId);
        }
    }

    @Override
    public void onPlayURL(final String name, final String url, boolean isACC, PodcastSource source) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_track), true);
        if (player != null) {
            player.playURL(name, url, isACC, source);
        }
    }

    @Override
    public void onPlayPodcast(PodcastEntry podcast) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_track), true);
        if (player != null) {
            player.playPodcast(podcast);
        }
    }

    @Override
    public void onDownloadPodcast(PodcastEntry podcast, boolean autoPlay) {
        if (player != null) {
            player.downloadPodcast(podcast, autoPlay);
        }
    }

    @Override
    public void onStopPlayback() {
        if (player != null) {
            player.stopPlayback();
        }
    }

    @Override
    public void onPauseResumePlayback() {
        if (player != null) {
            player.playPause();
        }
    }

    @Override
    public void onForwardPlayback() {
        if (player != null) {
            player.forward(isCarMode ? RPPlayer.SEEK_DELAY_CAR_MODE : RPPlayer.SEEK_DELAY);
        }
    }

    @Override
    public void onRewindPlayback() {
        if (player != null) {
            player.rewind(isCarMode ? RPPlayer.SEEK_DELAY_CAR_MODE : RPPlayer.SEEK_DELAY);
        }
    }

    @Override
    public void onSeek(int position) {
        if (player != null) {
            player.seek(position);
        }
    }

    @Override
    public void onFavoritesChanged() {

        // Let all child channel lists that some favorite has change
        for (ChannelListFrag fragment : channelFragments) {
            fragment.updateList();
        }
    }

    @Override
    public void onChannelListCreated(ChannelListFrag fragment) {
        channelFragments.add(fragment);
    }

    @Override
    public void onChannelListDestroyed(ChannelListFrag fragment) {
        channelFragments.remove(fragment);
    }

    public void onPodcastStopped(String title) {

        for (PodcastListFrag fragment : podcastFragments) {
            fragment.onPodcastStopped(title);
        }
    }

    @Override
    public void onPodcastListCreated(PodcastListFrag fragment) {
        podcastFragments.add(fragment);
    }

    @Override
    public void onPodcastListDestroyed(PodcastListFrag fragment) {
        podcastFragments.remove(fragment);
    }

    private void setPlayStatus(PlaybackStatus status) {
        if (status.isPlaying()) {
            findViewById(R.id.FrameControl).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.FrameControl).setVisibility(View.GONE);
        }

        if (isCarMode) {
            if (status.isPlaying()) {
                findViewById(R.id.indicator).setVisibility(View.GONE);

                if (findViewById(R.id.pagerCarMode) != null) {
                    findViewById(R.id.pagerCarMode).setVisibility(View.GONE);
                }
            } else {
                findViewById(R.id.indicator).setVisibility(View.VISIBLE);

                if (findViewById(R.id.pagerCarMode) != null) {
                    findViewById(R.id.pagerCarMode).setVisibility(View.VISIBLE);
                }
            }
        }

        PlaybackControlsFrag controls = (PlaybackControlsFrag) getSupportFragmentManager().findFragmentById(
                R.id.controls);
        if (controls != null) {
            controls.showAdvancedControls(status.areAdvancedControlSupported());

            if (!status.isLive()) {
                controls.setProgress(status);
            }
        }

        NowPlayingFrag nowPlaying = (NowPlayingFrag) getSupportFragmentManager().findFragmentById(R.id.now_playing);
        if (nowPlaying != null) {
            // On Honeycomb we display what is playing
            nowPlaying.setStatus(status);
        }

        if (status.isPaused()) {
            ((ImageButton) findViewById(R.id.PlayBtn)).setImageResource(R.drawable.play_large_blue);
        } else {
            ((ImageButton) findViewById(R.id.PlayBtn)).setImageResource(R.drawable.pause_large_blue);
        }

        if (!status.isLive() && (!status.isPlaying() || status.isPaused())) {
            onPodcastStopped(status.getTitle());
        }
    }

    /** Menu handling **/

    /**
     * Opens menu by clicking on "Menu" hard button
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (isCarMode) {
            return false;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.layout.main_menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.exit :
                if (player != null) {
                    player.stopPlayback();
                    player.destoy(true);
                    player = null;
                }
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
                startActivity(new Intent(Main.this, StartUp.class));
                finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.radiopirate.android.ACTION_LOGOUT");
        registerReceiver(receiver, intentFilter);
    }
}
