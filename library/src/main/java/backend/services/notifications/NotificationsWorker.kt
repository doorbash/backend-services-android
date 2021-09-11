package backend.services.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationsWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val notifications = BackendServicesNotificationsClient.fetch(context)
        Log.d(javaClass.simpleName, "notifications: $notifications")
        for (n in notifications) n.show(context)
        return Result.success()
    }
}