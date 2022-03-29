package backend.services.notifications

import android.app.PendingIntent.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
                getActivity(
                    context,
                    id,
                    Intent(context, NotificationActivity::class.java).apply {
                        putExtra("id", id)
                        putExtra("action", nAction.action)
                        putExtra("extra", nAction.extra)
                        putExtra("click_report", clickReport)

                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
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
}