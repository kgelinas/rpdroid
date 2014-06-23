package com.radiopirate.android.beta.frag;

import com.radiopirate.android.beta.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class MenuFragmentPopulate extends ListFragment {

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, null);
    }
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
	    String[] m = getResources().getStringArray(R.array.menu_title);
	    TypedArray icons = getResources().obtainTypedArray(R.array.menu_icons);

		SampleAdapter adapter = new SampleAdapter(getActivity());
		for(int i=0;i<m.length;i++){
			   adapter.add(new SampleItem(m[i],icons.getDrawable(i)));
			}

		setListAdapter(adapter);
		
		//Recycle TypedArray
		icons.recycle();
	}

	private class SampleItem {
		public String tag;
		public Drawable iconRes;
		public SampleItem(String tag, Drawable icons) {
			this.tag = tag; 
			this.iconRes = icons;
		}
	}

	public class SampleAdapter extends ArrayAdapter<SampleItem> {

		public SampleAdapter(Context context) {
			super(context, 0);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_menu, null);
			}
			ImageView icon = (ImageView) convertView.findViewById(R.id.fragment_ic_home);
			icon.setImageDrawable(getItem(position).iconRes);
			TextView title = (TextView) convertView.findViewById(R.id.fragment_text_home);
			title.setText(getItem(position).tag);

			return convertView;
		}

	}
}