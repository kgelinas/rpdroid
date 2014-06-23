package com.radiopirate.android.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.radiopirate.android.R;
import com.radiopirate.lib.model.PodcastSource;
import com.zylinc.view.ViewPagerIndicator;

public class RadioPagerAdapterNoFav extends FragmentPagerAdapter implements ViewPagerIndicator.PageInfoProvider {
    private static final int NUM_ITEMS = 3;
    private Context context;

    public RadioPagerAdapterNoFav(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return ChannelListFrag.newInstance(false);
            case 1 :
                return PodcastListFrag.newInstance(PodcastSource.RP);
            case 2 :
                return PodcastListFrag.newInstance(PodcastSource.Marto);
        }

        return null;
    }

    @Override
    public String getTitle(int pos) {
        Resources res = context.getResources();

        switch (pos) {
            case 0 :
                return res.getString(R.string.title_live);
            case 1 :
                return res.getString(R.string.title_podcast);
            case 2 :
                return res.getString(R.string.title_podcast_marto);
        }

        return "";
    }
}
