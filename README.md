# johnnylambada-location-service

This library is based on the [Google Samples LocationUpdatesForgroundService](https://github.com/googlesamples/android-play-location/tree/master/LocationUpdatesForegroundService). 

It allows you to set up an Android Service that will report location (as in GPS) to the app. If the user bacgrounds the app, the service will create a notification and continue to provide the app with location.

## Using this library

The following steps enable you to use this library.

### Set up jitpack.io for your project

Refer to [jitpack's documentation](https://jitpack.io/) for instructions.

### Add the library to your project

Add the following lines to your `app/build.gradle` dependencies:

```groovy
// https://github.com/johnnylambada/fibamlscan
implementation "com.github.johnnylambada:johnnylambada-location-service:0.0.1"
```

### Set up the service using the `LocationServiceController`

```java
    private final LocationServiceController mController = LocationServiceController.INSTANCE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ...

        if (!mController.isConfigured()){
            mController.configure(
                    new LocationServiceConfiguration.Builder()
                            .appName(getString(R.string.app_name))
                            .packageName(BuildConfig.APPLICATION_ID)
                            .build()
            );
        }

        mController
                .setNotification(getNotification(this,mController))
                .attach(
                    this, // LifecycleOwner
                    this, // Context
                    location -> Toast.makeText(this, "!(" + location.getLatitude() + ", " + location.getLongitude() + ")",
                            Toast.LENGTH_SHORT).show(),
                    this::setButtonsState
                );

        ...
    }

```

### Provide a Notification that has no references to the enclosing Activity

```java
    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     * Note that this is Notification is eventually passed to the service and will be active
     * when the activity that created it is no longer active. Therefore it should NOT contain
     * any references to the enclosing activity itself.
     */
    private static Notification getNotification(Context context, LocationServiceController controller) {
        final Context applicationContext = context.getApplicationContext();
        // The PendingIntent to launch the activity.
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(
                applicationContext,
                0,
                new Intent(applicationContext, LocationActivity.class),
                0
        );

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext)
                .addAction(R.drawable.ic_launch, applicationContext.getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, applicationContext.getString(R.string.remove_location_updates),
                        controller.getStopLocationServicePendingIntent(applicationContext))
                .setContentTitle(DateFormat.getDateTimeInstance().format(new Date()))
                .setContentText("Your text goes here")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("A ticker here")
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(controller.getConfiguration().getChannel()); // Channel ID
        }

        return builder.build();
    }
```

### Review the sample app to see how to integrate the library.

1. Review the [MainActivity](https://github.com/johnnylambada/johnnylambada-location-service/blob/master/app/src/main/java/app/LocationActivity.java), it shows you how to use the service from your activity.
2. Review the [BaseLocationActivity](https://github.com/johnnylambada/johnnylambada-location-service/blob/master/app/src/main/java/app/BaseLocationActivity.java). It has the code necessary to make the permissions work. You may have another permissions approach.
