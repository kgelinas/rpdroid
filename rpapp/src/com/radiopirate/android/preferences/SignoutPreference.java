package com.radiopirate.android.preferences;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

import com.radiopirate.android.RPPreferences;

public class SignoutPreference extends DialogPreference {
    public SignoutPreference(Context oContext, AttributeSet attrs) {
        super(oContext, attrs);
    }

    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {

            // Clear the saved credentials
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            preferences.edit().putString(RPPreferences.PREF_USERNAME, "").putString(RPPreferences.PREF_PASSWORD, "")
                    .commit();

            // Let all other activities know we are logging out
            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction("com.radiopirate.android.ACTION_LOGOUT");
            getContext().sendBroadcast(broadcastIntent);
        }
    }
}
