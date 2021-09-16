package backend.services.notifications

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import backend.services.callbacks.Function0Void
import backend.services.callbacks.Function1Void

class NotificationActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getIntExtra("id", 0)
        val action = intent.getSerializableExtra("action")
        val extra = intent.getStringExtra("extra")
        val flags = intent.getIntExtra("flags", 0)

        Log.d(javaClass.simpleName, "id: $id")
        Log.d(javaClass.simpleName, "action: $action")
        Log.d(javaClass.simpleName, "extra: $extra")
        Log.d(javaClass.simpleName, "flags: $flags")

        BackendServicesNotificationsClient.clicked(
            this,
            id,
            object : Function0Void {
                override fun invoke() {
                    Log.d(javaClass.simpleName, "click completed!")
                }
            },
            object : Function1Void<Exception> {
                override fun invoke(e: Exception?) {
                    Log.d(javaClass.simpleName, "click error: ${e?.message}")
                }
            }
        )

        startActivity(when (action) {
            ActionType.LINK -> Intent(Intent.ACTION_VIEW, Uri.parse(extra))
            ActionType.ACTIVITY -> Intent(this, Class.forName(extra))
            else -> return
        }.apply {
            this.flags = flags
        })
    }
}