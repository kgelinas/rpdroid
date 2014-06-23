package com.radiopirate.lib.service;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.radiopirate.lib.model.PodcastOpenHelper;
import com.radiopirate.lib.model.PodcastSource;

public class DownloadManager extends AsyncTask<String, Integer, Boolean> {
    // TODO: catch disk full event save on sdcard maybe is available?
    private static final String TAG = "RadioPirate";
    private static final int MAX_BUFFER_SIZE = 1024; // 1kb
    private Context context;
    private DownloadProgress progress;
    private String publishDate;
    private DownloadManagerHandler handler;
    private boolean autoPlay;
    private boolean autoDownloaded;

    interface DownloadManagerHandler {
        void onDownloadProgress(DownloadProgress progress);
        void onDownloadCompleted(DownloadProgress progress, boolean autoPlay, boolean autoDownloaded);
    }

    public enum DownloadStatus {
        DOWNLOADING, COMPLETED, FAILED
    }

    public class DownloadProgress implements Serializable {

        private static final long serialVersionUID = 1L;
        private String title;
        private DownloadStatus status;
        private String localPath;
        private int progress;
        private PodcastSource source;

        public DownloadProgress(String title, PodcastSource source) {
            status = DownloadStatus.DOWNLOADING;
            progress = 0;
            this.title = title;
            localPath = "";
            this.source = source;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public int getProgress() {
            return progress;
        }

        public void setStatus(DownloadStatus status) {
            this.status = status;
        }

        public DownloadStatus getStatus() {
            return status;
        }

        public void setLocalPath(String path) {
            this.localPath = path;
        }

        public String getLocalPath() {
            return localPath;
        }

        public String getTitle() {
            return title;
        }

        public PodcastSource getPodcastSource() {
            return source;
        }

    }

    public DownloadManager(Context context, String title, String publishDate, DownloadManagerHandler handler,
            boolean autoPlay, boolean autoDownloaded, PodcastSource source) {
        this.context = context;
        this.publishDate = publishDate;
        this.handler = handler;
        progress = new DownloadProgress(title, source);
        this.autoPlay = autoPlay;
        this.autoDownloaded = autoDownloaded;
    }

    @Override
    protected Boolean doInBackground(String... values) {
        HttpURLConnection conn = null;
        double fileSize = 0;
        double downloaded = 0;
        progress.setStatus(DownloadStatus.DOWNLOADING);
        int lastProgress = 0;

        try {
            InputStream stream;
            String downloadURL = values[0];
            Log.i(TAG, "DownloadManager - URL:" + downloadURL);
            // TODO: multiple redirect maybe a check for a 200 HTTP status is a better solution
            // downloadURL = RPUtil.getRedirectedURL(downloadURL);
            String filename = values[1];
            Log.i(TAG, "DownloadManager - filename:" + filename);
            FileOutputStream fos = new FileOutputStream(filename);

            conn = (HttpURLConnection) new URL(downloadURL).openConnection();
            Log.i(TAG, "URL redirect: " + downloadURL);
            fileSize = conn.getContentLength();
            OutputStream out = new BufferedOutputStream(fos);

            conn.connect();
            Log.i(TAG, "connection au URL pour le download");
            stream = conn.getInputStream();
            Log.i(TAG, "debut de l'écriture du fichier");
            while (true) {

                byte buffer[];

                if (fileSize - downloaded > MAX_BUFFER_SIZE) {
                    buffer = new byte[MAX_BUFFER_SIZE];
                } else {
                    buffer = new byte[(int) (fileSize - downloaded)];
                }
                int read = stream.read(buffer);

                if (read == -1) {
                    publishProgress(100);
                    break;
                }
                out.write(buffer, 0, read);
                downloaded += read;

                int curProgress = (int) ((downloaded / fileSize) * 100);
                if (curProgress != lastProgress) {
                    progress.setProgress(curProgress);
                    lastProgress = curProgress;
                    publishProgress(curProgress);
                }
            }

            PodcastOpenHelper database = new PodcastOpenHelper(context);
            database.setLocalPath(progress.getTitle(), filename, publishDate, progress.getPodcastSource());

            progress.setLocalPath(filename);
            progress.setStatus(DownloadStatus.COMPLETED);
            Log.i(TAG, "Download completed - title:" + progress.getTitle());
            out.close();
            return Boolean.TRUE;
        } catch (Exception e) {
            Log.e(TAG, "DownloadManager - Exception", e);
            progress.setStatus(DownloadStatus.FAILED);
            return Boolean.FALSE;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... changed) {
        handler.onDownloadProgress(progress);
    }

    @Override
    protected void onPostExecute(Boolean downloaded) {
        handler.onDownloadCompleted(progress, autoPlay, autoDownloaded);
    }
}
