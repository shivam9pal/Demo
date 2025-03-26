package com.example.smshandlerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SMS.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "messages";
    private static final String COL_ID = "id";
    private static final String COL_SENDER = "sender";
    private static final String COL_MESSAGE = "message";
    private static final String COL_TYPE = "type"; // New column: "received" or "sent"

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SENDER + " TEXT, " +
                COL_MESSAGE + " TEXT, " +
                COL_TYPE + " TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert received SMS
    public void insertMessage(String sender, String message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SENDER, sender);
        values.put(COL_MESSAGE, message);
        values.put(COL_TYPE, "received");
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Insert sent response
    public void insertResponse(String sender, String response) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SENDER, sender);
        values.put(COL_MESSAGE, response);
        values.put(COL_TYPE, "sent");
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Get received messages
    public ArrayList<String> getReceivedMessages() {
        ArrayList<String> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_SENDER, COL_MESSAGE},
                COL_TYPE + "=?", new String[]{"received"}, null, null, COL_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String sender = cursor.getString(0);
                String message = cursor.getString(1);
                messages.add(sender + ": " + message);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return messages;
    }

    // Get sent responses
    public ArrayList<String> getSentResponses() {
        ArrayList<String> responses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_SENDER, COL_MESSAGE},
                COL_TYPE + "=?", new String[]{"sent"}, null, null, COL_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String sender = cursor.getString(0);
                String response = cursor.getString(1);
                responses.add("To " + sender + ": " + response);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return responses;
    }

    // Get response for replying (unchanged)
    public String getResponse(String sender) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{COL_MESSAGE},
                COL_SENDER + "=? AND " + COL_TYPE + "=?", new String[]{sender, "received"},
                null, null, COL_ID + " DESC", "1");
        if (cursor != null && cursor.moveToFirst()) {
            String response = cursor.getString(0);
            cursor.close();
            return "Response: " + response;
        }
        return "No previous message found";
    }
}