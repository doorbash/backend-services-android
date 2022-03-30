package backend.services.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import backend.services.db.DatabaseHelper
import backend.services.db.NotificationDB
import backend.services.db.insertNotification

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
                DatabaseHelper(context).use {
                    if (it.insertNotification(NotificationDB(id))) {
                        Log.d(javaClass.simpleName, "inserted notification $id into database")
                    }
                }
            },
        )

        Notification.getPendingIntent(context, id, action as ActionType, extra, 0)?.send()

        finish()
    }
}