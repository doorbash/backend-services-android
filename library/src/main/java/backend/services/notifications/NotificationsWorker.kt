package backend.services.notifications

import android.content.Context
import android.util.Log
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