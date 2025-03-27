package com.sitender.gs_v1;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "drawings.db";
    private static final int DATABASE_VERSION = 1;

    // Table Name
    private static final String TABLE_DRAWINGS = "drawings";

    // Table Columns
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_THUMBNAIL = "thumbnail";
    private static final String KEY_DRAWING_DATA = "drawing_data";
    private static final String KEY_CREATED_AT = "created_at";
    private static final String KEY_UPDATED_AT = "updated_at";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // FIX: Add db.execSQL()
        String CREATE_DRAWINGS_TABLE = "CREATE TABLE " + TABLE_DRAWINGS + "("
                + KEY_ID + " INTEGER PRIMARY KEY,"
                + KEY_NAME + " TEXT,"
                + KEY_THUMBNAIL + " BLOB,"
                + KEY_DRAWING_DATA + " TEXT,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + KEY_UPDATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_DRAWINGS_TABLE); // THIS LINE WAS MISSING
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DRAWINGS);
        onCreate(db);
    }

    // Method to save a drawing
    public long saveDrawing(String name, Bitmap thumbnail, String drawingData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_NAME, name);
        values.put(KEY_THUMBNAIL, bitmapToByteArray(thumbnail));
        values.put(KEY_DRAWING_DATA, drawingData);
        try {
            long id = db.insert(TABLE_DRAWINGS, null, values);
            db.close();
            return id;
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error saving drawing", e);
            db.close();
            return -1;
        }
    }

    // Method to update an existing drawing
    public int updateDrawing(long id, String name, Bitmap thumbnail, String drawingData) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        if (name != null) values.put(KEY_NAME, name);
        if (thumbnail != null) values.put(KEY_THUMBNAIL, bitmapToByteArray(thumbnail));
        if (drawingData != null) values.put(KEY_DRAWING_DATA, drawingData);

        // Update row
        int rowsAffected = db.update(TABLE_DRAWINGS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rowsAffected;
    }

    // Method to delete a drawing
    public void deleteDrawing(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_DRAWINGS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Get all drawings
    public List<DrawingInfo> getAllDrawings() {
        List<DrawingInfo> drawingList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_DRAWINGS + " ORDER BY " + KEY_UPDATED_AT + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(KEY_ID);
            int nameIndex = cursor.getColumnIndex(KEY_NAME);
            int thumbnailIndex = cursor.getColumnIndex(KEY_THUMBNAIL);
            int createdAtIndex = cursor.getColumnIndex(KEY_CREATED_AT);
            int updatedAtIndex = cursor.getColumnIndex(KEY_UPDATED_AT);

            do {
                DrawingInfo drawing = new DrawingInfo();

                if (idIndex != -1) {
                    drawing.setId(cursor.getLong(idIndex));
                }
                if (nameIndex != -1) {
                    drawing.setName(cursor.getString(nameIndex));
                }
                if (thumbnailIndex != -1) {
                    drawing.setThumbnail(byteArrayToBitmap(cursor.getBlob(thumbnailIndex)));
                }
                if (createdAtIndex != -1) {
                    drawing.setCreatedAt(cursor.getString(createdAtIndex));
                }
                if (updatedAtIndex != -1) {
                    drawing.setUpdatedAt(cursor.getString(updatedAtIndex));
                }

                drawingList.add(drawing);
            } while (cursor.moveToNext());
        }


        cursor.close();
        db.close();
        return drawingList;
    }

    // Get a specific drawing with all data

    @SuppressLint("Range")
    public DrawingInfo getDrawing(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        DrawingInfo drawing = new DrawingInfo();
        Cursor cursor = null;

        try {
            cursor = db.query(
                    TABLE_DRAWINGS,
                    null,
                    KEY_ID + " = ?",
                    new String[]{String.valueOf(id)},
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                drawing.setId(cursor.getLong(cursor.getColumnIndex(KEY_ID)));
                drawing.setName(cursor.getString(cursor.getColumnIndex(KEY_NAME)));
                drawing.setDrawingData(cursor.getString(cursor.getColumnIndex(KEY_DRAWING_DATA)));
            } else {
                Log.e("DatabaseHelper", "No drawing found with ID: " + id);
                return null;
            }
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Error retrieving drawing", e);
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        return drawing;
    }
    // Utility method to convert Bitmap to byte array
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    // Utility method to convert byte array to Bitmap
    private Bitmap byteArrayToBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }

    // Utility method to get current timestamp
    private String getCurrentTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    // Model class to hold drawing information
    public static class DrawingInfo {
        private long id;
        private String name;
        private Bitmap thumbnail;
        private String drawingData;
        private String createdAt;
        private String updatedAt;

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Bitmap getThumbnail() { return thumbnail; }
        public void setThumbnail(Bitmap thumbnail) { this.thumbnail = thumbnail; }

        public String getDrawingData() { return drawingData; }
        public void setDrawingData(String drawingData) { this.drawingData = drawingData; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }
}