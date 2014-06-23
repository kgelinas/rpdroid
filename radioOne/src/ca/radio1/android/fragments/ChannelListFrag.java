package ca.radio1.android.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
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
import ca.radio1.android.ChannelInfo;
import ca.radio1.android.R;
import ca.radio1.android.RadioOneSettings;

public class ChannelListFrag extends ListFragment {

    private static final String TAG = "RP Player";
    private static final int MENU_ITEM_PLAY = Menu.FIRST;
    private static final int MENU_ITEM_CHANNEL_INFO = Menu.FIRST + 1;

    private OnPlaybackControlsListener listener;
    private RadioOneSettings setting;

    /**
     * 
     * @param favorite
     *            True if this Fragment displays only the favorite channels
     * @return
     */
    public static ChannelListFrag newInstance() {
        Log.i(TAG, "ChannelListFrag.newInstance()");

        ChannelListFrag fragment = new ChannelListFrag();

        Bundle args = new Bundle();
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
    }

    @Override
    public void onActivityCreated(Bundle savedState) {
        super.onActivityCreated(savedState);

        setting = new RadioOneSettings(this.getActivity());
        updateList();

        // Inform the list we provide context menus for items
        getListView().setOnCreateContextMenuListener(this);
    }

    @Override
    public void onDestroy() {
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

        item = menu.add(0, MENU_ITEM_CHANNEL_INFO, 0, R.string.show_channel_info);
        item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                RadioOneSettings settings = new RadioOneSettings(ChannelListFrag.this.getActivity());
                Intent intent = new Intent(ChannelListFrag.this.getActivity(), ChannelInfo.class);
                intent.putExtra(ChannelInfo.EXTRA_URL, settings.getChannelURL(channelId));
                intent.putExtra(ChannelInfo.EXTRA_TITLE, channel.getChannelName());
                intent.putExtra(ChannelInfo.EXTRA_IS_ACC, RadioOneSettings.isChannelACC(channelId));
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
        ArrayList<ChannelEntry> channels = setting.getChannelList(res);

        // Save the current scroll position
        ListView listView = getListView();
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        ChannelListAdapter listAdapter = new ChannelListAdapter(getActivity(), channels);
        setListAdapter(listAdapter);

        // restore the scroll position
        listView.setSelectionFromTop(index, top);
    }
}
