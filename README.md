# Backend-Service-Android

Android client for https://github.com/doorbash/backend-services

## Download

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.doorbash:backend-services-android:master-SNAPSHOT'
}
```

## Usage
```java
new Client(new ClientOptions(
        BuildConfig.VERSION_CODE,
        "com.example.project",
        "https://your.domain/api",
        10,
        true,
        R.drawable.ic_demo_notification_icon,
        MainActivity.class
)).init(this);
BackendServicesNotificationsClient.enqueueWorker(this);
BackendServicesRemoteConfigClient.enqueueWorker(this);
```