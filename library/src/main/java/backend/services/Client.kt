package backend.services

import android.content.Context
import android.util.Log
import backend.services.shared.unSafeOkHttpClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import ru.gildor.coroutines.okhttp.await
import java.util.concurrent.TimeUnit

internal const val SHARED_PREFERENCES_NAME = "backend.services.data"
private const val SHARED_PREFS_KEY_VERSION_CODE = "version_code"
private const val SHARED_PREFS_KEY_PROJECT_ID = "project_id"
private const val SHARED_PREFS_KEY_BASE_URL = "base_url"
private const val SHARED_PREFS_KEY_TIMEOUT = "timeout"
private const val SHARED_PREFS_KEY_INSECURE = "insecure"
private const val SHARED_PREFS_KEY_NOTIFICATION_ICON = "notification_icon"
private const val SHARED_PREFS_KEY_NOTIFICATION_DEFAULT_ACTIVITY = "notification_default_activity"

class NotOKException(message: String?) : Exception(message)

class Client(var options: ClientOptions? = null) {
    private lateinit var httpClient: OkHttpClient

    private fun initHttpClient() {
        httpClient =
            (if (!options!!.insecure) OkHttpClient.Builder() else unSafeOkHttpClient()).callTimeout(
                options!!.timeout,
                TimeUnit.SECONDS
            ).build()
    }

    public fun init(context: Context): Client {
        if (options == null) {
            options = ClientOptions.load(context)
            initHttpClient()
        } else {
            initHttpClient()
            options?.save(context)
        }
        return this
    }

    suspend fun httpRequest(path: String): Any {
        val url = "${options!!.baseUrl}${path}"
        Log.d(javaClass.simpleName, "httpRequest(): sending request to $url")
        val request = Request.Builder().url(url).build()
        val res = httpClient.newCall(request).await()
        val body = res.body!!.string()
        Log.d(javaClass.simpleName, "${res.code} ---  $body")
        val resultJson = JSONObject(body)
        if (resultJson["ok"] != true) {
            throw NotOKException(resultJson["error"] as String)
        }
        return resultJson["result"]
    }
}

class ClientOptions(
    val versionCode: Int,
    val projectId: String,
    val baseUrl: String,
    val timeout: Long,
    val insecure: Boolean,
    val notificationIcon: Int,
    val notificationDefaultActivity: Class<*>
) {

    private val projectRegex = Regex("^[A-Za-z0-9._-]+")

    init {
        if (!projectRegex.matches(projectId)) throw Exception("bad project id")
    }

    public fun save(context: Context) {
        val sharedPreferences = context.getSharedPreferences(
            SHARED_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        sharedPreferences.edit()
            .putInt(SHARED_PREFS_KEY_VERSION_CODE, versionCode)
            .putString(SHARED_PREFS_KEY_PROJECT_ID, projectId)
            .putString(SHARED_PREFS_KEY_BASE_URL, baseUrl)
            .putLong(SHARED_PREFS_KEY_TIMEOUT, timeout)
            .putBoolean(SHARED_PREFS_KEY_INSECURE, insecure)
            .putInt(SHARED_PREFS_KEY_NOTIFICATION_ICON, notificationIcon)
            .putString(SHARED_PREFS_KEY_NOTIFICATION_DEFAULT_ACTIVITY, notificationDefaultActivity.name)
            .commit()
    }

    companion object {
        public fun load(context: Context): ClientOptions {
            val sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

            val versionCode = sharedPreferences.getInt(SHARED_PREFS_KEY_VERSION_CODE, 0)

            val projectId = sharedPreferences.getString(SHARED_PREFS_KEY_PROJECT_ID, null)
                ?: throw Exception("bad project id")

            val baseUrl = sharedPreferences.getString(SHARED_PREFS_KEY_BASE_URL, null)
                ?: throw Exception("bad base url")

            val timeout = sharedPreferences.getLong(SHARED_PREFS_KEY_TIMEOUT, 10)

            val insecure = sharedPreferences.getBoolean(SHARED_PREFS_KEY_INSECURE, false)

            val notificationIcon =
                sharedPreferences.getInt(SHARED_PREFS_KEY_NOTIFICATION_ICON, 0)

            val notificationDefaultActivity = Class.forName(sharedPreferences.getString(
                SHARED_PREFS_KEY_NOTIFICATION_DEFAULT_ACTIVITY, null) ?: throw Exception("bad notification default activity"))

            return ClientOptions(versionCode, projectId, baseUrl, timeout, insecure, notificationIcon, notificationDefaultActivity)
        }
    }
}