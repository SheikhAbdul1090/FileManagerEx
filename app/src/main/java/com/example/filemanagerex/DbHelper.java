package com.example.filemanagerex;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {

    public final static String TABLE_NAME = "file_storage";
    public final static String COLUMN_PATH = "path";
    public final static String COLUMN_POSITION = "position";
    private static final String DATABASE_NAME = "storage.db";
    private static final int DATABASE_VERSION = 1;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a String that contains the SQL statement to create the pets table
        String SQL_CREATE_STORAGE_TABLE = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_PATH + " TEXT PRIMARY KEY, "
                + COLUMN_POSITION + " TEXT NOT NULL);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_STORAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // The database is still at version 1, so there's nothing to do be done here.
    }
}
