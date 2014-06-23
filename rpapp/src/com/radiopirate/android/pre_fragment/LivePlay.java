package com.radiopirate.android.pre_fragment;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.res.Resources;
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
import com.radiopirate.android.fragments.ChannelEntry;
import com.radiopirate.android.fragments.ChannelListAdapter;
import com.radiopirate.android.fragments.OnPlaybackControlsListener;
import com.radiopirate.lib.utils.RPUtil;

/**
 * Main activity of the app. Allows live play of a channel
 */
public class LivePlay extends ListActivity {

    private static final String TAG = "RP Player";
    private static final int MENU_ITEM_PLAY = Menu.FIRST;
    private OnPlaybackControlsListener listener;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ListView lv = getListView();
        Resources res = getResources();
        RPSettings settings = (RPSettings) RPSettings.getInstance();
        ArrayList<ChannelEntry> channels = settings.getChannelList(res, RPUtil.doesSupportGoogleTV(this));

        ChannelListAdapter listAdapter = new ChannelListAdapter(this, channels);
        setListAdapter(listAdapter);

        // Inform the list we provide context menus for items
        lv.setOnCreateContextMenuListener(this);

        this.listener = (OnPlaybackControlsListener) getParent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.listener = null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
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

        ChannelEntry channel = (ChannelEntry) getListAdapter().getItem(info.position);

        // Setup the menu header
        menu.setHeaderTitle(channel.getChannelName());

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

        ChannelEntry channel = (ChannelEntry) getListAdapter().getItem(info.position);

        switch (item.getItemId()) {

            case MENU_ITEM_PLAY :
                listener.onPlayChannel(channel.getChannelId());
                break;
        }
        return false;
    }
}
