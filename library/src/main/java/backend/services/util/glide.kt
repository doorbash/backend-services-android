package backend.services.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

private const val TIMEOUT = 10000

internal fun Context.isValidForGlide(): Boolean {
    if (this is Activity) {
        return !isDestroyed && !isFinishing
    }
    return true
}

internal fun tryLoadBitmap(
    context: Context,
    url: String?,
    done: (result: Bitmap?) -> Unit
) {
    if (!context.isValidForGlide()) return done(null)
    if (url != null && url != "") {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .timeout(TIMEOUT)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    return done(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    return done(null)
                }
            })
    } else return done(null)
}