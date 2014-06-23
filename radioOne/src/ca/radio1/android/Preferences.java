package ca.radio1.android;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.HoneycombHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class Preferences extends PreferenceActivity {

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Resources res = getResources();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String buffer = preferences.getString(RadioOnePreferences.PREF_BUFFER,
                res.getStringArray(R.array.Buffer_entryValues)[0]);
        updateBufferSummary(buffer);

        Preference bufferPref = findPreference(RadioOnePreferences.PREF_BUFFER);
        bufferPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                updateBufferSummary((String) newValue);
                return true;
            }
        });
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

    private void updateBufferSummary(String buffer) {
        Preference qualityPref = findPreference(RadioOnePreferences.PREF_BUFFER);

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
