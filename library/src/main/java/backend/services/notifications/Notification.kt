package backend.services.notifications

import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

enum class ActionType {
    LINK,
    ACTIVITY
}

data class NotificationAction(
    val action: ActionType,
    val extra: String,
)

enum class NotificationStyle {
    NORMAL,
    BIG_TEXT
}

class Notification(
    val id: Int,
    val title: String,
    val text: String,
    val icon: Int,
    val image: String,
    val priority: Int,
    val style: NotificationStyle,
    val nAction: NotificationAction
) {

    fun show(context: Context) {
        Log.d(javaClass.simpleName, "showing notification: $this")

        var builder = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setContentIntent(
                getActivity(
                    context,
                    id,
                    Intent(context, NotificationActivity::class.java).apply {
                        putExtra("id", id)
                        putExtra("action", nAction.action)
                        putExtra("extra", nAction.extra)
                    },
                    FLAG_CANCEL_CURRENT
                )
            )
            .setAutoCancel(true)

        if (style == NotificationStyle.BIG_TEXT) {
            builder = builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(text)
            )
        }

        fun _show() {
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        }

        if (image != "") {
            Glide.with(context)
                .asBitmap()
                .load(image)
                .timeout(10000)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        builder = builder.setLargeIcon(resource)
                        _show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        _show()
                    }
                })
        } else _show()
    }
}