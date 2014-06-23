package com.radiopirate.lib.utils;

import android.app.ActionBar;
import android.app.Activity;

/**
 * Helper class to wrap calls to Honeycomb API so older versions don't see them
 * 
 */
public class HoneycombHelper {

    public static void setDisplayHomeAsUpEnabled(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    public static void hideActionBar(Activity activity) {
        ActionBar actionBar = activity.getActionBar();
        actionBar.hide();
    }
}
