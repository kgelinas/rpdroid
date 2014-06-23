package com.radiopirate.lib.BufferedPlayer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.os.Environment;
import android.util.Log;

public class ShoutcastFile {
    private static final String TAG = "BufferedPlayer";
    private String m_shoutcast_name;
    private int m_bitrate; // kbit/s
    private long m_current_write_pos = 0;
    private boolean m_done = false;
    private File m_file;
    private boolean isInitialized = false;

    public void init(URLConnection connection) {
        m_shoutcast_name = connection.getHeaderField("icy-name");
        if (m_shoutcast_name == null || m_shoutcast_name.length() == 0) {
            m_shoutcast_name = "Default";
        }
        String icyBr = connection.getHeaderField("icy-br");
        if (icyBr != null && icyBr.length() > 0) {
            m_bitrate = Integer.parseInt(connection.getHeaderField("icy-br"));
        } else {
            m_bitrate = 128;
            Log.w(TAG, "ShoutcastFile using default bitrate: " + m_bitrate);
        }

        String contentType = connection.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("audio/mpeg")) {
            Log.w(TAG, "ShoutcastFile unsupported contentType: " + contentType);
        }

        build_file_name();
        isInitialized = true;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    private void build_file_name() {
        String fileName;
        Calendar now = new GregorianCalendar();
        fileName = m_shoutcast_name.replaceAll("[\\/:*?\"<>|]", "_");
        fileName = m_shoutcast_name.replaceAll("!", "");
        fileName += "-" + now.get(Calendar.YEAR) + "_" + (now.get(Calendar.MONTH) + 1) + "_"
                + now.get(Calendar.DAY_OF_MONTH) + "_" + now.get(Calendar.HOUR_OF_DAY) + "." + now.get(Calendar.MINUTE)
                + "." + now.get(Calendar.SECOND) + ".mp3";

        File folder;
        folder = new File(Environment.getExternalStorageDirectory() + "/RadioBuffer");
        emptyDir(folder); // Delete all buffered files
        folder.mkdirs();
        m_file = new File(folder.getAbsolutePath(), fileName);
    }

    public void done() {
        m_done = true;
    }

    public String file_path() {
        return m_file.getAbsolutePath();
    }

    public boolean download(DownloadThread download_thread, URLConnection connection) {

        Log.d(TAG, "ShoutcastFile.download");
        FileOutputStream output = null;

        try {
            InputStream input = connection.getInputStream();
            output = new FileOutputStream(m_file, true);
            byte[] buffer = new byte[1024];
            int numRead;

            while ((numRead = input.read(buffer)) != -1 && !m_done) {
                output.write(buffer, 0, numRead);
                m_current_write_pos += numRead;
            }
        } catch (IOException e) {
            Log.e(TAG, "ShoutcastFile.download IOException", e);
            return false;
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    Log.e(TAG, "ShoutcastFile.download failed to close - IOException", e);
                }
            }
        }

        done();
        return true;
    }
    public boolean isDone() {
        return m_done;
    }

    // Get the buffer position in milliseconds
    public long getBufferedMsec() {
        if (m_current_write_pos == 0)
            return 0;

        return m_current_write_pos / (m_bitrate / 8);
    }

    private static boolean emptyDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        return true;
    }

    // Deletes all files and sub-directories under dir.
    // Returns true if all deletions were successful.
    // If a deletion fails, the method stops attempting to delete and returns false.
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }
}
