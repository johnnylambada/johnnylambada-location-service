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
implementation "com.github.johnnylambada:fibamlscan:0.1.2"
```

### Review the sample app to see how to integrate the library.

1. Review the [PreviewActivity](https://github.com/johnnylambada/fibamlscan/blob/master/app/src/main/java/fibamlscan/app/PreviewActivity.java).
2. Review the [PreviewActivity's layout](https://github.com/johnnylambada/fibamlscan/blob/master/library/src/main/res/layout/activity_live_preview.xml).
