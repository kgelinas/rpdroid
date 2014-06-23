package com.radiopirate.lib.BufferedPlayer;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

import android.util.Log;

public class DownloadThread implements Runnable {
    private static final String TAG = "BufferedPlayer";
    private static final int MAX_RETRY = 5;
    private URL m_url;
    private ShoutcastFile m_shoutcast_file = null;
    private Boolean failedDownload = false;

    public DownloadThread(URL url, ShoutcastFile shoutcastFile) {
        m_url = url;
        m_shoutcast_file = shoutcastFile;
    }

    public boolean didDownloadFail() {
        return failedDownload;
    }

    @Override
    public void run() {
        boolean failed = false;
        failedDownload = false;

        try {
            int tryCount = 0;
            do {
                URLConnection connection = m_url.openConnection();
                connection.connect();
                if (!m_shoutcast_file.isInitialized()) {
                    m_shoutcast_file.init(connection);
                }
                failed = !m_shoutcast_file.download(this, connection);
                tryCount++;
            } while (failed && tryCount < MAX_RETRY);

            if (failed && tryCount >= MAX_RETRY) {
                m_shoutcast_file.done();
            }

        } catch (IOException e) {
            Log.e(TAG, "DownloadThread.Run IOException", e);
            failed = true;
            m_shoutcast_file.done();
        }

        failedDownload = failed;
    }
}
