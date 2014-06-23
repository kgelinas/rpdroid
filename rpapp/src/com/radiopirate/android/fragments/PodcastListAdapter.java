package com.radiopirate.android.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radiopirate.android.R;
import com.radiopirate.lib.model.PodcastEntry;

/**
 * Holds the data displayed in the podcast list and own the layout of each entry
 */
public class PodcastListAdapter extends ArrayAdapter<PodcastEntry> {

    private Activity parentActivity;

    public PodcastListAdapter(Activity activity, ArrayList<PodcastEntry> contacts) {

        super(activity, R.layout.podcast_entry, contacts);
        this.parentActivity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            v = inflater.inflate(R.layout.podcast_entry, null);
        }

        final PodcastEntry entry = getItem(position);
        if (entry != null) {

            TextView podcastTitle = (TextView) v.findViewById(R.id.podcast_name);
            if (podcastTitle != null) {
                podcastTitle.setText(entry.getPodcastDisplayName());
            }

            TextView podcastProgress = (TextView) v.findViewById(R.id.podcast_progress);
            if (podcastProgress != null) {
                if (entry.getDuration() > 0) {

                    if (entry.getDuration() - entry.getCurrentPosition() < 1000) {
                        podcastProgress.setText("100 %");
                    } else {
                        podcastProgress.setText(entry.getCurrentPosition() * 100 / entry.getDuration() + "%");
                    }
                } else {
                    podcastProgress.setText("");
                }
            }

            v.findViewById(R.id.DownloadedImage).setVisibility(entry.isDownloaded() ? View.VISIBLE : View.GONE);
            v.findViewById(R.id.DownloadProgress).setVisibility(entry.isDownloading() ? View.VISIBLE : View.GONE);
        }
        return v;
    }
}
