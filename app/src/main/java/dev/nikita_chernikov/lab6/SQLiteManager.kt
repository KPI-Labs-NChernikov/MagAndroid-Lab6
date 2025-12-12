package dev.nikita_chernikov.lab6

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class SQLiteManager(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Reminders.db"
        private const val DATABASE_VERSION = 1
        const val TABLE_NAME = "reminders"
        const val COLUMN_ID = "id"
        const val COLUMN_THEME = "theme"
        const val COLUMN_MESSAGE = "message"
        const val COLUMN_DATE = "date"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_THEME TEXT, " +
                "$COLUMN_MESSAGE TEXT, " +
                "$COLUMN_DATE INTEGER)" // Storing date as Long (timestamp)
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addReminder(theme: String, message: String, date: Long): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_THEME, theme)
            put(COLUMN_MESSAGE, message)
            put(COLUMN_DATE, date)
        }
        val id = db.insert(TABLE_NAME, null, values)
        db.close()
        return id
    }

    fun getAllReminders(): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME ORDER BY $COLUMN_DATE DESC", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val theme = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THEME))
                val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))
                reminders.add(Reminder(id, theme, message, date))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return reminders
    }

    fun deleteReminder(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID=?", arrayOf(id.toString()))
        db.close()
    }

    fun getReminder(id: Long): Reminder? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            null,
            "$COLUMN_ID=?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        var reminder: Reminder? = null
        if (cursor.moveToFirst()) {
            val theme = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THEME))
            val message = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE))
            val date = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE))
            reminder = Reminder(id, theme, message, date)
        }
        cursor.close()
        db.close()
        return reminder
    }
}
