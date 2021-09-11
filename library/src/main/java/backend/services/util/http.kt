package backend.services.shared

import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.*

fun unSafeOkHttpClient(): OkHttpClient.Builder {
    val okHttpClient = OkHttpClient.Builder()
    try {
        // Create a trust manager that does not validate certificate chains
        val trustAllCerts: Array<TrustManager> = arrayOf(object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun checkServerTrusted(
                chain: Array<out X509Certificate>?,
                authType: String?
            ) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory
        if (trustAllCerts.isNotEmpty() && trustAllCerts.first() is X509TrustManager) {
            okHttpClient.sslSocketFactory(
                sslSocketFactory,
                trustAllCerts.first() as X509TrustManager
            )
            okHttpClient.hostnameVerifier { _, _ -> true }
        }

        return okHttpClient
    } catch (e: Exception) {
        return okHttpClient
    }
}