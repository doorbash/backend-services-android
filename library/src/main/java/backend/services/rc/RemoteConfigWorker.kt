package backend.services.rc

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class RemoteConfigWorker(val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        BackendServicesRemoteConfigClient.fetch(context)
        return Result.success()
    }
}