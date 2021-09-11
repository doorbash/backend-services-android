package backend.services.notifications

import android.content.Context
import android.content.Intent
import android.net.Uri

abstract class NotificationAction {
    abstract fun getIntent(context: Context): Intent
}

class OpenActivityAction(val activity: Class<*>, val f: Int) : NotificationAction() {
    override fun getIntent(context: Context): Intent {
        return Intent(context, activity).apply { flags = f }
    }
}

class OpenLinkAction(val link: String) : NotificationAction() {
    override fun getIntent(context: Context): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(link))
    }
}