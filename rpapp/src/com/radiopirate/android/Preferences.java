package com.radiopirate.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.radiopirate.lib.service.AutoDownloader;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.HoneycombHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class Preferences extends PreferenceActivity {
    private BroadcastReceiver receiver = null;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RPSettings.getInstance().isGuest()) {
            addPreferencesFromResource(R.xml.preferences_guest);
        } else {
            addPreferencesFromResource(R.xml.preferences);
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String user = preferences.getString(RPPreferences.PREF_USERNAME, "");

        Preference pref = findPreference(RPPreferences.PREF_USERNAME);
        Resources res = getResources();
        if (user.length() == 0) {
            user = res.getString(R.string.guest);
        }

        if (pref != null) {
            pref.setSummary(user);
        }

        Preference autoDownloadPref = findPreference(RPPreferences.PREF_AUTO_DOWNLOAD_PODCAST);

        if (autoDownloadPref != null) {
            autoDownloadPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    boolean autoDownload = (Boolean) newValue;
                    if (autoDownload) {
                        AutoDownloader.startPodcastDownloadTimer(Preferences.this);
                    } else {
                        AutoDownloader.cancelPodcastDownloadTimer(Preferences.this);
                    }

                    return true;
                }
            });
        }

        String buffer = preferences.getString(RPPreferences.PREF_BUFFER,
                res.getStringArray(R.array.Buffer_entryValues)[0]);
        updateBufferSummary(buffer);

        Preference bufferPref = findPreference(RPPreferences.PREF_BUFFER);
        bufferPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateBufferSummary((String) newValue);
                return true;
            }
        });

        registerLogoutReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!CupcakeHelper.isCupcake() && VersionCheck.isHoneycombOrGreater()) {
            HoneycombHelper.setDisplayHomeAsUpEnabled(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home :
                finish();
                return true;
            default :
                return super.onOptionsItemSelected(item);
        }
    }

    private void registerLogoutReceiver() {
        receiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        IntentFilter intentFilter = new IntentFilter("com.radiopirate.android.ACTION_LOGOUT");
        registerReceiver(receiver, intentFilter);
    }

    private void updateBufferSummary(String buffer) {
        Preference qualityPref = findPreference(RPPreferences.PREF_BUFFER);

        Resources res = getResources();
        String bufferSummary = "";
        if (buffer.equals(res.getStringArray(R.array.Buffer_entryValues)[0])) {
            bufferSummary = res.getStringArray(R.array.Buffer_entries)[0];
        } else if (buffer.equals(res.getStringArray(R.array.Buffer_entryValues)[1])) {
            bufferSummary = res.getStringArray(R.array.Buffer_entries)[1];
        } else if (buffer.equals(res.getStringArray(R.array.Buffer_entryValues)[2])) {
            bufferSummary = res.getStringArray(R.array.Buffer_entries)[2];
        }

        qualityPref.setSummary(bufferSummary);
    }
}
