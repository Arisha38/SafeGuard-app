package com.safeguard.womensafety;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ContactsDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "safeguard.db";
    private static final int DB_VERSION = 1;
    private static final String TABLE_CONTACTS = "contacts";

    public ContactsDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CONTACTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONTACTS);
        onCreate(db);
    }

    public long addContact(String name, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        return db.insert(TABLE_CONTACTS, null, values);
    }

    public int updateContact(int id, String name, String phone) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);
        return db.update(TABLE_CONTACTS, values, "id=?", new String[]{String.valueOf(id)});
    }

    public int deleteContact(int id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_CONTACTS, "id=?", new String[]{String.valueOf(id)});
    }

    public List<Contact> getAllContacts() {
        List<Contact> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, phone FROM " + TABLE_CONTACTS + " ORDER BY id DESC", null);
        while (c.moveToNext()) {
            list.add(new Contact(c.getInt(0), c.getString(1), c.getString(2)));
        }
        c.close();
        return list;
    }

    public Contact getContactById(int id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, phone FROM " + TABLE_CONTACTS + " WHERE id=?",
                new String[]{String.valueOf(id)});
        Contact contact = null;
        if (c.moveToFirst()) {
            contact = new Contact(c.getInt(0), c.getString(1), c.getString(2));
        }
        c.close();
        return contact;
    }
}
