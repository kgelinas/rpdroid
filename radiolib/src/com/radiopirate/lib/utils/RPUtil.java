package com.radiopirate.lib.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.util.Log;

public class RPUtil {
    private static final String TAG = "RP Util";

    /*
     * public static String getRedirectedURL(String fileURL) { String prefix = "http://podcast.radiopirate.com"; if
     * (!CupcakeHelper.isCupcake() && VersionCheck.isFroyoOrGreater() || (fileURL.startsWith(prefix))) { // if (true) {
     * URL url = null; HttpURLConnection cn = null; try { url = new URL(fileURL); } catch (MalformedURLException e) {
     * Log.e(TAG, "getRedirectedURL - MalformedURLException", e); return fileURL; }
     * 
     * try { cn = (HttpURLConnection) url.openConnection(); } catch (Exception e) { Log.e(TAG,
     * "getRedirectedURL - Exception", e); }
     * 
     * cn.setInstanceFollowRedirects(false); String redirectURL = cn.getHeaderField("Location"); if (redirectURL !=
     * null) { fileURL = redirectURL; Log.d(TAG, "Converted url to: " + fileURL); } // Fix for new beta podcast site
     * which use https by default that only SDK >= 10 seem to be able to play String prefixhttps = "https://"; if
     * ((fileURL.startsWith(prefixhttps))) { fileURL = fileURL.replace("https", "http"); Log.d(TAG,
     * "https: stripped new url is " + fileURL);
     * 
     * } }
     * 
     * return fileURL; }
     */
    /**
     * 
     * @param date
     *            Date formated as Thu, 08 Sep 2011 08:00:00 -0500
     * @return
     */
    public static Date parseDate(String date) {

        if (date == null) {
            return new Date(0);
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzzz", Locale.US);
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            Log.e(TAG, "parseDate - ParseException", e);
        }

        return new Date(0);
    }

    public static Date parsePodcastDate(String date) {

        if (date == null) {
            return new Date(0);
        }

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            return simpleDateFormat.parse(date);
        } catch (ParseException e) {
            Log.e(TAG, "parseDate - ParseException", e);
        }

        return new Date(0);
    }

    public static String formatDate(Date date) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss zzzz", Locale.US);
        return simpleDateFormat.format(date);
    }

    public static String trimCarModePodcastTitle(String title) {
        return title.replace("RP-", "").replace("2011-", "").trim();
    }

    public static boolean doesSupportGoogleTV(Context context) {
        if (!CupcakeHelper.isCupcake() && VersionCheck.isGreaterThanEclair()) {
            return EclairHelper.doesSupportGoogleTV(context.getPackageManager());
        }

        return false;
    }
}
