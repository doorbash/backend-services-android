package backend.services.app;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import backend.services.notifications.BackendServicesNotificationsClient;
import backend.services.notifications.Notification;
import backend.services.rc.BackendServicesRemoteConfigClient;
import backend.services.callbacks.Cancelable;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    Cancelable remoteConfigJob;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        if (remoteConfigJob != null) remoteConfigJob.cancel();
    }
}
