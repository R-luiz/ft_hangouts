package com.example.ft_hangouts.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ft_hangouts.model.Message;

import java.util.ArrayList;
import java.util.List;

public class MessageDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "messages.db";
    private static final int DATABASE_VERSION = 1;

    // Table name
    public static final String TABLE_MESSAGES = "messages";

    // Column names
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_PHONE_NUMBER = "phone_number";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_IS_SENT = "is_sent";
    public static final String COLUMN_READ = "read";

    // Create table query
    private static final String SQL_CREATE_MESSAGES_TABLE =
            "CREATE TABLE " + TABLE_MESSAGES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_PHONE_NUMBER + " TEXT, " +
                    COLUMN_TIMESTAMP + " INTEGER, " +
                    COLUMN_IS_SENT + " INTEGER, " +
                    COLUMN_READ + " INTEGER DEFAULT 0)";

    // Singleton instance
    private static MessageDbHelper instance;

    // Get singleton instance
    public static synchronized MessageDbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new MessageDbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private MessageDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MESSAGES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For future migrations
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    /**
     * Insert a new message into the database
     */
    public long insertMessage(Message message) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CONTENT, message.getContent());
        values.put(COLUMN_PHONE_NUMBER, message.getPhoneNumber());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());
        values.put(COLUMN_IS_SENT, message.isSent() ? 1 : 0);
        values.put(COLUMN_READ, 0); // New messages are unread by default

        return db.insert(TABLE_MESSAGES, null, values);
    }

    /**
     * Get all messages for a specific phone number
     */
    public List<Message> getMessagesForContact(String phoneNumber) {
        List<Message> messages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        String selection = COLUMN_PHONE_NUMBER + " = ?";
        String[] selectionArgs = { phoneNumber };
        String sortOrder = COLUMN_TIMESTAMP + " ASC";

        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                null,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder)) {

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = cursorToMessage(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
        }

        return messages;
    }

    /**
     * Get the last message for each unique phone number
     */
    public List<Message> getLastMessagesForAllContacts() {
        List<Message> lastMessages = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        // This is a complex query that gets the last message for each phone number
        String query = "SELECT m1.* FROM " + TABLE_MESSAGES + " m1 " +
                "INNER JOIN (SELECT " + COLUMN_PHONE_NUMBER + ", MAX(" + COLUMN_TIMESTAMP + ") as max_time " +
                "FROM " + TABLE_MESSAGES + " GROUP BY " + COLUMN_PHONE_NUMBER + ") m2 " +
                "ON m1." + COLUMN_PHONE_NUMBER + " = m2." + COLUMN_PHONE_NUMBER + " " +
                "AND m1." + COLUMN_TIMESTAMP + " = m2.max_time " +
                "ORDER BY m1." + COLUMN_TIMESTAMP + " DESC";

        try (Cursor cursor = db.rawQuery(query, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Message message = cursorToMessage(cursor);
                    lastMessages.add(message);
                } while (cursor.moveToNext());
            }
        }

        return lastMessages;
    }

    /**
     * Get the count of unread messages from a specific phone number
     */
    public int getUnreadMessageCount(String phoneNumber) {
        SQLiteDatabase db = getReadableDatabase();
        String selection = COLUMN_PHONE_NUMBER + " = ? AND " + COLUMN_READ + " = 0 AND " + COLUMN_IS_SENT + " = 0";
        String[] selectionArgs = { phoneNumber };

        try (Cursor cursor = db.query(
                TABLE_MESSAGES,
                new String[] { "COUNT(*)" },
                selection,
                selectionArgs,
                null,
                null,
                null)) {

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        return 0;
    }

    /**
     * Mark all messages from a specific phone number as read
     */
    public int markMessagesAsRead(String phoneNumber) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_READ, 1);

        String selection = COLUMN_PHONE_NUMBER + " = ? AND " + COLUMN_READ + " = 0 AND " + COLUMN_IS_SENT + " = 0";
        String[] selectionArgs = { phoneNumber };

        return db.update(TABLE_MESSAGES, values, selection, selectionArgs);
    }

    /**
     * Convert a cursor to a Message object
     */
    private Message cursorToMessage(Cursor cursor) {
        int idIndex = cursor.getColumnIndex(COLUMN_ID);
        int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);
        int phoneNumberIndex = cursor.getColumnIndex(COLUMN_PHONE_NUMBER);
        int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);
        int isSentIndex = cursor.getColumnIndex(COLUMN_IS_SENT);

        String content = contentIndex != -1 ? cursor.getString(contentIndex) : "";
        String phoneNumber = phoneNumberIndex != -1 ? cursor.getString(phoneNumberIndex) : "";
        long timestamp = timestampIndex != -1 ? cursor.getLong(timestampIndex) : 0;
        boolean isSent = isSentIndex != -1 && cursor.getInt(isSentIndex) == 1;

        return new Message(content, phoneNumber, timestamp, isSent);
    }
}