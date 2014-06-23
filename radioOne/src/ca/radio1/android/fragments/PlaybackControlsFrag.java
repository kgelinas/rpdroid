package ca.radio1.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import ca.radio1.android.R;

import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class PlaybackControlsFrag extends Fragment {

    private OnPlaybackControlsListener listener;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = null;

        boolean isCarMode = false;
        if (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater()) {
            isCarMode = FroyoHelper.isCarMode(this.getActivity());
        }

        if (isCarMode) {
            view = inflater.inflate(R.layout.playback_controls_carmode, container, false);
        } else {
            view = inflater.inflate(R.layout.playback_controls, container, false);
        }

        ImageButton button = (ImageButton) view.findViewById(R.id.StopBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onStopPlayback();
            }

        });

        button = (ImageButton) view.findViewById(R.id.PlayBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onPlayChannel(1);
            }
        });

        return view;
    }
}
