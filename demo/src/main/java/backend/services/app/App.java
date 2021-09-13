package backend.services.app;

import android.app.Application;

import backend.services.Client;
import backend.services.ClientOptions;
import backend.services.notifications.BackendServicesNotificationsClient;
import backend.services.rc.BackendServicesRemoteConfigClient;

public class App extends Application {
    @Override
    public void onCreate() {
        new Client(new ClientOptions(
                BuildConfig.VERSION_CODE,
                "com.example.project",
                "https://your.domain.com/api",
                10,
                false,
                R.drawable.ic_demo_notification_icon,
                MainActivity.class
        )).init(this);
        BackendServicesNotificationsClient.enqueueWorker(this);
        BackendServicesRemoteConfigClient.enqueueWorker(this);
        super.onCreate();
    }
}
