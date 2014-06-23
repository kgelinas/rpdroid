package ca.radio1.android.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class RadioPagerAdapter extends FragmentPagerAdapter {
    private static final int NUM_ITEMS = 1;

    public RadioPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
    }

    @Override
    public int getCount() {
        return NUM_ITEMS;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0 :
                return ChannelListFrag.newInstance();
        }

        return null;
    }
}
