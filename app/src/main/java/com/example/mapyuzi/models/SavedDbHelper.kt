package com.example.mapyuzi.models

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SavedDbHelper(context: Context) :
    SQLiteOpenHelper(context, "saved_areas.db", null, 1) {

    companion object {
        const val TABLE_NAME = "areas"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_POINTS = "points"
        const val COL_AREA = "area"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT,
                $COL_POINTS TEXT,
                $COL_AREA TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertArea(area: SavedArea): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COL_NAME, area.name)
            put(COL_POINTS, area.points)
            put(COL_AREA, area.area)
        }
        return db.insert(TABLE_NAME, null, cv)
    }

    fun getAllAreas(): List<SavedArea> {
        val db = readableDatabase
        val list = ArrayList<SavedArea>()
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(0)
                val name = cursor.getString(1)
                val points = cursor.getString(2)
                val area = cursor.getString(3)
                list.add(SavedArea(id, name, points, area))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun deleteArea(id: Int): Int {
        val db = writableDatabase
        return db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
    }
}