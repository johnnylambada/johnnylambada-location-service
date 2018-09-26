package com.johnnylambada.locationservice;

import android.app.Notification;
import android.app.PendingIntent;
import android.arch.lifecycle.DefaultLifecycleObserver;
import android.arch.lifecycle.LifecycleOwner;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static com.johnnylambada.locationservice.LocationService.EXTRA_STARTED_FROM_NOTIFICATION;

public enum LocationServiceController {
    INSTANCE;

    public static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";
    private final LifecycleObserver lifecycleObserver = new LifecycleObserver();
    private final Map<LifecycleOwner,Client> clients = new HashMap<>();
    private Notification notification;

    private LocationServiceConfiguration configuration;

    // region configuration
    public void configure(LocationServiceConfiguration configuration){
        if (configuration==null){
            throw new IllegalArgumentException("configuration can't be null");
        }
        if (this.configuration!=null){
            throw new IllegalStateException("LocationServiceController can only be configured once");
        }
        this.configuration = configuration;
    }

    public boolean isConfigured(){
        return configuration !=null;
    }

    public LocationServiceConfiguration getConfiguration() {
        return configuration;
    }
    // endregion

    // region notification

    public @NonNull Notification getNotification() {
        if (notification==null){
            throw new IllegalStateException("notification must be set before it's used");
        }
        return notification;
    }

    public LocationServiceController setNotification(Notification notification) {
        this.notification = notification;
        return this;
    }

    // endregion

    // region clients
    public LocationServiceController attach(
            LifecycleOwner lifecycleOwner,
            Context context,
            LocationObserver locationObserver,
            RequestingLocationUpdatesObserver requestingLocationUpdatesObserver
    ){
        final Client client = new Client(
                lifecycleOwner,
                context,
                locationObserver,
                requestingLocationUpdatesObserver
        );
        clients.put(lifecycleOwner, client);
        lifecycleOwner.getLifecycle().addObserver(lifecycleObserver);
        return this;
    }

    public void requestLocationUpdates(LifecycleOwner lifecycleOwner){
        clients.get(lifecycleOwner).mService.requestLocationUpdates();
    }

    public void removeLocationUpdates(LifecycleOwner lifecycleOwner){
        clients.get(lifecycleOwner).mService.removeLocationUpdates();
    }

    public Location getLocation(LifecycleOwner lifecycleOwner){
        return clients.get(lifecycleOwner).mService.getLocation();
    }

    private class Client {
        private final LifecycleOwner lifecycleOwner;
        private final Context context;
        private final LocationObserver locationObserver;
        private final RequestingLocationUpdatesObserver requestingLocationUpdatesObserver;

        // A reference to the service used to get location updates.
        private LocationService mService = null;

        // Monitors the state of the connection to the service.
        private final ServiceConnection mServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
                mService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
            }
        };

        private final BroadcastReceiver mLocationReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                Location location = intent.getParcelableExtra(LocationService.EXTRA_LOCATION);
                if (location != null) {
                    locationObserver.onLocation(location);
                }
            }
        };

        private final SharedPreferences.OnSharedPreferenceChangeListener mPrefsChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if (key.equals(KEY_REQUESTING_LOCATION_UPDATES)) {
                    requestingLocationUpdatesObserver.isRequestingLocationUpdates(isRequestingLocationUpdates(context));
                }
            }
        };

        Client(LifecycleOwner lifecycleOwner,
               Context context,
               LocationObserver locationObserver,
               RequestingLocationUpdatesObserver requestingLocationUpdatesObserver
        ){
            this.lifecycleOwner = lifecycleOwner;
            this.context = context;
            this.locationObserver = locationObserver;
            this.requestingLocationUpdatesObserver = requestingLocationUpdatesObserver;
        }
    }

    private class LifecycleObserver implements DefaultLifecycleObserver {
        @Override public void onStart(@NonNull LifecycleOwner owner) {
            Log.i("LSC","onStart");
            final Client client = clients.get(owner);
            // Bind to the service. If the service is in foreground mode, this signals to the service
            // that since this activity is in the foreground, the service can exit foreground mode.
            client.context.bindService(
                    new Intent(client.context, LocationService.class),
                    client.mServiceConnection,
                    Context.BIND_AUTO_CREATE);
            PreferenceManager.getDefaultSharedPreferences(client.context)
                    .registerOnSharedPreferenceChangeListener(client.mPrefsChangeListener);
        }

        @Override public void onStop(@NonNull LifecycleOwner owner) {
            Log.i("LSC","onStop");
            final Client client = clients.get(owner);
            if (client.mService!=null) {
                // Unbind from the service. This signals to the service that this activity is no longer
                // in the foreground, and the service can respond by promoting itself to a foreground
                // service.
                client.context.unbindService(client.mServiceConnection);
            }
            PreferenceManager.getDefaultSharedPreferences(client.context)
                    .unregisterOnSharedPreferenceChangeListener(client.mPrefsChangeListener);
        }

        @Override public void onResume(@NonNull LifecycleOwner owner) {
            Log.i("LSC","onResume");
            final Client client = clients.get(owner);
            LocalBroadcastManager.getInstance(client.context)
                    .registerReceiver(client.mLocationReceiver,
                    new IntentFilter(getConfiguration().getActionBroadcast()));
        }

        @Override public void onPause(@NonNull LifecycleOwner owner) {
            Log.i("LSC","onPause");
            final Client client = clients.get(owner);
            LocalBroadcastManager.getInstance(client.context).unregisterReceiver(client.mLocationReceiver);
        }

        @Override public void onDestroy(@NonNull LifecycleOwner owner) {
            Log.i("LSC","onDestroy");
            clients.remove(owner);
        }
    }
    // endregion

    // region shared preference
    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    public boolean isRequestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    public void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }
    // endregion

    // region notification support
    /**
     * returns a PendingIntent that will stop the LocationService
     * @param context
     * @return
     */
    public PendingIntent getStopLocationServicePendingIntent(Context context){
        Intent intent = new Intent(context.getApplicationContext(), LocationService.class);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        return servicePendingIntent;
    }
    // endregion
}
