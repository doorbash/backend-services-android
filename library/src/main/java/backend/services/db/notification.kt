package backend.services.db

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.text.TextUtils

private const val TABLE_NOTIFICATIONS = "notifications"
private const val COLUMN_NOTIFICATION_ID = "id"
private const val COLUMN_NOTIFICATION_INSERT_TIME = "insert_time"

class NotificationDB(val id: Int) {
    companion object{
        fun create(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE $TABLE_NOTIFICATIONS (" +
                        "$COLUMN_NOTIFICATION_ID INTEGER PRIMARY KEY NOT NULL, " +
                        "$COLUMN_NOTIFICATION_INSERT_TIME INTEGER NOT NULL" +
                        ")"
            )
        }
        fun upgrade(db: SQLiteDatabase){
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        }
    }
}

public fun DatabaseHelper.getNotifications(count: Int): List<NotificationDB>? {
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

public fun DatabaseHelper.insertNotification(notification: NotificationDB): Boolean {
    val ret = writableDatabase?.insert(TABLE_NOTIFICATIONS, null, ContentValues().apply {
        with(notification) {
            put(COLUMN_NOTIFICATION_ID, id)
            put(COLUMN_NOTIFICATION_INSERT_TIME, System.currentTimeMillis() / 1000)
        }
    })
    return ret != null && ret != -1L
}

fun DatabaseHelper.deleteNotifications(list: List<NotificationDB>): Int {
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