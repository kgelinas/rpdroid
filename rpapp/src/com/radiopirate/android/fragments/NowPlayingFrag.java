package com.radiopirate.android.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.radiopirate.android.R;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.RPUtil;
import com.radiopirate.lib.utils.VersionCheck;

public class NowPlayingFrag extends Fragment {
    private boolean isCarMode = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = null;
        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            isCarMode = FroyoHelper.isCarMode(this.getActivity());
        }

        if (isCarMode) {
            view = inflater.inflate(R.layout.now_playing_carmode, container, false);
        } else {
            view = inflater.inflate(R.layout.now_playing, container, false);
        }

        return view;
    }

    public void setStatus(PlaybackStatus status) {

        if (getView() == null) {
            return;
        }

        String title = status.getTitle();
        if (isCarMode && !status.isLive()) {
            title = RPUtil.trimCarModePodcastTitle(title);
        }

        ((TextView) getView().findViewById(R.id.stream)).setText(title);
        ((TextView) getView().findViewById(R.id.title)).setText(status.getTrackTitle());
    }
}
