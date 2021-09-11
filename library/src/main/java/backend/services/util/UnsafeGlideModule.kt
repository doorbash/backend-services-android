package backend.services.util

import android.content.Context
import backend.services.shared.unSafeOkHttpClient
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import java.io.InputStream
import java.util.concurrent.TimeUnit

@GlideModule
internal class UnsafeGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(
            GlideUrl::class.java,
            InputStream::class.java,
            OkHttpUrlLoader.Factory(unSafeOkHttpClient().callTimeout(10, TimeUnit.SECONDS).build())
        )
    }
}