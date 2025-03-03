package com.example.ft_hangouts.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ft_hangouts.model.Contact;

import java.util.ArrayList;
import java.util.List;

public class ContactDbHelper extends SQLiteOpenHelper {
    
    // Database Info
    private static final String DATABASE_NAME = "contactsDatabase";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    private static final String TABLE_CONTACTS = "contacts";

    // Contact Table Columns
    private static final String KEY_CONTACT_ID = "id";
    private static final String KEY_CONTACT_NAME = "name";
    private static final String KEY_CONTACT_PHONE = "phone_number";
    private static final String KEY_CONTACT_EMAIL = "email";
    private static final String KEY_CONTACT_ADDRESS = "address";
    private static final String KEY_CONTACT_PHOTO = "photo";
    private static final String KEY_CONTACT_NOTES = "notes";

    // Singleton pattern
    private static ContactDbHelper sInstance;

    public static synchronized ContactDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ContactDbHelper(context.getApplicationContext());
        }
        return sInstance;
    }

    private ContactDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS +
                "(" +
                KEY_CONTACT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                KEY_CONTACT_NAME + " TEXT NOT NULL," +
                KEY_CONTACT_PHONE + " TEXT," +
                KEY_CONTACT_EMAIL + " TEXT," +
                KEY_CONTACT_ADDRESS + " TEXT," +
                KEY_CONTACT_PHOTO + " BLOB," +
                KEY_CONTACT_NOTES + " TEXT" +
                ")";

        db.execSQL(CREATE_CONTACTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            // Drop older table if existed
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
            // Create tables again
            onCreate(db);
        }
    }

    // Insert a contact into the database
    public long addContact(Contact contact) {
        SQLiteDatabase db = getWritableDatabase();
        long contactId = -1;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CONTACT_NAME, contact.getName());
            values.put(KEY_CONTACT_PHONE, contact.getPhoneNumber());
            values.put(KEY_CONTACT_EMAIL, contact.getEmail());
            values.put(KEY_CONTACT_ADDRESS, contact.getAddress());
            values.put(KEY_CONTACT_PHOTO, contact.getPhoto());
            values.put(KEY_CONTACT_NOTES, contact.getNotes());

            contactId = db.insertOrThrow(TABLE_CONTACTS, null, values);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return contactId;
    }

    // Get all contacts from the database
    public List<Contact> getAllContacts() {
        List<Contact> contacts = new ArrayList<>();

        String CONTACTS_SELECT_QUERY = "SELECT * FROM " + TABLE_CONTACTS + " ORDER BY " + KEY_CONTACT_NAME + " ASC";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(CONTACTS_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                do {
                    Contact contact = new Contact();
                    contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CONTACT_ID)));
                    contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NAME)));
                    contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHONE)));
                    contact.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_EMAIL)));
                    contact.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_ADDRESS)));
                    contact.setPhoto(cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHOTO)));
                    contact.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NOTES)));

                    contacts.add(contact);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return contacts;
    }

    // Get a single contact by id
    public Contact getContactById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Contact contact = null;

        String CONTACT_SELECT_QUERY = "SELECT * FROM " + TABLE_CONTACTS + " WHERE " + KEY_CONTACT_ID + " = " + id;
        Cursor cursor = db.rawQuery(CONTACT_SELECT_QUERY, null);

        try {
            if (cursor.moveToFirst()) {
                contact = new Contact();
                contact.setId(cursor.getLong(cursor.getColumnIndexOrThrow(KEY_CONTACT_ID)));
                contact.setName(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NAME)));
                contact.setPhoneNumber(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHONE)));
                contact.setEmail(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_EMAIL)));
                contact.setAddress(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_ADDRESS)));
                contact.setPhoto(cursor.getBlob(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHOTO)));
                contact.setNotes(cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_NOTES)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return contact;
    }

    // Update an existing contact
    public int updateContact(Contact contact) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(KEY_CONTACT_NAME, contact.getName());
            values.put(KEY_CONTACT_PHONE, contact.getPhoneNumber());
            values.put(KEY_CONTACT_EMAIL, contact.getEmail());
            values.put(KEY_CONTACT_ADDRESS, contact.getAddress());
            values.put(KEY_CONTACT_PHOTO, contact.getPhoto());
            values.put(KEY_CONTACT_NOTES, contact.getNotes());

            rowsAffected = db.update(TABLE_CONTACTS, values, KEY_CONTACT_ID + " = ?", 
                    new String[]{String.valueOf(contact.getId())});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return rowsAffected;
    }

    // Delete a contact
    public int deleteContact(long contactId) {
        SQLiteDatabase db = getWritableDatabase();
        int rowsAffected = 0;
        
        db.beginTransaction();
        try {
            rowsAffected = db.delete(TABLE_CONTACTS, KEY_CONTACT_ID + " = ?", 
                    new String[]{String.valueOf(contactId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }

        return rowsAffected;
    }
}