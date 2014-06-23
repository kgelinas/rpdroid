package com.radiopirate.android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.radiopirate.android.RPAPI.LoginResult;
import com.radiopirate.android.pre_fragment.MainTabActivity;
import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.utils.CupcakeHelper;
import com.radiopirate.lib.utils.VersionCheck;

public class Login extends Activity {
    private SharedPreferences preferences;

    private ProgressDialog dialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        final EditText userID = (EditText) findViewById(R.id.etLogin);
        final EditText password = (EditText) findViewById(R.id.etPassword);
        final Button login = (Button) findViewById(R.id.bLogin);
        final Button subscribe = (Button) findViewById(R.id.bSubscribe);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        final String user = preferences.getString(RPPreferences.PREF_USERNAME, "");
        final String pwd = preferences.getString(RPPreferences.PREF_PASSWORD, "");

        // If we remembered the credentials, auto login
        if (!user.equals("")) {
            userID.setText(user);
            password.setText(pwd);

            dialog = ProgressDialog.show(this, "", getResources().getString(R.string.logging_in), true);

            new AsyncTask<String, Void, Integer>() {

                @Override
                protected Integer doInBackground(String... params) {
                    String user = params[0];
                    String pwd = params[1];

                    RPSettings setting = (RPSettings) RPSettings.getInstance();
                    return setting.fetchConfig(user, pwd).ordinal();
                }

                @Override
                protected void onPostExecute(Integer loginResult) {
                    LoginResult result = LoginResult.int2e(((int) loginResult));

                    if (dialog != null) {
                        dialog.dismiss();
                        dialog = null;
                    }

                    onLoggedIn(result);

                }
            }.execute(user, pwd);
        }

        login.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {
                final String user = userID.getText().toString().trim();
                final String pwd = password.getText().toString().trim();

                dialog = ProgressDialog.show(Login.this, "", getResources().getString(R.string.logging_in), true);

                new AsyncTask<String, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(String... params) {
                        String user = params[0];
                        String pwd = params[1];

                        preferences.edit().putString(RPPreferences.PREF_USERNAME, user)
                                .putString(RPPreferences.PREF_PASSWORD, pwd).commit();

                        RPSettings setting = (RPSettings) RPSettings.getInstance();
                        return setting.fetchConfig(user, pwd).ordinal();
                    }

                    @Override
                    protected void onPostExecute(Integer loginResult) {
                        LoginResult result = LoginResult.int2e(((int) loginResult));

                        if (dialog != null) {
                            dialog.dismiss();
                            dialog = null;
                        }

                        onLoggedIn(result);

                    }
                }.execute(user, pwd);
            }

        });

        subscribe.setOnClickListener(new OnClickListener() {

            public void onClick(View view) {

                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(RPSettings.getSiteURL()
                        + "/store/category/abonnements/"));
                startActivity(browserIntent);
            }
        });
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
            return;
        } else if (result == LoginResult.NO_NETWORK) {
            boolean found = false;

            PodcastOpenHelper database = new PodcastOpenHelper(this);
            found = database.hasCachedPodcasts(PodcastSource.RP) || database.hasCachedPodcasts(PodcastSource.Marto);

            if (found) {
                Toast.makeText(this, R.string.login_offline, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.login_no_network, Toast.LENGTH_LONG).show();
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
}
