package com.radiopirate.android.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.radiopirate.android.R;
import com.radiopirate.lib.service.PlaybackStatus;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.FroyoHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class PlaybackControlsFrag extends Fragment {

    private static final int UPDATE_DELAY = 1000;
    private static final String KEY_MAX = "keyMax";
    private static final String KEY_START_PROGRESS = "keyStartProgress";
    private static final String KEY_START_TIME = "keyStartTime";
    private OnPlaybackControlsListener listener;
    private Handler handler = new Handler();
    private long startTime;
    private int startProgress;
    private boolean isSeeking;

    private Runnable updateProgressTask = new Runnable() {
        public void run() {
            updateProgress();
        }
    };

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (startTime != 0) {
            handler.removeCallbacks(updateProgressTask);
            updateProgress();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.PlayProgress);
        int max = progressBar.getMax();

        outState.putInt(KEY_MAX, max);
        outState.putLong(KEY_START_TIME, startTime);
        outState.putInt(KEY_START_PROGRESS, startProgress);
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateProgressTask);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = null;
        isSeeking = false;

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

        button = (ImageButton) view.findViewById(R.id.RWBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onRewindPlayback();
            }

        });

        button = (ImageButton) view.findViewById(R.id.PlayBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onPauseResumePlayback();
            }
        });

        button = (ImageButton) view.findViewById(R.id.FFBtn);
        button.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                listener.onForwardPlayback();
            }
        });

        SeekBar progressBar = (SeekBar) view.findViewById(R.id.PlayProgress);
        if (savedInstanceState != null) {
            int max = savedInstanceState.getInt(KEY_MAX, 0);
            startProgress = savedInstanceState.getInt(KEY_START_PROGRESS, 0);
            startTime = savedInstanceState.getLong(KEY_START_TIME, 0);

            if (startTime != 0) {
                progressBar.setMax(max);
                updateProgress(view);
                return view;
            }
        }

        progressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                int progress = seekBar.getProgress();
                listener.onSeek(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateProgressLable(getView(), progress, seekBar.getMax());
                }
            }
        });

        progressBar.setMax(100);
        progressBar.setProgress(0);
        updateProgressLable(view, 0, 60 * 60 * 1000);

        return view;
    }

    public void showAdvancedControls(boolean show) {
        ImageButton button;
        View view = getView();

        button = (ImageButton) view.findViewById(R.id.RWBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        button = (ImageButton) view.findViewById(R.id.PlayBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        button = (ImageButton) view.findViewById(R.id.FFBtn);
        button.setVisibility(show ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.PlayProgressControls).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void setProgress(PlaybackStatus status) {
        startProgress = status.getProgress();
        int max = status.getDuration();

        ProgressBar progressBar = (ProgressBar) getView().findViewById(R.id.PlayProgress);

        progressBar.setMax(max);
        progressBar.setProgress(startProgress);
        progressBar.setSecondaryProgress(max * status.getDownloadPercent() / 100);

        handler.removeCallbacks(updateProgressTask);

        if (status.isPlaying() && !status.isPaused()) {
            startTime = System.currentTimeMillis();
            handler.postDelayed(updateProgressTask, UPDATE_DELAY);
        } else {
            startTime = 0;
        }

        updateProgressLable(getView(), startProgress, max);
    }

    private void updateProgress(View view) {
        long millis = System.currentTimeMillis() - startTime;

        if (view == null) {
            return;
        }

        ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.PlayProgress);

        if (!isSeeking) {
            progressBar.setProgress(startProgress + (int) millis);
            updateProgressLable(view, millis + startProgress, progressBar.getMax());
        }

        handler.postDelayed(updateProgressTask, UPDATE_DELAY);
    }

    private void updateProgressLable(View view, long progress, int max) {
        TextView text = (TextView) view.findViewById(R.id.PlayProgressLable);
        if (text != null) {
            String progressValue = miliToText(progress) + "/" + miliToText(max);
            text.setText(progressValue);
        }
    }

    private String miliToText(long millis) {
        long totalSec = millis / 1000;

        return totalSec / 60 + ":" + String.format("%02d", totalSec % 60);
    }

    private void updateProgress() {
        updateProgress(getView());
    }
}
