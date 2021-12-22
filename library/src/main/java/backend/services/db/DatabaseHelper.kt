package backend.services.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

private const val DB_NAME = "backend.services.db"
private const val DB_VERSION = 1

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        NotificationDB.create(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        NotificationDB.upgrade(db)
        onCreate(db)
    }

    inline fun <R> use(block: (DatabaseHelper) -> R): R {
        var closed = false
        try {
            return block(this)
        } catch (e: Exception) {
            closed = true
            try {
                close()
            } catch (closeException: Exception) {
                // eat the closeException as we are already throwing the original cause
                // and we don't want to mask the real exception
            }
            throw e
        } finally {
            if (!closed) {
                close()
            }
        }
    }
}