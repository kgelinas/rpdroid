package com.radiopirate.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.bugsense.trace.BugSenseHandler;
import com.radiopirate.android.RPAPI.LoginResult;
import com.radiopirate.android.pre_fragment.MainTabActivity;
import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class StartUp extends Activity {
    private SharedPreferences preferences;

    private ProgressDialog dialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startup);

        BugSenseHandler.setup(this, RPSettings.BUG_SENSE_API_KEY);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String user = preferences.getString(RPPreferences.PREF_USERNAME, "");
        final String pwd = preferences.getString(RPPreferences.PREF_PASSWORD, "");

        dialog = ProgressDialog.show(this, "", getResources().getString(R.string.logging_in), true);

        new AsyncTask<String, Void, Integer>() {

            @Override
            protected Integer doInBackground(String... params) {
                String user = params[0];
                String pwd = params[1];
                RPSettings setting = (RPSettings) RPSettings.getInstance();

                if (!user.equals("")) {
                    LoginResult result = setting.fetchConfig(user, pwd);

                    if (result == LoginResult.BAD_CREDENTIALS) {
                        LoginResult resultGuest = setting.fetchGuestConfig();
                        if (resultGuest == LoginResult.NO_NETWORK) {
                            // Handle unlikely event of double failure
                            result = resultGuest;
                        }
                    }

                    return result.ordinal();
                } else {
                    return setting.fetchGuestConfig().ordinal();
                }

            }

            @Override
            protected void onPostExecute(Integer loginResult) {
                LoginResult result = LoginResult.int2e(((int) loginResult));

                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }

                if (result == LoginResult.BAD_CREDENTIALS) {
                    // We fetched the guest credentials, and now we must show the error from the main thread
                    Toast.makeText(StartUp.this, R.string.login_wrong_credential, Toast.LENGTH_LONG).show();
                    result = LoginResult.LOGGED_IN;
                }

                onLoggedIn(result);
            }
        }.execute(user, pwd);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void onLoggedIn(LoginResult result) {

        if (result == LoginResult.BAD_CREDENTIALS) {
            Toast.makeText(this, R.string.login_wrong_credential, Toast.LENGTH_LONG).show();
            startLogin();
            return;
        } else if (result == LoginResult.NO_NETWORK) {
            boolean found = false;

            PodcastOpenHelper database = new PodcastOpenHelper(this);
            found = database.hasCachedPodcasts(PodcastSource.RP) || database.hasCachedPodcasts(PodcastSource.Marto);

            if (found) {
                Toast.makeText(this, R.string.login_offline, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.login_no_network, Toast.LENGTH_LONG).show();
                startLogin();
                return;
            }
        }

        startMain();
    }

    private void startMain() {
        Intent intent;
        if (CupcakeHelper.isCupcake() || VersionCheck.isSDKSmallerThanDunut()) {
            // 1.5 does not support fragments, and Build.VERSION.SDK_INT
            // 1.6 does not support then properly for now
            intent = new Intent(this, MainTabActivity.class);
        } else {
            intent = new Intent(this, Main.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void startLogin() {
        Intent intent = new Intent(this, Login.class);

        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
