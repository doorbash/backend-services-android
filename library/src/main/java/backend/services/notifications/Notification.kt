package backend.services.notifications

import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import backend.services.notifications.NotificationStyle.*
import backend.services.util.tryLoadBitmap

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
    BIG_TEXT,
    BIG_IMAGE
}

class Notification(
    val id: Int,
    val title: String,
    val text: String,
    val bigText: String,
    val icon: Int,
    val image: String,
    val bigImage: String,
    val priority: Int,
    val style: NotificationStyle,
    val nAction: NotificationAction,
    val clickReport: Boolean = true
) {

    fun show(context: Context) {
        Log.d(javaClass.simpleName, "showing notification: $this")

        val builder = NotificationCompat.Builder(context, NOTIFICATIONS_CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(priority)
            .setContentIntent(
                if (clickReport)
                    getActivity(
                        context,
                        id,
                        Intent(context, NotificationActivity::class.java).apply {
                            putExtra("id", id)
                            putExtra("action", nAction.action)
                            putExtra("extra", nAction.extra)

                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        },
                        FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
                    ) else
                    getPendingIntent(
                        context,
                        id,
                        nAction.action,
                        nAction.extra,
                        FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
                    )
            )
            .setAutoCancel(true)

        if (style == BIG_TEXT) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(bigText)
            )
        }

        tryLoadBitmap(context, image) { imageResult: Bitmap? ->
            if (imageResult != null) {
                builder.setLargeIcon(imageResult)
            }
            tryLoadBitmap(context, bigImage) { bigImageResult: Bitmap? ->
                if (bigImageResult != null) {
                    builder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bigImageResult)
                    )
                }
                NotificationManagerCompat.from(context).notify(id, builder.build())
            }
        }
    }

    companion object {
        fun getPendingIntent(
            context: Context,
            id: Int,
            action: ActionType,
            extra: String,
            flags: Int
        ): PendingIntent? {
            when (action) {
                ActionType.ACTIVITY -> {
                    val extraParts = extra.split(" ")
                    if (extraParts.size == 1) {
                        return getActivity(
                            context, id, Intent(context, Class.forName(extra)).addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            ), flags
                        )
                    } else if (extraParts.size == 2) {
                        return with(TaskStackBuilder.create(context)) {
                            val parentClass = Class.forName(extraParts[0])
                            addParentStack(parentClass)
                            addNextIntent(Intent(context, parentClass))
                            addNextIntent(Intent(context, Class.forName(extraParts[1])))
                        }.getPendingIntent(id, flags)
                    }
                }
                ActionType.LINK -> return getActivity(
                    context, id, Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(extra)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), flags
                )
            }
            return null
        }
    }
}