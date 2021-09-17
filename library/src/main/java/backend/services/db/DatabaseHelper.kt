package backend.services.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils

private const val DB_NAME = "backend.services.db"
private const val DB_VERSION = 1

const val TABLE_NOTIFICATIONS = "notifications"
const val COLUMN_NOTIFICATION_ID = "id"
const val COLUMN_NOTIFICATION_INSERT_TIME = "insert_time"

data class NotificationDB(val id: Int)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE $TABLE_NOTIFICATIONS (" +
                    "$COLUMN_NOTIFICATION_ID INTEGER PRIMARY KEY NOT NULL, " +
                    "$COLUMN_NOTIFICATION_INSERT_TIME INTEGER NOT NULL" +
                    ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        onCreate(db)
    }

    fun getNotifications(count: Int): List<NotificationDB>? {
        val c = readableDatabase?.query(
            TABLE_NOTIFICATIONS,
            arrayOf(COLUMN_NOTIFICATION_ID),
            null,
            null,
            null,
            null,
            "$COLUMN_NOTIFICATION_INSERT_TIME ASC",
            "$count"
        ) ?: return null
        val notifications = arrayListOf<NotificationDB>()
        while (c.moveToNext()) {
            notifications.add(NotificationDB(c.getInt(0)))
        }
        c.close()
        return notifications
    }

    fun insertNotification(notification: NotificationDB): Boolean {
        val ret = writableDatabase?.insert(TABLE_NOTIFICATIONS, null, ContentValues().apply {
            with(notification) {
                put(COLUMN_NOTIFICATION_ID, id)
                put(COLUMN_NOTIFICATION_INSERT_TIME, System.currentTimeMillis() / 1000)
            }
        })
        return ret != null && ret != -1L
    }

    fun deleteNotifications(list: List<NotificationDB>): Int {
        return writableDatabase?.delete(
            TABLE_NOTIFICATIONS,
            String.format(
                "%s IN (%s)",
                COLUMN_NOTIFICATION_ID,
                TextUtils.join(",", list.map { it.id })
            ),
            null
        ) ?: 0
    }
}