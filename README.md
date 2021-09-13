# Backend-Services-Android

Android client for https://github.com/doorbash/backend-services

## Download

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.doorbash:backend-services-android:-SNAPSHOT'
}
```

## Usage
```java
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
```