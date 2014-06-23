package ca.radio1.android;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import ca.radio1.android.fragments.ChannelListFrag;
import ca.radio1.android.fragments.NowPlayingFrag;
import ca.radio1.android.fragments.OnPlaybackControlsListener;
import ca.radio1.android.fragments.RadioPagerAdapter;

import com.bugsense.trace.BugSenseHandler;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.DownloadManager.DownloadProgress;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.service.ServiceMessages;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.HoneycombHelper;
import com.radiopirate.lib.utils.VersionCheck;

/**
 * Main activity of the app.
 */
public class Main extends FragmentActivity implements OnPlaybackControlsListener {

    private static final int START_POS_DEFAULT = 1;
    private static final String TAG = "Radio1";

    private RPPlayer player;
    private ProgressDialog dialog = null;
    private ArrayList<ChannelListFrag> channelFragments = new ArrayList<ChannelListFrag>();
    boolean isCarMode = false;
    RadioOneSettings setting;

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

        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BugSenseHandler.setup(this, RadioOneSettings.BUG_SENSE_API_KEY);

        setting = new RadioOneSettings(this);

        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            isCarMode = FroyoHelper.isCarMode(this);
        }

        PagerAdapter adapter = null;
        ViewPager pager = null;

        int startPosition = START_POS_DEFAULT;
        if (isCarMode) {
            setContentView(R.layout.main_carmode);
            adapter = new RadioPagerAdapter(getSupportFragmentManager(), this);
            pager = (ViewPager) findViewById(R.id.pagerCarMode);

            // In car mode, start at the leftmost page
            startPosition = 0;

            if (!CupcakeHelper.isCupcake() && VersionCheck.isHoneycombOrGreater()) {
                HoneycombHelper.hideActionBar(this);
            }
        } else {
            setContentView(R.layout.main);
            adapter = new RadioPagerAdapter(getSupportFragmentManager(), this);
            pager = (ViewPager) findViewById(R.id.pager);
            startPosition = 0;
        }

        player = new RPPlayer(this, handler);
        pager.setAdapter(adapter);

        if (startPosition > adapter.getCount() - 1) {
            // Ensure the start position is not out of bound
            startPosition = adapter.getCount() - 1;
        }

        // Start at a custom position
        pager.setCurrentItem(startPosition);

        // Not sure why setting this in the XML is not enough
        if (findViewById(R.id.now_playing) != null) {
            findViewById(R.id.now_playing).setVisibility(View.GONE);
        }
    }

    /**
     * Called when we are terminated by the OS because it needs memory Save the state so the user does not notice we
     * were stopped
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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

        setting = null;
        channelFragments.clear();
    }

    @Override
    public void onPlayChannel(int channelId) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_channel), true);
        player.playChannel(channelId);
    }

    @Override
    public void onPlayURL(final String name, final String url, boolean isACC, PodcastSource source) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_track), true);
        player.playURL(name, url, isACC);
    }

    @Override
    public void onPlayPodcast(PodcastEntry podcast) {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        dialog = ProgressDialog.show(Main.this, "", getResources().getString(R.string.buffering_track), true);
        player.playPodcast(podcast);
    }

    @Override
    public void onDownloadPodcast(PodcastEntry podcast, boolean autoPlay) {
        player.downloadPodcast(podcast, autoPlay);
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
        player.forward(isCarMode ? RPPlayer.SEEK_DELAY_CAR_MODE : RPPlayer.SEEK_DELAY);
    }

    @Override
    public void onRewindPlayback() {
        player.rewind(isCarMode ? RPPlayer.SEEK_DELAY_CAR_MODE : RPPlayer.SEEK_DELAY);
    }

    @Override
    public void onSeek(int position) {
        player.seek(position);
    }

    private void setPlayStatus(PlaybackStatus status) {

        if (isCarMode) {
            if (status.isPlaying()) {
                findViewById(R.id.FrameControl).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.FrameControl).setVisibility(View.GONE);
            }

            if (status.isPlaying()) {

                if (findViewById(R.id.pagerCarMode) != null) {
                    findViewById(R.id.pagerCarMode).setVisibility(View.GONE);
                }
            } else {

                if (findViewById(R.id.pagerCarMode) != null) {
                    findViewById(R.id.pagerCarMode).setVisibility(View.VISIBLE);
                }
            }
        } else {
            if (status.isPlaying()) {

                if (findViewById(R.id.now_playing) != null) {
                    findViewById(R.id.now_playing).setVisibility(View.VISIBLE);
                }
            } else {

                if (findViewById(R.id.now_playing) != null) {
                    findViewById(R.id.now_playing).setVisibility(View.GONE);
                }
            }
        }

        NowPlayingFrag nowPlaying = (NowPlayingFrag) getSupportFragmentManager().findFragmentById(R.id.now_playing);
        if (nowPlaying != null) {
            // On Honeycomb we display what is playing
            nowPlaying.setStatus(status);
        }

        if (status.isPlaying()) {
            ((ImageButton) findViewById(R.id.PlayBtn)).setVisibility(View.GONE);
            ((ImageButton) findViewById(R.id.StopBtn)).setVisibility(View.VISIBLE);
        } else {
            ((ImageButton) findViewById(R.id.PlayBtn)).setVisibility(View.VISIBLE);
            ((ImageButton) findViewById(R.id.StopBtn)).setVisibility(View.GONE);
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
                String url = "http://code.google.com/p/rpdroid/issues/list";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                break;
        }

        return true;
    }
}
