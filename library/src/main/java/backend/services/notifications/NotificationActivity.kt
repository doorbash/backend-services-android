package backend.services.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.core.app.TaskStackBuilder
import backend.services.db.DatabaseHelper
import backend.services.db.NotificationDB
import backend.services.db.insertNotification
import backend.services.notifications.ActionType.ACTIVITY
import backend.services.notifications.ActionType.LINK

class NotificationActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(applicationContext, intent)
    }

    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0);
    }

    private fun handleIntent(context: Context, intent: Intent) {
        val id = intent.getIntExtra("id", 0)
        val action = intent.getSerializableExtra("action") ?: return
        val extra = intent.getStringExtra("extra") ?: return

        if (id == 0) return

        Log.d(javaClass.simpleName, "id: $id")
        Log.d(javaClass.simpleName, "action: $action")
        Log.d(javaClass.simpleName, "extra: $extra")

        BackendServicesNotificationsClient.clicked(
            context,
            arrayListOf(NotificationDB(id)),
            {
                Log.d(javaClass.simpleName, "sending click event for notification $id is done!")
            },
            { e ->
                Log.d(
                    javaClass.simpleName,
                    "error while sending click event for notification $id: ${e.message}"
                )
                DatabaseHelper(context).apply {
                    if (insertNotification(NotificationDB(id))) {
                        Log.d(javaClass.simpleName, "inserted notification $id into database")
                    }
                    close()
                }
            },
        )

        when (action) {
            ACTIVITY -> {
                val extraParts = extra.split(" ")
                if (extraParts.size == 1) {
                    context.startActivity(
                        Intent(context, Class.forName(extra)).addFlags(
                            FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK
                        )
                    )
                } else if (extraParts.size == 2) {
                    with(TaskStackBuilder.create(context)) {
                        val parentClass = Class.forName(extraParts[0])
                        addParentStack(parentClass)
                        addNextIntent(Intent(context, parentClass))
                        addNextIntent(Intent(context, Class.forName(extraParts[1])))
                        startActivities()
                    }
                }
            }
            LINK -> context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(extra)).addFlags(FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}