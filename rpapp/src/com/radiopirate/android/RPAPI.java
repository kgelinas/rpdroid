package com.radiopirate.android;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.util.Log;

import com.radiopirate.lib.model.PodcastEntry;
import com.radiopirate.lib.model.PodcastSource;
import com.radiopirate.lib.utils.RPUtil;

public class RPAPI {
    private static final String TAG = "RadioPirate";

    private static final long DATA_REFRESH_DELAY = 5 * 60 * 1000; // 5 minutes
    private JSONObject jObject;
    private String siteURL;
    private String apiURL;
    private String username = "";
    private String password = "";
    private long lastFetchTime = 0;

    enum LoginResult {
        LOGGED_IN, BAD_CREDENTIALS, NO_NETWORK, UNKNOWN;

        public static LoginResult int2e(int i) {
            for (LoginResult current : values()) {
                if (current.ordinal() == i) {
                    return current;
                }
            }
            return UNKNOWN;
        }
    }

    enum FetchResponse {
        OK, Unauthorized, Offline
    }

    enum StreamFormat {
        MP3, ACC;

        public static String toStr(StreamFormat format) {
            switch (format) {
                case MP3 :
                    return "mp3";
                case ACC :
                    return "m4a";
            }

            return "";
        }
    }

    RPAPI(String siteURL, String apiURL) {
        this.siteURL = siteURL;
        this.apiURL = apiURL;
    }

    public LoginResult fetchConfig(final String username, final String password) {
        try {
            this.username = username;
            this.password = password;
            lastFetchTime = System.currentTimeMillis();

            // HttpURLConnection does not handle authentication on Android 2.1
            // so use HttpClient to support all platforms
            DefaultHttpClient client = new DefaultHttpClient();
            Credentials credentials = new UsernamePasswordCredentials(username, password);
            client.getCredentialsProvider().setCredentials(new AuthScope(null, -1), credentials);

            HttpGet request = new HttpGet();
            request.setURI(new URI(apiURL + "auth/"));
            HttpResponse response = client.execute(request);

            return fetchConfig(response);
        } catch (MalformedURLException e) {
            Log.e(TAG, "fetchConfig - MalformedURLException", e);
            return LoginResult.NO_NETWORK;
        } catch (IOException e) {
            Log.e(TAG, "fetchConfig - IOException", e);
            return LoginResult.NO_NETWORK;
        } catch (URISyntaxException e) {
            Log.e(TAG, "fetchConfig - URISyntaxException", e);
            return LoginResult.NO_NETWORK;
        }
    }

