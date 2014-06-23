package ca.radio1.android;

import java.io.IOException;
import java.net.URL;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import ca.radio1.android.fragments.NowPlayingFrag;

import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.service.IcyStreamMeta;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.HoneycombHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class ChannelInfo extends FragmentActivity {

    public static final String EXTRA_URL = "KeyURL";
    public static final String EXTRA_TITLE = "KeyUTitle";
    public static final String EXTRA_IS_ACC = "KeyIsACC";
    public static final String EXTRA_IS_LIVE = "KeyIsLive";
    public static final String EXTRA_CHANNEL_ID = "KeyChannelId";
    private static final String TAG = "Radio1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_info);

        if (getIntent() == null) {
            return;
        }
        Intent intent = getIntent();
        Bundle data = intent.getExtras();

        String url = data.getString(ChannelInfo.EXTRA_URL);
        String title = data.getString(ChannelInfo.EXTRA_TITLE);
        boolean isACC = data.getBoolean(ChannelInfo.EXTRA_IS_ACC);
        boolean isLive = data.getBoolean(ChannelInfo.EXTRA_IS_LIVE);
        int channelId = data.getInt(ChannelInfo.EXTRA_CHANNEL_ID);

        new AsyncTask<PlaybackStatus, Void, PlaybackStatus>() {

            @Override
            protected PlaybackStatus doInBackground(PlaybackStatus... params) {
                PlaybackStatus status = params[0];
                String title = "";

                if (status.isLive()) {
                    try {
                        URL metaURL = new URL(status.getURL());
                        IcyStreamMeta meta = new IcyStreamMeta(metaURL);

                        meta.refreshMeta();
                        title = meta.getStreamTitle();
                    } catch (IOException e) {
                        Log.e(TAG, "ChannelInfo - IOException url:" + status.getURL(), e);
                    }
                }

                status.setTrackTitle(title);

                return status;
            }

            @Override
            protected void onPostExecute(PlaybackStatus status) {
                NowPlayingFrag nowPlaying = (NowPlayingFrag) getSupportFragmentManager().findFragmentById(
                        R.id.now_playing);
                if (nowPlaying != null) {
                    nowPlaying.setStatus(status);
                }
            }
        }.execute(new PlaybackStatus(url, title, isACC, isLive, channelId, false, PodcastSource.Unknown));
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!CupcakeHelper.isCupcake() && VersionCheck.isHoneycombOrGreater()) {
            HoneycombHelper.setDisplayHomeAsUpEnabled(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }
}
