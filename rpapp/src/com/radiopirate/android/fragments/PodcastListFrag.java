package com.radiopirate.android.fragments;

import java.io.File;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.radiopirate.android.R;
import com.radiopirate.android.RPSettings;
import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.RPUtil;
import com.radiopirate.lib.utils.VersionCheck;

public class PodcastListFrag extends ListFragment {

    private static final String TAG = "RadioPirate";
    private static final int MENU_ITEM_PLAY = Menu.FIRST;
    private static final int MENU_ITEM_DL = Menu.FIRST + 1;
    private static final int MENU_ITEM_DEL = Menu.FIRST + 2;
    private static final int MENU_ITEM_DEL_FROM_HERE = Menu.FIRST + 3;
    private static final int MENU_ITEM_DL_PLAY = Menu.FIRST + 4;

    private OnPlaybackControlsListener listener;
    private ProgressDialog progressDialog = null;
    private PodcastListListener podcastListListener = null;
    private String downloadingTitle = ""; // Title of the podcast for which the download progress is shown
    private UpdatePodcast podcastUpdater = null;
    private UpdateCachedDataTask updateCachedDataTask = null;
    private PodcastSource podcastSource = PodcastSource.RP;

    public interface PodcastListListener {
        void onPodcastListCreated(PodcastListFrag fragment);
        void onPodcastListDestroyed(PodcastListFrag fragment);
    }

    public static PodcastListFrag newInstance(PodcastSource podcastSource) {
        Log.i(TAG, "PodcastListFrag.newInstance()");

        PodcastListFrag fragment = new PodcastListFrag();
        fragment.podcastSource = podcastSource;
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (OnPlaybackControlsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPlaybackControlsListener");
        }

        try {
            podcastListListener = (PodcastListListener) activity;
            podcastListListener.onPodcastListCreated(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement PodcastListListener");
        }
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        setEmptyText(getResources().getString(R.string.podcast_fail));

        // call the DoInBackground AsyncTask class
        podcastUpdater = new UpdatePodcast();
        podcastUpdater.execute();

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
    }

    @Override
    public void onDestroy() {
        if (podcastUpdater != null) {
            podcastUpdater.cancel(true);
            podcastUpdater = null;
        }

        if (updateCachedDataTask != null) {
            updateCachedDataTask.cancel(true);
            updateCachedDataTask = null;
        }

        super.onDestroy();

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        podcastListListener.onPodcastListDestroyed(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(position);
        listener.onPlayPodcast(podcast);
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

        final PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(info.position);

        // Setup the menu header
        menu.setHeaderTitle(podcast.getPodcastDisplayName());

        // Add menu entries
        MenuItem item = menu.add(0, MENU_ITEM_PLAY, 0, R.string.play);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                listener.onPlayPodcast(podcast);
                return true;
            }
        });

