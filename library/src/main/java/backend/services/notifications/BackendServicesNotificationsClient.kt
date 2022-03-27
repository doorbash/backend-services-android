package backend.services.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Build
import android.text.TextUtils
import android.util.Log
import androidx.core.app.NotificationCompat.*
import androidx.work.*
import androidx.work.ExistingPeriodicWorkPolicy.KEEP
import androidx.work.ExistingPeriodicWorkPolicy.REPLACE
import backend.services.Client
import backend.services.NotOKException
import backend.services.SHARED_PREFERENCES_NAME
import backend.services.async.Async
import backend.services.callbacks.Cancelable
import backend.services.callbacks.Function0Void
import backend.services.callbacks.Function1Void
import backend.services.db.NotificationDB
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MINUTES

private const val MIN_FETCH_INTERVAL = 15 // minutes
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
//            Client.init(context)
            val notificationsSharedPreferences =
                context.getSharedPreferences(
                    "${Client.options!!.projectId}-$NOTIFICATIONS_SHARED_PREF_NAME",
                    MODE_PRIVATE
                )
            val lastTime = notificationsSharedPreferences.getString(
                SHARED_PREFS_KEY_NOTIFICATIONS_LAST_TIME,
                ""
            )
            val lastTimeDate =
                if (lastTime != "") SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(lastTime)
                else null

            val result = try {
                Client.httpRequest("/${Client.options!!.projectId}/notifications?time=$lastTime") as JSONObject
            } catch (e: NotOKException) {
                Log.e(javaClass.simpleName, "error: ${e.message}")
                sharedPreferences.edit().putLong(SHARED_PREFS_KEY_NOTIFICATIONS_LAST_FETCH, now)
                    .commit()
                return arrayListOf()
            }
            sharedPreferences.edit().putLong(SHARED_PREFS_KEY_NOTIFICATIONS_LAST_FETCH, now)
                .commit()

            val notifications = result["notifications"] as JSONArray
            val time = result["time"] as String
            val list = ArrayList<Notification>()
            for (i in 0 until notifications.length()) {
                val nobj = notifications[i] as JSONObject

                val activeTime =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse((nobj["active_time"] as String))

                if (lastTimeDate != null && !activeTime.after(lastTimeDate)) continue

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
                    "big-text" -> NotificationStyle.BIG_TEXT
                    "big-image" -> NotificationStyle.BIG_IMAGE
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

                val bigText = if (nobj.has("big-text")) nobj.getString("big-text") else ""
                val bigImage = if (nobj.has("big-image")) nobj.getString("big-image") else ""

                list.add(
                    Notification(
                        id,
                        title,
                        text,
                        bigText,
                        icon,
                        image,
                        bigImage,
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

            return list
        }

        public suspend fun fetch(context: Context): List<Notification> {
            return Async.withLockAndTimeout(Client.init(context).options!!.timeout * 1000) {
                fetchImpl(context)
            }
        }

        @JvmStatic
        @JvmOverloads
        public fun fetch(
            context: Context,
            callback: Function1Void<List<Notification>>? = null,
            onError: Function1Void<Exception>? = null
        ): Cancelable {
            val job =
                Async.launchWithLockAndTimeout(Client.init(context).options!!.timeout * 1000) {
                    val list = try {
                        fetchImpl(context)
                    } catch (e: Exception) {
                        onError?.invoke(e)
                        return@launchWithLockAndTimeout
                    }
                    callback?.invoke(list)
                }
            return Cancelable { job.cancel() }
        }

        private suspend fun clickedImpl(context: Context, notifications: List<NotificationDB>) {
//            Client.init(context)
            try {
                val ids = TextUtils.join(",", notifications.map { it.id })
                Client.httpRequest("/${Client.options!!.projectId}/notifications/clicked?ids=$ids")
            } catch (e: NotOKException) {
                Log.e(javaClass.simpleName, "error: ${e.message}")
            }
        }


        internal suspend fun clicked(context: Context, notifications: List<NotificationDB>) {
            Async.withLockAndTimeout(Client.init(context).options!!.timeout * 1000) {
                clickedImpl(context, notifications)
            }
        }

        @JvmStatic
        @JvmOverloads
        internal fun clicked(
            context: Context,
            notifications: List<NotificationDB>,
            callback: Function0Void? = null,
            onError: Function1Void<Exception>? = null
        ): Cancelable {
            val job =
                Async.launchWithLockAndTimeout(Client.init(context).options!!.timeout * 1000) {
                    try {
                        clickedImpl(context, notifications)
                    } catch (e: Exception) {
                        onError?.invoke(e)
                        return@launchWithLockAndTimeout
                    }
                    callback?.invoke()
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
        @JvmOverloads
        public fun enqueueWorker(
            context: Context,
            repeatInterval: Long = 20,
            repeatIntervalTimeUnit: TimeUnit = MINUTES,
            initDelay: Long = 10,
            initDelayTimeUnit: TimeUnit = MINUTES
        ) {
            createNotificationChannel(context)
            val notificationsWorker = PeriodicWorkRequestBuilder<NotificationsWorker>(
                repeatInterval,
                repeatIntervalTimeUnit
            ).setInitialDelay(initDelay, initDelayTimeUnit).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).addTag("notifications").build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NOTIFICATIONS_WORKER_NAME,
                if (Client.update) REPLACE else KEEP,
                notificationsWorker
            )
        }

        @JvmStatic
        public fun cancelWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATIONS_WORKER_NAME)
        }
    }
}