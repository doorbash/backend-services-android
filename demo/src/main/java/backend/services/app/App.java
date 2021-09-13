package backend.services.app;

import android.app.Application;

import backend.services.Client;
import backend.services.ClientOptions;
import backend.services.notifications.BackendServicesNotificationsClient;
import backend.services.rc.BackendServicesRemoteConfigClient;

public class App extends Application {
    @Override
    public void onCreate() {
        Client.init(this, new ClientOptions(
                BuildConfig.VERSION_CODE,
                "com.example.project",
                "https://192.168.1.201/api",
                10,
                true,
                R.drawable.ic_demo_notification_icon,
                MainActivity.class
        ));
        BackendServicesNotificationsClient.enqueueWorker(this);
        BackendServicesRemoteConfigClient.enqueueWorker(this);
        super.onCreate();
    }
}
