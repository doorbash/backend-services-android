# Backend-Services-Android

Android client for https://github.com/doorbash/backend-services

## Download

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.doorbash:backend-services-android:1.3.5'
}
```

## Usage
```java
Client.init(this, new ClientOptions(
        BuildConfig.VERSION_CODE,
        BuildConfig.APPLICATION_ID,
        "https://your.domain.com/api",
        20,
        false,
        "Notifications",
        R.drawable.ic_notification_icon
));
BackendServicesNotificationsClient.enqueueWorker(this);
BackendServicesRemoteConfigClient.enqueueWorker(this);
```

## Examples
- [backend-services-android-demo](https://github.com/doorbash/backend-services-android-demo)