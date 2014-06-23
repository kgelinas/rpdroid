package com.radiopirate.android.database;

import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.radiopirate.android.fragments.ChannelEntry;

public class FavoriteOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "RP_favorite";
    private static final String FAVORITE_TABLE_NAME = "favorite";
    private static final String KEY_CHANNEL_ID = "channelId";
    private static final String KEY_IS_FAVORITE = "isFavorite";
    private static final String FAVORITE_TABLE_CREATE = "CREATE TABLE " + FAVORITE_TABLE_NAME + " (" + BaseColumns._ID
            + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_CHANNEL_ID + " INTEGER, " + KEY_IS_FAVORITE + " INTEGER);";

    private static Object lock = new Object();

    public FavoriteOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(FAVORITE_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
        // Implement if the schema needs to change
    }

    public void updateFavorite(int channelId, boolean isFavorite) {

        synchronized (lock) {
            SQLiteDatabase database = getWritableDatabase();
            Cursor cursor = database.query(FAVORITE_TABLE_NAME, new String[]{KEY_CHANNEL_ID}, KEY_CHANNEL_ID + "="
                    + channelId, null, null, null, null);

            try {
                ContentValues values = new ContentValues();
                values.put(KEY_CHANNEL_ID, channelId);
                values.put(KEY_IS_FAVORITE, isFavorite ? 1 : 0);

                if (cursor.getCount() > 0) {
                    database.update(FAVORITE_TABLE_NAME, values, KEY_CHANNEL_ID + "=" + channelId, null);
                } else {
                    database.insert(FAVORITE_TABLE_NAME, null, values);
                }
            } finally {
                cursor.close();
                database.close();
            }
        }
    }

    public boolean hasFavorites() {
        boolean found = false;
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();

            Cursor cursor = database.query(FAVORITE_TABLE_NAME, new String[]{KEY_IS_FAVORITE}, KEY_IS_FAVORITE + "=1",
                    null, null, null, null);

            try {
                if (cursor.getCount() > 0) {
                    found = true;
                }
            } finally {
                cursor.close();
                database.close();
            }
        }

        return found;
    }

    public void loadFavorites(List<ChannelEntry> channels) {
        synchronized (lock) {
            SQLiteDatabase database = getReadableDatabase();
            try {
                for (ChannelEntry channel : channels) {
                    Cursor cursor = database.query(FAVORITE_TABLE_NAME, new String[]{KEY_IS_FAVORITE}, KEY_CHANNEL_ID
                            + "=" + channel.getChannelId(), null, null, null, null);
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        int index = cursor.getColumnIndex(KEY_IS_FAVORITE);
                        boolean favorite = cursor.getInt(index) != 0;
                        channel.setFavorite(favorite);
                    }

                    cursor.close();
                }

            } finally {
                database.close();
            }
        }
    }
}
