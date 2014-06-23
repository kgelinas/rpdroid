package com.radiopirate.android.fragments;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.radiopirate.android.R;
import com.zylinc.view.ViewPagerIndicator;

public class RadioPagerAdapterGuest extends FragmentPagerAdapter implements ViewPagerIndicator.PageInfoProvider {
    private static final int NUM_ITEMS = 1;
    private Context context;

    public RadioPagerAdapterGuest(FragmentManager fm, Context context) {
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
        }

        return null;
    }

    @Override
    public String getTitle(int pos) {
        Resources res = context.getResources();

        switch (pos) {
            case 0 :
                return res.getString(R.string.title_live);
        }

        return "";
    }
}
