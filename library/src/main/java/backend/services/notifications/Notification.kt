package backend.services.notifications

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.getActivity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import backend.services.notifications.ActionType.ACTIVITY
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

        val intent = Intent(context, NotificationActivity::class.java).apply {
            putExtra("id", id)
            putExtra("action", nAction.action)
            putExtra("extra", nAction.extra)
            putExtra("flags", 0)
            this.flags = FLAG_ACTIVITY_NEW_TASK
        }

        val extraParts = nAction.extra.split(' ')

        val pendingIntent: PendingIntent = when (nAction.action) {
            ACTIVITY -> {
                if (extraParts.size == 2) {
                    intent.putExtra("extra", extraParts[1])
                    with(TaskStackBuilder.create(context)) {
                        val parentClass = Class.forName(extraParts[0])
                        val i = Intent(context, parentClass)
                        addParentStack(parentClass)
                        addNextIntent(i)
                        addNextIntent(intent)
                        getPendingIntent(id, FLAG_CANCEL_CURRENT)!!
                    }
                } else {
                    intent.putExtra("flags", FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
                    getActivity(context, id, intent, FLAG_CANCEL_CURRENT)
                }
            }
            else -> {
                getActivity(context, id, intent, FLAG_CANCEL_CURRENT)
            }
        }

        var builder = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setContentIntent(pendingIntent)
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