        if (podcast.isDownloaded()) {
            item = menu.add(0, MENU_ITEM_DEL, 0, R.string.delete);

            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    File file = new File(podcast.getLocalPath());
                    boolean deleted = file.delete();
                    if (deleted) {
                        PodcastOpenHelper database = new PodcastOpenHelper(PodcastListFrag.this.getActivity());
                        podcast.setLocalPath("");

                        PodcastListAdapter adapter = (PodcastListAdapter) getListAdapter();

                        if (podcast.isLocalOnly()) {
                            // The file is not part of the RSS feed anymore, remove it from the list view and the dB
                            adapter.remove(podcast);
                            database.deleteEntry(podcast.getPodcastTitle(), podcastSource);
                        } else {
                            // Clear the path only, keep the progress, it will show until the podcast is removed from
                            // the RSS feed
                            database.setLocalPath(podcast.getPodcastTitle(), "", podcast.getPubDateStr(), podcastSource);
                        }

                        if (adapter != null) {
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.e(TAG, "Failed to delete file : " + file.toString());
                        Toast.makeText(PodcastListFrag.this.getActivity(), R.string.toast_poadcast_delete_failed,
                                Toast.LENGTH_LONG);
                    }
                    return deleted;
                }
            });

            item = menu.add(0, MENU_ITEM_DEL_FROM_HERE, 0, R.string.delete_from_here);

            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    PodcastListAdapter adapter = (PodcastListAdapter) getListAdapter();

                    for (int i = adapter.getPosition(podcast); i < adapter.getCount();) {
                        PodcastEntry curPodcast = adapter.getItem(i);

                        File file = new File(curPodcast.getLocalPath());
                        boolean deleted = file.delete();
                        if (deleted) {
                            PodcastOpenHelper database = new PodcastOpenHelper(PodcastListFrag.this.getActivity());
                            curPodcast.setLocalPath("");

                            if (curPodcast.isLocalOnly()) {
                                // The file is not part of the RSS feed anymore, remove it from the list view and the dB
                                adapter.remove(curPodcast);
                                database.deleteEntry(curPodcast.getPodcastTitle(), podcastSource);
                                // No i++ in this path since the list shrank
                            } else {
                                // Clear the path only, keep the progress, it will show until the podcast is removed
                                // from
                                // the RSS feed
                                database.setLocalPath(curPodcast.getPodcastTitle(), "", curPodcast.getPubDateStr(),
                                        podcastSource);
                                i++;
                            }
                        } else {
                            i++;
                        }
                    }

                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                    }

                    return true;
                }
            });
        } else {
            item = menu.add(0, MENU_ITEM_DL, 0, R.string.download);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(getResources().getString(R.string.pod_dlding));
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                    downloadingTitle = podcast.getPodcastTitle();
                    podcast.setDownloading(true);

                    BaseAdapter adapter = (BaseAdapter) getListAdapter();
                    adapter.notifyDataSetChanged();
                    listener.onDownloadPodcast(podcast, false);
                    return true;
                }
            });

            item = menu.add(0, MENU_ITEM_DL_PLAY, 0, R.string.download_play);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }

                    progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    progressDialog.setMessage(getResources().getString(R.string.pod_dlding));
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                    downloadingTitle = podcast.getPodcastTitle();
                    podcast.setDownloading(true);

                    BaseAdapter adapter = (BaseAdapter) getListAdapter();
                    adapter.notifyDataSetChanged();
                    listener.onDownloadPodcast(podcast, true);
                    return true;
                }
            });
        }
    }

    public void onPodcastStopped(String title) {
        updateCachedDataTask = new UpdateCachedDataTask();
        updateCachedDataTask.execute(title);
    }

    public void onDownloadProgress(String title, int progress) {

        if (getListAdapter() == null) {
            return;
        }

        for (int i = 0; i < getListAdapter().getCount(); ++i) {
            final PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(i);

            if (podcast.getPodcastTitle().equals(title)) {

                if (!podcast.isDownloading()) {
                    podcast.setDownloading(true);

                    BaseAdapter adapter = (BaseAdapter) getListAdapter();
                    adapter.notifyDataSetChanged();
                    break;
                }
            }
        }

        if (progressDialog != null && downloadingTitle.equals(title)) {
            progressDialog.setProgress(progress);
        }
    }

    public void onDownloadCompleted(String title, String filename) {
        Log.i(TAG, "onDownloadCompleted - title:" + title + " filename:" + filename);

        if (getListAdapter() == null) {
            return;
        }

        for (int i = 0; i < getListAdapter().getCount(); ++i) {
            final PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(i);

            if (podcast.getPodcastTitle().equals(title)) {
                podcast.setDownloading(false);
                podcast.setLocalPath(filename);

                BaseAdapter adapter = (BaseAdapter) getListAdapter();
                adapter.notifyDataSetChanged();
                break;
            }
        }

        if (downloadingTitle.equals(title)) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    public void onDownloadFailed(String title) {
        Log.w(TAG, "onDownloadFailed - title:" + title);

        if (getListAdapter() == null) {
            return;
        }

        for (int i = 0; i < getListAdapter().getCount(); ++i) {
            final PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(i);

            if (podcast.getPodcastTitle().equals(title)) {
                podcast.setDownloading(false);

                BaseAdapter adapter = (BaseAdapter) getListAdapter();
                adapter.notifyDataSetChanged();
                break;
            }
        }

        Toast.makeText(PodcastListFrag.this.getActivity(), R.string.toast_download_poadcast_failed, Toast.LENGTH_LONG)
                .show();
        if (downloadingTitle.equals(title)) {
            if (progressDialog != null) {
                progressDialog.dismiss();
                progressDialog = null;
            }
        }
    }

    private class UpdateCachedDataTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... values) {
            String title = values[0];
            PodcastOpenHelper database = new PodcastOpenHelper(PodcastListFrag.this.getActivity());

            if (getListAdapter() == null) {
                Log.w(TAG, "UpdateCachedDataTask - No list adapter");
                return null;
            }

            for (int i = 0; i < getListAdapter().getCount(); ++i) {
                final PodcastEntry podcast = (PodcastEntry) getListAdapter().getItem(i);

                if (podcast.getPodcastTitle().equals(title)) {
                    int duration = database.getDuration(podcast.getPodcastTitle(), podcastSource);
                    int currentPosition = database.getCurrentPosition(podcast.getPodcastTitle());
                    String localPath = database.getLocalPath(podcast.getPodcastTitle());

                    podcast.setCachedValue(localPath, currentPosition, duration);
                    break;
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void unusued) {

            BaseAdapter adapter = (BaseAdapter) getListAdapter();

            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    // method to populate the listview with the RSS titles
    private void populateListView(ArrayList<PodcastEntry> array) {

        if (getActivity() == null) {
            Log.e(TAG, "activity is null");
            return;
        }
        if (array == null) {
            Log.e(TAG, "array is null");
        }
        PodcastListAdapter listAdapter = new PodcastListAdapter(getActivity(), array);
        setListAdapter(listAdapter);
    }

    /**
     * Asynchronously download and parse the RSS feed
     */
    private class UpdatePodcast extends AsyncTask<Void, Void, ArrayList<PodcastEntry>> {

        protected ArrayList<PodcastEntry> doInBackground(Void... unused) {

            ArrayList<PodcastEntry> list = ((RPSettings) RPSettings.getInstance()).getPodcasts(podcastSource);

            if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
                boolean isCarMode = FroyoHelper.isCarMode(PodcastListFrag.this.getActivity());

                if (isCarMode) {
                    for (PodcastEntry entry : list) {
                        String trimmedTitle = RPUtil.trimCarModePodcastTitle(entry.getPodcastDisplayName());
                        entry.setPodcastDisplayName(trimmedTitle);
                    }
                }
            }

            return list;
        }

        @Override
        protected void onPostExecute(ArrayList<PodcastEntry> array) {
            if (PodcastListFrag.this == null || PodcastListFrag.this.getActivity() == null) {
                populateListView(new ArrayList<PodcastEntry>());
                return;
            }

            if (array.size() == 0) {
                Toast.makeText(PodcastListFrag.this.getActivity(), R.string.toast_poadcast_failed, Toast.LENGTH_LONG)
                        .show();
            }

            populateListView(array);

            PodcastListFrag.this.podcastUpdater = null;
        }
    }
}
