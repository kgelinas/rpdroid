package ca.radio1.android.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ca.radio1.android.R;
import ca.radio1.android.RadioOneSettings;

/**
 * Holds the data displayed in the channel list and own the layout of each entry
 */
public class ChannelListAdapter extends ArrayAdapter<ChannelEntry> {

    private Activity parentActivity;

    public ChannelListAdapter(Activity activity, ArrayList<ChannelEntry> contacts) {

        super(activity, R.layout.channel_entry, contacts);
        this.parentActivity = activity;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater inflater = parentActivity.getLayoutInflater();
            v = inflater.inflate(R.layout.channel_entry, null);
        }

        final ChannelEntry entry = getItem(position);
        if (entry != null) {

            ImageView channelImage = (ImageView) v.findViewById(R.id.channel_image);
            if (channelImage != null) {

                if (RadioOneSettings.showChannelImage()) {
                    channelImage.setImageResource(RadioOneSettings.getChannelImage(entry.getChannelId()));
                } else {
                    channelImage.setVisibility(View.GONE);
                }
            }

            TextView channelName = (TextView) v.findViewById(R.id.channel_name);
            if (channelName != null) {
                channelName.setText(entry.getChannelName());
            }

            Resources res = parentActivity.getResources();
            TextView channelDescription = (TextView) v.findViewById(R.id.channel_description);
            if (channelDescription != null) {
                channelDescription.setText(RadioOneSettings.getDescription(res, entry.getChannelId()));
            }
        }
        return v;
    }
}
