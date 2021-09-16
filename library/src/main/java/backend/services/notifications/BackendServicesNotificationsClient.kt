package backend.services.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat.*
import androidx.work.*
import backend.services.Client
import backend.services.NotOKException
import backend.services.SHARED_PREFERENCES_NAME
import backend.services.async.Async
import backend.services.callbacks.Cancelable
import backend.services.callbacks.Function0Void
import backend.services.callbacks.Function1Void
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val MIN_FETCH_INTERVAL = 10 // minutes
private const val NOTIFICATIONS_SHARED_PREF_NAME = "backend.services.notifications"
private const val SHARED_PREFS_KEY_NOTIFICATIONS_LAST_FETCH = "notifications_last_fetch_time"
private const val SHARED_PREFS_KEY_NOTIFICATIONS_LAST_TIME = "____last_time____"
private const val NOTIFICATIONS_WORKER_NAME = "backend.services.notifications"
internal const val NOTIFICATIONS_CHANNEL_ID = "backend.services.notifications"
private const val NOTIFICATIONS_CHANNEL_NAME = "Notifications"

class BackendServicesNotificationsClient {
    companion object {
        private suspend fun fetchImpl(context: Context): List<Notification> {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
            val lastFetchTime =
                sharedPreferences.getLong(SHARED_PREFS_KEY_NOTIFICATIONS_LAST_FETCH, 0L)
            val now = System.currentTimeMillis()
            if (now - lastFetchTime < MIN_FETCH_INTERVAL * 60 * 1000)
                throw Exception("fetch interval limit: please try ${(MIN_FETCH_INTERVAL * 60 * 1000 - now + lastFetchTime) / 1000} seconds later")
            Client.init(context)
            val notificationsSharedPreferences =
                context.getSharedPreferences(
                    "${Client.options!!.projectId}-$NOTIFICATIONS_SHARED_PREF_NAME",
                    MODE_PRIVATE
                )
            val lastTime = notificationsSharedPreferences.getString(
                SHARED_PREFS_KEY_NOTIFICATIONS_LAST_TIME,
                ""
            )
            val result = try {
                Client.httpRequest("/${Client.options!!.projectId}/notifications?time=$lastTime") as JSONObject
            } catch (e: NotOKException) {
                Log.e(javaClass.simpleName, "error: ${e.message}")
                return arrayListOf()
            }
            val notifications = result["notifications"] as JSONArray
            val time = result["time"] as String
            val list = ArrayList<Notification>()
            for (i in 0 until notifications.length()) {
                val nobj = notifications[i] as JSONObject

                val id = nobj["id"] as Int

                val title = nobj["title"] as String

                val text = nobj["text"] as String

                val icon = Client.options!!.notificationIcon

                val image = if (nobj.has("image")) nobj.getString("image") else ""

                val priority =
                    when (if (nobj.has("priority")) nobj["priority"] as String else "default") {
                        "high" -> PRIORITY_HIGH
                        "low" -> PRIORITY_LOW
                        "min" -> PRIORITY_MIN
                        "max" -> PRIORITY_MAX
                        "default" -> PRIORITY_DEFAULT
                        else -> throw Exception("bad priority")
                    }

                val style = when (if (nobj.has("style")) nobj.getString("style") else "") {
                    "big" -> NotificationStyle.BIG_TEXT
                    else -> NotificationStyle.NORMAL
                }

                val action = when (if (nobj.has("action")) nobj.getString("action") else null) {
                    "activity" -> NotificationAction(
                        ActionType.ACTIVITY,
                        nobj["extra"] as String,
                    )
                    "link" -> NotificationAction(
                        ActionType.LINK,
                        nobj["extra"] as String
                    )
                    "update" -> {
                        val parts = nobj.getString("extra").split(" ")
                        if (parts.size != 2) continue
                        val version = parts[1].toIntOrNull() ?: continue
                        if (Client.options!!.versionCode >= version) continue
                        NotificationAction(ActionType.LINK, parts[0])
                    }
                    else -> continue
                }

                list.add(
                    Notification(
                        id,
                        title,
                        text,
                        icon,
                        image,
                        priority,
                        style,
                        action
                    )
                )
            }

            notificationsSharedPreferences
                .edit()
                .putString(SHARED_PREFS_KEY_NOTIFICATIONS_LAST_TIME, time)
                .commit()

            sharedPreferences.edit()
                .putLong(SHARED_PREFS_KEY_NOTIFICATIONS_LAST_FETCH, now)
                .commit()

            return list
        }

        public suspend fun fetch(context: Context): List<Notification> {
            return withContext(Async.coroutineContext) { fetchImpl(context) }
        }

        @JvmStatic
        @JvmOverloads
        public fun fetch(
            context: Context,
            callback: Function1Void<List<Notification>>? = null,
            onError: Function1Void<Exception>? = null
        ): Cancelable {
            val job = Async.launch {
                try {
                    callback?.invoke(fetchImpl(context))
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
            return Cancelable { job.cancel() }
        }

        private suspend fun clickedImpl(context: Context, id: Int) {
            Client.init(context)
            try {
                Client.httpRequest("/${Client.options!!.projectId}/notifications/clicked?id=$id") as JSONObject
            } catch (e: NotOKException) {
                Log.e(javaClass.simpleName, "error: ${e.message}")
            }
        }


        internal suspend fun clicked(context: Context, id: Int) {
            withContext(Async.coroutineContext) { clickedImpl(context, id) }
        }

        @JvmStatic
        @JvmOverloads
        internal fun clicked(
            context: Context,
            id: Int,
            callback: Function0Void? = null,
            onError: Function1Void<Exception>? = null
        ): Cancelable {
            val job = Async.launch {
                try {
                    clickedImpl(context, id)
                    callback?.invoke()
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
            return Cancelable { job.cancel() }
        }


        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(
                    NOTIFICATIONS_CHANNEL_ID,
                    NOTIFICATIONS_CHANNEL_NAME,
                    importance
                )
                val notificationManager: NotificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        @JvmStatic
        public fun enqueueWorker(context: Context) {
            createNotificationChannel(context)
            val notificationsWorker = PeriodicWorkRequestBuilder<NotificationsWorker>(
                15,
                TimeUnit.MINUTES
            ).setInitialDelay(10, TimeUnit.MINUTES).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).addTag("notifications").build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOTIFICATIONS_WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                notificationsWorker
            )
        }

        @JvmStatic
        public fun cancelWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATIONS_WORKER_NAME)
        }
    }
}