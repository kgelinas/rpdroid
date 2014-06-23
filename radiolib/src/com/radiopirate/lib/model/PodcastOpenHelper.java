package com.radiopirate.lib.model;

import java.io.File;
import java.util.Collections;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class PodcastOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "RadioPirate";

    private static final String DATABASE_NAME = "RP_podcast";
    private static final String PODCAST_TABLE_NAME = "podcast";
    private static final String PODCAST_MARTO_TABLE_NAME = "podcastMarto";

    private static final String KEY_TITLE = "title";
    private static final String KEY_LOCAL_PATH = "localPath";
    private static final String KEY_CURRENT_POSITION = "currentPosition";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_PUBLISH_DATE = "pubDate";

    private static final String PODCAST_TABLE_CREATE = "CREATE TABLE " + PODCAST_TABLE_NAME + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_TITLE + " TEXT, " + KEY_LOCAL_PATH + " TEXT, "
            + KEY_CURRENT_POSITION + " INTEGER, " + KEY_DURATION + " INTEGER," + KEY_PUBLISH_DATE + " TEXT);";

    private static final String PODCAST_MARTO_TABLE_CREATE = "CREATE TABLE " + PODCAST_MARTO_TABLE_NAME + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_TITLE + " TEXT, " + KEY_LOCAL_PATH
            + " TEXT, " + KEY_CURRENT_POSITION + " INTEGER, " + KEY_DURATION + " INTEGER," + KEY_PUBLISH_DATE
            + " TEXT);";

    private static Object lock = new Object();

    public PodcastOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(PODCAST_TABLE_CREATE);
        db.execSQL(PODCAST_MARTO_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Implement if the schema needs to change

        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE " + PODCAST_TABLE_NAME + " ADD COLUMN " + KEY_PUBLISH_DATE + " TEXT");
        } else if (oldVersion == 2 && newVersion == 3) {
            db.execSQL(PODCAST_MARTO_TABLE_CREATE);
        }
    }

    public void setLocalPath(String title, String localPath, String pubDate, PodcastSource source) {

        synchronized (lock) {
            SQLiteDatabase database = getWritableDatabase();
            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_TITLE}, KEY_TITLE + "=\""
                    + title + "\"", null, null, null, null);

            try {
                ContentValues values = new ContentValues();
                values.put(KEY_TITLE, title);
                values.put(KEY_LOCAL_PATH, localPath);
                values.put(KEY_PUBLISH_DATE, pubDate);

                if (cursor.getCount() > 0) {
                    database.update(getPodcastTableName(source), values, KEY_TITLE + "=\"" + title + "\"", null);
                } else {
                    database.insert(getPodcastTableName(source), null, values);
                }
            } finally {
                cursor.close();
                database.close();
            }
        }
    }

    public void deleteEntry(String title, PodcastSource source) {

        synchronized (lock) {
            SQLiteDatabase database = getWritableDatabase();

            try {
                database.delete(getPodcastTableName(source), KEY_TITLE + "=\"" + title + "\"", null);
            } finally {
                database.close();
            }
        }
    }

    public void setPubDate(String title, String pubDate, PodcastSource source) {

        synchronized (lock) {
            SQLiteDatabase database = getWritableDatabase();
            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_TITLE}, KEY_TITLE + "=\""
                    + title + "\"", null, null, null, null);

            ContentValues values = new ContentValues();
            values.put(KEY_PUBLISH_DATE, pubDate);

            try {
                if (cursor.getCount() > 0) {
                    database.update(getPodcastTableName(source), values, KEY_TITLE + "=\"" + title + "\"", null);
                }
            } finally {
                cursor.close();
                database.close();
            }
        }
    }

    public void updateCurrentPosition(String title, int currentPosition, int duration, PodcastSource source) {

        synchronized (lock) {
            SQLiteDatabase database = getWritableDatabase();

            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_TITLE}, KEY_TITLE + "=\""
                    + title + "\"", null, null, null, null);

            ContentValues values = new ContentValues();
            values.put(KEY_TITLE, title);
            values.put(KEY_CURRENT_POSITION, currentPosition);
            values.put(KEY_DURATION, duration);

            try {
                if (cursor.getCount() > 0) {
                    database.update(getPodcastTableName(source), values, KEY_TITLE + "=\"" + title + "\"", null);
                } else {
                    database.insert(getPodcastTableName(source), null, values);
                }
            } finally {
                cursor.close();
                database.close();
            }
        }
    }

    public String getLocalPath(String title) {
        String value = "";
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();

            Cursor cursorMarto = null;
            Cursor cursor = database.query(getPodcastTableName(PodcastSource.RP), new String[]{KEY_LOCAL_PATH},
                    KEY_TITLE + "=\"" + title + "\"", null, null, null, null);

            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex(KEY_LOCAL_PATH);
                    value = cursor.getString(index);

                    if (value == null) {
                        value = "";
                    }
                } else {
                    cursorMarto = database.query(getPodcastTableName(PodcastSource.Marto),
                            new String[]{KEY_LOCAL_PATH}, KEY_TITLE + "=\"" + title + "\"", null, null, null, null);

                    if (cursorMarto.getCount() > 0) {
                        cursorMarto.moveToFirst();
                        int index = cursorMarto.getColumnIndex(KEY_LOCAL_PATH);
                        value = cursorMarto.getString(index);

                        if (value == null) {
                            value = "";
                        }
                    }
                }
            } finally {
                if (cursorMarto != null) {
                    cursorMarto.close();
                }
                cursor.close();
                database.close();
            }
        }

        return value;
    }

    public int getCurrentPosition(String title) {
        int value = 0;
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();

            Cursor cursorMarto = null;
            Cursor cursor = database.query(getPodcastTableName(PodcastSource.RP), new String[]{KEY_CURRENT_POSITION},
                    KEY_TITLE + "=\"" + title + "\"", null, null, null, null);

            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex(KEY_CURRENT_POSITION);
                    value = cursor.getInt(index);
                } else {
                    cursorMarto = database.query(getPodcastTableName(PodcastSource.Marto),
                            new String[]{KEY_CURRENT_POSITION}, KEY_TITLE + "=\"" + title + "\"", null, null, null,
                            null);
                    if (cursorMarto.getCount() > 0) {
                        cursorMarto.moveToFirst();
                        int index = cursorMarto.getColumnIndex(KEY_CURRENT_POSITION);
                        value = cursorMarto.getInt(index);
                    }
                }
            } finally {
                if (cursorMarto != null) {
                    cursorMarto.close();
                }

                cursor.close();
                database.close();
            }
        }

        return value;
    }

    public int getDuration(String title, PodcastSource source) {
        int value = 0;
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();

            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_DURATION}, KEY_TITLE + "=\""
                    + title + "\"", null, null, null, null);

            try {
                if (cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    int index = cursor.getColumnIndex(KEY_DURATION);
                    value = cursor.getInt(index);
                }
            } finally {
                cursor.close();
                database.close();
            }
        }
        return value;
    }

    public void loadCachedData(List<PodcastEntry> podcasts, PodcastSource source) {
        synchronized (lock) {

            Log.d(TAG, "loadCachedData IN");

            SQLiteDatabase database = getReadableDatabase();
            // Query all cached podcasts
            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_TITLE, KEY_LOCAL_PATH,
                    KEY_CURRENT_POSITION, KEY_DURATION, KEY_PUBLISH_DATE}, null, null, null, null, null);

            try {
                if (cursor.getCount() > 0) {
                    String localPath;
                    String pubDate;
                    String title;
                    int index;
                    int currentPosition;
                    int duration;

                    cursor.moveToFirst();
                    do {
                        index = cursor.getColumnIndex(KEY_TITLE);
                        title = cursor.getString(index);

                        index = cursor.getColumnIndex(KEY_LOCAL_PATH);
                        localPath = cursor.getString(index);
                        if (localPath == null) {
                            localPath = "";
                        }

                        index = cursor.getColumnIndex(KEY_CURRENT_POSITION);
                        currentPosition = cursor.getInt(index);

                        index = cursor.getColumnIndex(KEY_DURATION);
                        duration = cursor.getInt(index);

                        index = cursor.getColumnIndex(KEY_PUBLISH_DATE);
                        pubDate = cursor.getString(index);

                        boolean found = false;
                        for (PodcastEntry podcast : podcasts) {
                            // Add the cached data to the data from the RSS feed
                            if (podcast.getPodcastTitle().equals(title)) {
                                podcast.setCachedValue(localPath, currentPosition, duration);
                                setPubDate(title, podcast.getPubDateStr(), source);
                                found = true;
                                break;
                            }
                        }

                        if (!found && localPath.length() > 0) {

                            if ((new File(localPath)).exists()) {
                                // The podcast is not on the RSS, but we have it cached so display it
                                PodcastEntry cachedPodcast = new PodcastEntry(title, localPath, pubDate, source);
                                cachedPodcast.setCachedValue(localPath, currentPosition, duration);
                                cachedPodcast.setLocalOnly(true);
                                podcasts.add(cachedPodcast);
                            } else {
                                Log.w(TAG, "Delete entry for podcast title:" + title);
                                deleteEntry(title, source);
                            }
                        } else if (!found && podcasts.size() > 0) {
                            // Remove the old rows, the podcast is not part of the feed anymore and the user did not
                            // download it
                            Log.w(TAG, "Data about unpublished podcast title:" + title);
                            deleteEntry(title, source);
                        }

                    } while (cursor.moveToNext());
                }

            } finally {
                cursor.close();
                database.close();
            }
        }

        Log.d(TAG, "loadCachedData Sort");
        Collections.sort(podcasts); // Sort by date
        Log.d(TAG, "loadCachedData OUT");
    }

    public boolean hasCachedPodcasts(PodcastSource source) {
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();
            Cursor cursor = database.query(getPodcastTableName(source), new String[]{KEY_TITLE}, KEY_LOCAL_PATH
                    + "!=\"\"", null, null, null, null);

            boolean found = false;
            try {
                if (cursor.getCount() > 0) {
                    found = true;
                }
            } finally {
                cursor.close();
                database.close();
            }

            return found;
        }
    }

    private String getPodcastTableName(PodcastSource source) {
        return source == PodcastSource.RP ? PODCAST_TABLE_NAME : PODCAST_MARTO_TABLE_NAME;
    }
}
