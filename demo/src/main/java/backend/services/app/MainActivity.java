package backend.services.app;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import backend.services.callbacks.Cancelable;
import backend.services.notifications.ActionType;
import backend.services.notifications.Notification;
import backend.services.notifications.NotificationAction;
import backend.services.notifications.NotificationStyle;
import backend.services.rc.BackendServicesRemoteConfigClient;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    Cancelable remoteConfigJob;
    int id;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.notification_to_main_activity).setOnClickListener(view -> new Notification(
                ++id,
                "#" + id + " MainActivity",
                "Opens MainActivity",
                "So basically what it does is... it opens MainActivity......",
                R.drawable.ic_demo_notification_icon,
                "https://avatars.githubusercontent.com/u/5982526?v=4",
                "",
                NotificationCompat.PRIORITY_MAX,
                NotificationStyle.BIG_TEXT,
                new NotificationAction(
                        ActionType.ACTIVITY,
                        MainActivity.class.getName()
                )
        ).show(this));
        findViewById(R.id.notification_to_other_activity).setOnClickListener(view -> new Notification(
                ++id,
                "#" + id + " OtherActivity",
                "Opens OtherActivity",
                "",
                R.drawable.ic_demo_notification_icon,
                "https://avatars.githubusercontent.com/u/5982526?v=4",
                "",
                NotificationCompat.PRIORITY_MAX,
                NotificationStyle.NORMAL,
                new NotificationAction(
                        ActionType.ACTIVITY,
                        OtherActivity.class.getName()
                )
        ).show(this));
        findViewById(R.id.notification_to_other_activity_stack).setOnClickListener(view -> new Notification(
                ++id,
                "#" + id + " OtherActivity",
                "Opens OtherActivity",
                "",
                R.drawable.ic_demo_notification_icon,
                "https://avatars.githubusercontent.com/u/5982526?v=4",
                "",
                NotificationCompat.PRIORITY_MAX,
                NotificationStyle.NORMAL,
                new NotificationAction(
                        ActionType.ACTIVITY,
                        MainActivity.class.getName() + " " + OtherActivity.class.getName()
                )
        ).show(this));
        findViewById(R.id.notification_to_link).setOnClickListener(view -> new Notification(
                ++id,
                "#" + id + " Link",
                "Opens a link",
                "",
                R.drawable.ic_demo_notification_icon,
                "https://avatars.githubusercontent.com/u/5982526?v=4",
                "https://i.kym-cdn.com/entries/icons/original/000/026/638/cat.jpg",
                NotificationCompat.PRIORITY_MAX,
                NotificationStyle.NORMAL,
                new NotificationAction(
                        ActionType.LINK,
                        "https://github.com/doorbash/backend-services"
                )
        ).show(this));
    }



    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        if (remoteConfigJob != null) remoteConfigJob.cancel();
        remoteConfigJob = BackendServicesRemoteConfigClient.fetch(this, () -> {
            Log.d(TAG, "fetch complete!!");
        }, error -> {
            Log.e(TAG, "fetch error!!");
            Log.e(TAG, error.getMessage());
        });
    }
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (remoteConfigJob != null) remoteConfigJob.cancel();
    }
}
