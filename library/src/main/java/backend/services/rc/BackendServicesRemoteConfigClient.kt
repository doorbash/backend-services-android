package backend.services.rc

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
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
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

private const val MIN_FETCH_INTERVAL = 15 // minutes
private const val RC_DATA_SHARED_PREF_NAME = "backend.services.rc"
private const val SHARED_PREFS_KEY_RC_LAST_FETCH = "rc_last_fetch_time"
private const val SHARED_PREFS_KEY_RC_VERSION = "____version____"
private const val RC_WORKER_NAME = "backend.services.rc"

class RemoteConfigBadVersionException(message: String?) : Exception(message)

class BackendServicesRemoteConfigClient {
    companion object {
        private suspend fun fetchImpl(context: Context) {
            val sharedPreferences =
                context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
            val lastFetchTime = sharedPreferences.getLong(SHARED_PREFS_KEY_RC_LAST_FETCH, 0L)
            val now = System.currentTimeMillis()
            if (now - lastFetchTime < MIN_FETCH_INTERVAL * 60 * 1000)
                throw Exception("fetch interval limit: please try ${(MIN_FETCH_INTERVAL * 60 * 1000 - now + lastFetchTime) / 1000} seconds later")
            Client.init(context)
            val rcSharedPreferences =
                context.getSharedPreferences(
                    "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                    MODE_PRIVATE
                )
            val lastVersion = rcSharedPreferences.getInt(SHARED_PREFS_KEY_RC_VERSION, 0)
            val result = try {
                Client.httpRequest("/${Client.options!!.projectId}/rc?version=$lastVersion") as JSONObject
            } catch (e: NotOKException) {
                Log.e(javaClass.simpleName, "error: ${e.message}")
                return
            } finally {
                sharedPreferences.edit().putLong(SHARED_PREFS_KEY_RC_LAST_FETCH, now).commit()
            }
            val data = result.getJSONObject("data")
            val version = result.getInt("version")
            if (lastVersion > 0 && version <= lastVersion) {
                throw RemoteConfigBadVersionException("expected version > $lastVersion. get version $version")
            }
            Log.d(javaClass.simpleName, "remote config: version: $version")
            val edit = rcSharedPreferences.edit()
            edit.clear()
            for (key in data.keys()) {
                when (val value = data.get(key)) {
                    is Boolean -> edit.putBoolean(key, value)
                    is Int -> edit.putInt(key, value)
                    is Long -> edit.putLong(key, value)
                    is Float -> edit.putFloat(key, value)
                    is Double -> edit.putFloat(key, value.toFloat())
                    is String -> edit.putString(key, value)
                    is JSONArray -> {
                        val set = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            if (value.get(i) is String) {
                                set.add(value.getString(i))
                            } else {
                                throw Exception("bad value at $key[$i]: $value, type: ${value.javaClass.name}")
                            }
                        }
                        edit.putStringSet(key, set)
                    }
                    else -> throw Exception("bad value at $key: $value, type: ${value.javaClass.name}")
                }
            }
            edit.putInt(SHARED_PREFS_KEY_RC_VERSION, version)
            edit.commit()
        }

        public suspend fun fetch(context: Context) {
            withContext(Async.coroutineContext) { fetchImpl(context) }
        }

        @JvmStatic
        @JvmOverloads
        public fun fetch(
            context: Context,
            callback: Function0Void? = null,
            onError: Function1Void<Exception>? = null
        ): Cancelable {
            val job = Async.launch {
                try {
                    fetchImpl(context)
                    callback?.invoke()
                } catch (e: Exception) {
                    onError?.invoke(e)
                }
            }
            return Cancelable { job.cancel() }
        }

        @JvmStatic
        @JvmOverloads
        public fun getBoolean(context: Context, key: String, default: Boolean = false): Boolean {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getBoolean(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun getInt(context: Context, key: String, default: Int = 0): Int {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getInt(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun getLong(context: Context, key: String, default: Long = 0L): Long {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getLong(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun getFloat(context: Context, key: String, default: Float = 0f): Float {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getFloat(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun getString(context: Context, key: String, default: String? = null): String? {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getString(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun getStringSet(
            context: Context,
            key: String,
            default: Set<String>? = null
        ): Set<String>? {
            return context.getSharedPreferences(
                "${Client.options!!.projectId}-$RC_DATA_SHARED_PREF_NAME",
                MODE_PRIVATE
            )
                .getStringSet(key, default)
        }

        @JvmStatic
        @JvmOverloads
        public fun enqueueWorker(
            context: Context,
            repeatInterval: Long = 3,
            repeatIntervalTimeUnit: TimeUnit = HOURS,
            initDelay: Long = 30,
            initDelayTimeUnit: TimeUnit = MINUTES
        ) {
            val notificationsWorker = PeriodicWorkRequestBuilder<RemoteConfigWorker>(
                repeatInterval,
                repeatIntervalTimeUnit
            ).setInitialDelay(initDelay, initDelayTimeUnit).setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            ).addTag("rc").build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                RC_WORKER_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                notificationsWorker
            )
        }

        @JvmStatic
        public fun cancelWorker(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(RC_WORKER_NAME)
        }
    }
}