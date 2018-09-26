/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.widget.Button;
import android.widget.Toast;

import com.johnnylambada.locationservice.LocationServiceConfiguration;
import com.johnnylambada.locationservice.LocationServiceController;

import java.text.DateFormat;
import java.util.Date;

/**
 */
public class LocationActivity extends BaseLocationActivity {
    private static final String TAG = "LocationActivity";

    private final LocationServiceController mController = LocationServiceController.INSTANCE;

    // UI elements.
    private Button mRequestLocationUpdatesButton;
    private Button mRemoveLocationUpdatesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        mRequestLocationUpdatesButton = findViewById(R.id.request_location_updates_button);
        mRemoveLocationUpdatesButton = findViewById(R.id.remove_location_updates_button);

        mRequestLocationUpdatesButton.setOnClickListener(__ -> {
            if (!checkPermissions()) {
                requestPermissions();
            } else {
                mController.requestLocationUpdates(this);
            }
        });

        mRemoveLocationUpdatesButton.setOnClickListener(__ -> mController.removeLocationUpdates(this));

        if (!mController.isConfigured()){
            mController.configure(
                    new LocationServiceConfiguration.Builder()
                            .appName(getString(R.string.app_name))
                            .packageName(BuildConfig.APPLICATION_ID)
//                            .notificationImportance(NotificationManager.IMPORTANCE_DEFAULT)
                            .build()
            );
        }

        mController
                .setNotification(getNotification())
                .attach(
                    this,
                    this,
                    location -> Toast.makeText(this, "!(" + location.getLatitude() + ", " + location.getLongitude() + ")",
                            Toast.LENGTH_SHORT).show(),
                    this::setButtonsState
                );

        // Check that the user hasn't revoked permissions by going to Settings.
        if (mController.isRequestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions();
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        setButtonsState(mController.isRequestingLocationUpdates(this));
    }


    @Override protected void permissionsWereGranted() {
        mController.requestLocationUpdates(this);
    }

    @Override protected void permissionsWereDenied() {
        setButtonsState(false);
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     * Note that this is Notification is eventually passed to the service and will be active
     * when the activity that created it is no longer active. Therefore it should NOT contain
     * any references to the enclosing activity itself.
     */
    private Notification getNotification() {
        // The PendingIntent to launch the activity.
        final PendingIntent activityPendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                new Intent(this, LocationActivity.class),
                0
        );

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                        mController.getStopLocationServicePendingIntent(getApplicationContext()))
                .setContentTitle(DateFormat.getDateTimeInstance().format(new Date()))
                .setContentText("Your text goes here")
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_launcher)
                .setTicker("A ticker here")
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(mController.getConfiguration().getChannel()); // Channel ID
        }

        return builder.build();
    }

    private void setButtonsState(boolean requestingLocationUpdates) {
        mRequestLocationUpdatesButton.setEnabled(!requestingLocationUpdates);
        mRemoveLocationUpdatesButton.setEnabled(requestingLocationUpdates);
    }
}
