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
        try {
            if (instance == null && context != null) {
                instance = new MessageDbHelper(context.getApplicationContext());
            }
            return instance;
        } catch (Exception e) {
            // Log error but don't crash
            android.util.Log.e("MessageDbHelper", "Error creating database helper", e);
            return null;
        }
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
        
        try {
            // Validate input
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                android.util.Log.e("MessageDbHelper", "Invalid phone number for query");
                return messages;
            }
            
            SQLiteDatabase db = getReadableDatabase();
            if (db == null) {
                android.util.Log.e("MessageDbHelper", "Could not get readable database");
                return messages;
            }
            
            // Check if the table exists
            try {
                Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", 
                    new String[]{TABLE_MESSAGES});
                boolean tableExists = checkTable != null && checkTable.moveToFirst();
                checkTable.close();
                
                if (!tableExists) {
                    // Table doesn't exist yet, create it
                    db.execSQL(SQL_CREATE_MESSAGES_TABLE);
                    return messages; // Return empty list since table was just created
                }
            } catch (Exception e) {
                android.util.Log.e("MessageDbHelper", "Error checking if table exists", e);
                // Try creating the table anyway
                try {
                    db.execSQL(SQL_CREATE_MESSAGES_TABLE);
                } catch (Exception e2) {
                    // Ignore - table may already exist
                }
            }

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
                        if (message != null) {
                            messages.add(message);
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                android.util.Log.e("MessageDbHelper", "Error querying messages for contact: " + phoneNumber, e);
            }
        } catch (Exception e) {
            android.util.Log.e("MessageDbHelper", "Fatal error in getMessagesForContact", e);
        }
        
        return messages;
    }

    /**
     * Get the last message for each unique phone number
     */
    public List<Message> getLastMessagesForAllContacts() {
        List<Message> lastMessages = new ArrayList<>();
        
        try {
            SQLiteDatabase db = getReadableDatabase();
            if (db == null) {
                return lastMessages;
            }

            // Use a simpler query first to check if the table exists
            try {
                Cursor checkTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", 
                    new String[]{TABLE_MESSAGES});
                boolean tableExists = checkTable != null && checkTable.moveToFirst();
                checkTable.close();
                
                if (!tableExists) {
                    // Table doesn't exist yet, create it
                    db.execSQL(SQL_CREATE_MESSAGES_TABLE);
                    return lastMessages; // Return empty list since table was just created
                }
            } catch (Exception e) {
                // If there's an error checking the table, try creating it
                try {
                    db.execSQL(SQL_CREATE_MESSAGES_TABLE);
                } catch (Exception e2) {
                    // Ignore - table may already exist
                }
                return lastMessages;
            }

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
                        if (message != null) {
                            lastMessages.add(message);
                        }
                    } while (cursor.moveToNext());
                }
            } catch (Exception e) {
                android.util.Log.e("MessageDbHelper", "Error executing query: " + query, e);
            }
        } catch (Exception e) {
            android.util.Log.e("MessageDbHelper", "Error in getLastMessagesForAllContacts", e);
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
        try {
            int idIndex = cursor.getColumnIndex(COLUMN_ID);
            int contentIndex = cursor.getColumnIndex(COLUMN_CONTENT);
            int phoneNumberIndex = cursor.getColumnIndex(COLUMN_PHONE_NUMBER);
            int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);
            int isSentIndex = cursor.getColumnIndex(COLUMN_IS_SENT);
            
            // Check if any required columns are missing
            if (contentIndex == -1 || phoneNumberIndex == -1 || timestampIndex == -1 || isSentIndex == -1) {
                android.util.Log.e("MessageDbHelper", "Missing columns in cursor");
                return null;
            }

            String content = cursor.getString(contentIndex);
            String phoneNumber = cursor.getString(phoneNumberIndex);
            long timestamp = cursor.getLong(timestampIndex);
            boolean isSent = cursor.getInt(isSentIndex) == 1;
            
            // Validate data
            if (phoneNumber == null || phoneNumber.isEmpty()) {
                phoneNumber = "Unknown";
            }
            
            if (content == null) {
                content = "";
            }
            
            if (timestamp <= 0) {
                timestamp = System.currentTimeMillis();
            }

            return new Message(content, phoneNumber, timestamp, isSent);
        } catch (Exception e) {
            android.util.Log.e("MessageDbHelper", "Error creating Message from cursor", e);
            return null;
        }
    }
}