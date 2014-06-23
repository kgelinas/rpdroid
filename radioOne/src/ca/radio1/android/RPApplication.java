package ca.radio1.android;

import android.app.Application;
import ca.radio1.android.delegate.RadioOneResource;

import com.radiopirate.lib.delegates.RadioSettingsDelegate;
import com.radiopirate.lib.delegates.ResourceDelegate;

/*
 * Object that is created when the app starts and destroyed on exit
 * Used to be able to create global objects that customize functionality needed by the radio library
 */
public class RPApplication extends Application {

    /*
     * Called when the application is starting, before any other application objects have been created.
     * 
     * @see android.app.Application#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        RadioSettingsDelegate.setDelegate(new RadioOneSettings(this));
        ResourceDelegate.setDelegate(new RadioOneResource());
    }
}
