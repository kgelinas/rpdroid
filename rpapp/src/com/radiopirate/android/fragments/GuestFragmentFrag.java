package com.radiopirate.android.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.radiopirate.android.Login;
import com.radiopirate.android.R;
import com.radiopirate.android.RPSettings;

public class GuestFragmentFrag extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.guest_welcome, container, false);

        final Button login = (Button) view.findViewById(R.id.bLogin);
        final Button subscribe = (Button) view.findViewById(R.id.bSubscribe);

        login.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), Login.class);

                startActivity(intent);
                getActivity().finish();
            }
        });

        if (subscribe != null) {
            subscribe.setOnClickListener(new OnClickListener() {

                public void onClick(View view) {

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(RPSettings.getSiteURL()
                            + "/store/category/abonnements/"));
                    startActivity(browserIntent);
                }
            });
        }

        return view;
    }
}