    public LoginResult fetchConfig() {
        this.username = "";
        this.password = "";
        lastFetchTime = System.currentTimeMillis();

        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.setURI(new URI(apiURL));
            HttpResponse response = client.execute(request);

            return fetchConfig(response);
        } catch (MalformedURLException e) {
            Log.e(TAG, "fetchConfig - MalformedURLException", e);
            return LoginResult.NO_NETWORK;
        } catch (IOException e) {
            Log.e(TAG, "fetchConfig - IOException", e);
            return LoginResult.NO_NETWORK;
        } catch (URISyntaxException e) {
            Log.e(TAG, "fetchConfig - URISyntaxException", e);
            return LoginResult.NO_NETWORK;
        }
    }

    public LoginResult fetchConfig(HttpResponse response) {

        try {
            // Use Apache HTTP client instead of HttpURLConnection since it does not work on 2.1 with 401 responses.

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                InputStream responseStream = response.getEntity().getContent();
                BufferedReader r = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder jString = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    jString.append(line);
                }

                jObject = new JSONObject(jString.toString());
            } else {
                Log.e(TAG, "Fetch config failed with response:" + responseCode);
                return LoginResult.BAD_CREDENTIALS;
            }
        } catch (IOException e) {
            Log.e(TAG, "fetchConfig - IOException", e);
            return LoginResult.NO_NETWORK;
        } catch (JSONException e) {
            Log.e(TAG, "fetchConfig - JSONException", e);
            return LoginResult.NO_NETWORK;
        }

        return LoginResult.LOGGED_IN;
    }

    private void refreshData() {

        long curTime = System.currentTimeMillis();

        if (curTime - lastFetchTime > DATA_REFRESH_DELAY) {
            if (username != null && username.length() > 0) {
                fetchConfig(username, password);
            } else {
                fetchConfig();
            }

            lastFetchTime = curTime;
        }
    }

    public int getStreamCount() {
        int count = getAPIStreamCount();

        if (RPSettings.ENABLE_EXTRA_CHANNELS) {
            count += ExtraChannels.getRadioCount();
        }

        return count;
    }

    public int getBaseStreamCount() {
        int count = getAPIStreamCount();
        return count;
    }

    private int getAPIStreamCount() {
        int count = 0;

        if (jObject == null) {
            return 0;
        }

        JSONArray streamsObject;
        try {
            streamsObject = jObject.getJSONArray("streams");
            count = streamsObject.length();
        } catch (JSONException e) {
            Log.e(TAG, "getAPIStreamCount - JSONException", e);
        }

        return count;
    }

    public String getRemainingDays() {
        String remaining = "";

        if (jObject == null) {
            return remaining;
        }

        try {
            remaining = jObject.getString("remaining");
        } catch (JSONException e) {
            Log.e(TAG, "getStreamURL - JSONException", e);
        }

        return remaining;
    }

    public String getStreamURL(int channelId) {
        String streamURL = "";
        JSONArray streamsObject;

        if (jObject == null) {
            return streamURL;
        }

        if (RPSettings.ENABLE_EXTRA_CHANNELS) {
            int apiStreamCount = getAPIStreamCount();
            if (channelId >= apiStreamCount) {
                return ExtraChannels.getChannelURL(channelId - apiStreamCount + 1);
            }
        }

        try {
            streamsObject = jObject.getJSONArray("streams");
            streamURL = siteURL + streamsObject.getJSONObject(channelId).getString("url");
        } catch (JSONException e) {
            Log.e(TAG, "getStreamURL - JSONException", e);
        }

        return streamURL;
    }

    public String getStreamTitle(Resources res, int channelId) {
        String value = "";
        JSONArray streamsObject;

        if (jObject == null) {
            return value;
        }

        if (RPSettings.ENABLE_EXTRA_CHANNELS) {
            int apiStreamCount = getAPIStreamCount();
            if (channelId >= apiStreamCount) {
                return ExtraChannels.getRPChannelName(res, channelId - apiStreamCount + 1);
            }
        }

        try {
            streamsObject = jObject.getJSONArray("streams");
            value = streamsObject.getJSONObject(channelId).getString("title");

            if (value.indexOf('(') != -1) {
                value = value.substring(0, value.indexOf('(')).trim();
            }

        } catch (JSONException e) {
            Log.e(TAG, "getStreamTitle - JSONException", e);
        }

        return value;
    }

    public String getStreamDescription(Resources res, int channelId) {
        String value = "";
        JSONArray streamsObject;

        if (jObject == null) {
            return value;
        }

        if (RPSettings.ENABLE_EXTRA_CHANNELS) {
            int apiStreamCount = getAPIStreamCount();
            if (channelId >= apiStreamCount) {
                return ExtraChannels.getDescription(res, channelId - apiStreamCount + 1);
            }
        }

        try {
            streamsObject = jObject.getJSONArray("streams");
            value = streamsObject.getJSONObject(channelId).getString("title").trim();

            if (value.indexOf('(') != -1) {
                value = value.substring(value.indexOf('(') + 1, value.length() - 1).trim();
            } else {
                value = "";
            }
        } catch (JSONException e) {
            Log.e(TAG, "getStreamDescription - JSONException", e);
        }

        return value;
    }

    public StreamFormat getStreamFormat(int channelId) {
        String format = "";
        JSONArray streamsObject;

        if (jObject == null) {
            return StreamFormat.MP3;
        }

        if (RPSettings.ENABLE_EXTRA_CHANNELS) {
            int apiStreamCount = getAPIStreamCount();
            if (channelId >= apiStreamCount) {
                return ExtraChannels.isACC(channelId - apiStreamCount + 1) ? StreamFormat.ACC : StreamFormat.MP3;
            }
        }

        try {
            streamsObject = jObject.getJSONArray("streams");
            format = streamsObject.getJSONObject(channelId).getString("format");
        } catch (JSONException e) {
            Log.e(TAG, "getStreamFormat - JSONException", e);
        }

        return format.equalsIgnoreCase("mp3") ? StreamFormat.MP3 : StreamFormat.ACC;
    }

    public ArrayList<PodcastEntry> getPodcasts(StreamFormat format, PodcastSource source) {
        ArrayList<PodcastEntry> podcasts = new ArrayList<PodcastEntry>();

        refreshData(); // Refresh the data if it is too old
        JSONArray podcastObject = getPodcastJSON(format, source);

        try {
            for (int i = 0; podcastObject != null && i < podcastObject.length(); i++) {
                String podcastTitle = "";
                String podcastURL = "";
                String pubDate = "";
                JSONObject obj = podcastObject.getJSONObject(i);
                Iterator iter = obj.keys();
                if (iter.hasNext()) {
                    String key = (String) iter.next();
                    String value = obj.getString(key);

                    podcastTitle = key;
                    podcastURL = value;
                    pubDate = RPUtil.formatDate(RPUtil.parsePodcastDate(podcastTitle));
                }

                podcasts.add(new PodcastEntry(podcastTitle, podcastURL, pubDate, source));
            }
        } catch (JSONException e) {
            Log.e(TAG, "getPodcasts - JSONException", e);
        }

        return podcasts;
    }

    private JSONArray getPodcastJSON(StreamFormat format, PodcastSource source) {

        JSONArray podcastObject;

        if (jObject == null) {
            Log.e(TAG, "getPodcastJSON jObject is NULL;");
            return null;
        }

        try {
            podcastObject = jObject.getJSONArray("podcasts");

            for (int i = 0; podcastObject != null && i < podcastObject.length(); i++) {
                if ((source == PodcastSource.Marto) != podcastObject.getJSONObject(i).getString("title")
                        .equalsIgnoreCase("Marto 4:11")) {
                    continue;
                }
                String formatStr = podcastObject.getJSONObject(i).getString("format");
                if (formatStr != null && formatStr.equalsIgnoreCase(StreamFormat.toStr(format))) {
                    return podcastObject.getJSONObject(i).getJSONArray("files");
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "getPodcastJSON - JSONException", e);
        }

        return null;
    }
}
