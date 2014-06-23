package com.radiopirate.android.pre_fragment;

import java.util.ArrayList;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.radiopirate.android.R;
import com.radiopirate.android.RPSettings;
import com.radiopirate.android.fragments.OnPlaybackControlsListener;
import com.radiopirate.android.fragments.PodcastListAdapter;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;

public class Podcasts extends ListActivity {

    private static final String TAG = "RP Player";
    private static final int MENU_ITEM_PLAY = Menu.FIRST;
    private OnPlaybackControlsListener listener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // call the DoInBackground AsyncTask class
        new UpdatePodcast().execute();

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);

        this.listener = (OnPlaybackControlsListener) getParent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.listener = null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(position);
        onPlayURL(podcast.getPodcastDisplayName(), podcast.getURL(), !RPSettings.areRPPodcastsMP3());
    }

    // method to populate the listview with the RSS titles
    public void populateListView(ArrayList<PodcastEntry> array) {

        PodcastListAdapter listAdapter = new PodcastListAdapter(this, array);
        setListAdapter(listAdapter);
    }

    /**
     * Asynchronously download and parse the RSS feed
     */
    private class UpdatePodcast extends AsyncTask<Void, Void, ArrayList<PodcastEntry>>
            implements
                DialogInterface.OnCancelListener {
        private ProgressDialog dialog;

        protected void onPreExecute() {
            dialog = ProgressDialog.show(Podcasts.this, "", getResources().getString(R.string.loading_podcast_msg),
                    true);
        }

        protected ArrayList<PodcastEntry> doInBackground(Void... unused) {
            return RPSettings.getInstance().getPodcasts();
        }

        protected void onPostExecute(ArrayList<PodcastEntry> array) {
            dialog.dismiss();
            populateListView(array);
        }

        public void onCancel(DialogInterface dialog) {
            cancel(true);
            dialog.dismiss();
        }
    }

    private void onPlayURL(final String name, final String url, final boolean isACC) {
        listener.onPlayURL(name, url, isACC, PodcastSource.RP);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(info.position);

        // Setup the menu header
        menu.setHeaderTitle(podcast.getPodcastDisplayName());

        // Add menu entries
        menu.add(0, MENU_ITEM_PLAY, 0, R.string.play);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(info.position);

        switch (item.getItemId()) {

            case MENU_ITEM_PLAY :
                onPlayURL(podcast.getPodcastDisplayName(), podcast.getURL(), !RPSettings.areRPPodcastsMP3());
                break;
        }
        return false;
    }
}
