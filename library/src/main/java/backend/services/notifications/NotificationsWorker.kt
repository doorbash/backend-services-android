package backend.services.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import backend.services.db.DatabaseHelper
import backend.services.db.deleteNotifications
import backend.services.db.getNotifications

class NotificationsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DatabaseHelper(context).use { db ->
            val notifications = db.getNotifications(10)
            if (notifications != null && notifications.isNotEmpty()) {
                Log.d(
                    javaClass.simpleName,
                    "read ${notifications.size} notifications from database"
                )
                Log.d(javaClass.simpleName, notifications.toString())
                BackendServicesNotificationsClient.clicked(context, notifications)
                val numAffectedRows = db.deleteNotifications(notifications)
                Log.d(javaClass.simpleName, "deleted $numAffectedRows notifications from database")
            }
        }
        val notifications = BackendServicesNotificationsClient.fetch(context)
        Log.d(javaClass.simpleName, "notifications: $notifications")
        for (n in notifications) n.show(context)
        return Result.success()
    }
}