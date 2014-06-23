package com.radiopirate.android.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
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
import android.widget.ListView;

import com.radiopirate.android.ChannelInfo;
import com.radiopirate.android.R;
import com.radiopirate.android.RPSettings;
import com.radiopirate.android.database.FavoriteOpenHelper;
import com.radiopirate.lib.utils.RPUtil;

public class ChannelListFrag extends ListFragment {

    private static final String TAG = "RP Player";
    private static final int MENU_ITEM_PLAY = Menu.FIRST;
    private static final int MENU_ITEM_FAVORITE = Menu.FIRST + 1;
    private static final int MENU_ITEM_CHANNEL_INFO = Menu.FIRST + 2;
    public static final String KEY_FAVORITE = "favorite";

    private OnPlaybackControlsListener listener;
    private FavoriteChannelListener favoritesListener;
    private boolean isFavorite = false; // True if this is the favorite view
    private RPSettings setting;

    public interface FavoriteChannelListener {
        void onFavoritesChanged();
        void onChannelListCreated(ChannelListFrag fragment);
        void onChannelListDestroyed(ChannelListFrag fragment);
    }

    /**
     * 
     * @param favorite
     *            True if this Fragment displays only the favorite channels
     * @return
     */
    public static ChannelListFrag newInstance(boolean favorite) {
        Log.i(TAG, "ChannelListFrag.newInstance()");

        ChannelListFrag fragment = new ChannelListFrag();

        Bundle args = new Bundle();
        args.putBoolean(KEY_FAVORITE, favorite);
        fragment.setArguments(args);

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
            favoritesListener = (FavoriteChannelListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement FavoriteChannelListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        favoritesListener.onChannelListCreated(this);
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        isFavorite = getArguments().getBoolean(KEY_FAVORITE);
        setting = (RPSettings) RPSettings.getInstance();
        updateList();

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
    }

    @Override
    public void onDestroy() {
        favoritesListener.onChannelListDestroyed(this);

        super.onDestroy();
        setting = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ChannelEntry channel = (ChannelEntry) getListAdapter().getItem(position);
        listener.onPlayChannel(channel.getChannelId());
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

        final ChannelEntry channel = (ChannelEntry) getListAdapter().getItem(info.position);
        final int channelId = channel.getChannelId();

        // Setup the menu header
        menu.setHeaderTitle(channel.getChannelName());

        // Add menu entries
        MenuItem item = menu.add(0, MENU_ITEM_PLAY, 0, R.string.play);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                listener.onPlayChannel(channelId);
                return true;
            }
        });

        if (setting.isGuest()) {
            return;
        }

        item = menu.add(0, MENU_ITEM_FAVORITE, 0, (channel.isFavorite()
                ? R.string.favorite_remove
                : R.string.favorite_add));
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                channel.setFavorite(!channel.isFavorite());

                new AsyncTask<Integer, Void, Void>() {

                    @Override
                    protected Void doInBackground(Integer... params) {
                        FavoriteOpenHelper database = new FavoriteOpenHelper(ChannelListFrag.this.getActivity());
                        database.updateFavorite(params[0], params[1] != 0);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        favoritesListener.onFavoritesChanged();
                    }
                }.execute(channel.getChannelId(), channel.isFavorite() ? 1 : 0);
                return true;
            }
        });

        item = menu.add(0, MENU_ITEM_CHANNEL_INFO, 0, R.string.show_channel_info);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(ChannelListFrag.this.getActivity(), ChannelInfo.class);
                intent.putExtra(ChannelInfo.EXTRA_URL, setting.getChannelURL(channelId));
                intent.putExtra(ChannelInfo.EXTRA_TITLE, channel.getChannelName());
                intent.putExtra(ChannelInfo.EXTRA_IS_ACC, setting.isACC(channelId));
                intent.putExtra(ChannelInfo.EXTRA_IS_LIVE, true);
                intent.putExtra(ChannelInfo.EXTRA_CHANNEL_ID, channel.getChannelId());
                startActivity(intent);
                return true;
            }
        });
    }
    /**
     * This is needed to allow the parent to tell an instance when a neighbor changes the favorite list
     */
    public void updateList() {
        Resources res = getResources();
        ArrayList<ChannelEntry> channels = setting.getChannelList(res, RPUtil.doesSupportGoogleTV(getActivity()));

        FavoriteOpenHelper database = new FavoriteOpenHelper(getActivity());
        database.loadFavorites(channels);

        if (getView() == null) {
            // getListView() will cause a crash if called when getView() is null
            return;
        }

        // Save the current scroll position
        ListView listView = getListView();
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        if (isFavorite) {
            ArrayList<ChannelEntry> favoriteChannels = new ArrayList<ChannelEntry>();
            for (ChannelEntry channel : channels) {
                if (channel.isFavorite()) {
                    favoriteChannels.add(channel);
                }
            }

            ChannelListAdapter listAdapter = new ChannelListAdapter(getActivity(), favoriteChannels);
            setListAdapter(listAdapter);

            setEmptyText(getResources().getString(R.string.favorite_empty));
        } else {
            ChannelListAdapter listAdapter = new ChannelListAdapter(getActivity(), channels);
            setListAdapter(listAdapter);
        }

        // restore the scroll position
        listView.setSelectionFromTop(index, top);
    }
}
