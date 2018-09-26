package com.johnnylambada.locationservice;

import android.app.NotificationManager;

public class LocationServiceConfiguration {
    /**
     * The name of the channel for notifications.
     */
    private final String appName;
    private final String channel;
    private final String packageName;
    private final int notificationImportance;
    private final int intervalMs;

    private LocationServiceConfiguration(
            String appName,
            String channel,
            String packageName,
            int notificationImportance,
            int intervalMs
    ){
        this.appName = appName;
        this.channel = channel;
        this.packageName = packageName;
        this.notificationImportance = notificationImportance;
        this.intervalMs = intervalMs;
    }

    public String getAppName(){
        return appName;
    }

    public String getChannel() {
        return channel;
    }

    public int getNotificationImportance() {
        return notificationImportance;
    }

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public int getIntervalMs() {
        return intervalMs;
    }

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    public int getFastestIntervalMs() {
        return intervalMs / 2;
    }

    public String getActionBroadcast(){
        return packageName+".broadcast";
    }

    public static class Builder {
        private String appName;
        private String channel;
        private String packageName;
        private int intervalMs = 10000; // 10 seconds
        private int notificationImportance = -1000;//NotificationManager.IMPORTANCE_UNSPECIFIED;;

        public Builder appName(String appName){
            this.appName = appName;
            return this;
        }

        public Builder channel(String channel){
            this.channel = channel;
            return this;
        }

        public Builder packageName(String packageName){
            this.packageName = packageName;
            return this;
        }

        public Builder notificationImportance(int notificationImportance){
            this.notificationImportance = notificationImportance;
            return this;
        }

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        public Builder intervalMs(int intervalMs){
            this.intervalMs = intervalMs;
            return this;
        }

        public LocationServiceConfiguration build(){
            if (appName==null){
                throw new IllegalArgumentException("appName can't be null");
            }
            if (packageName==null){
                throw new IllegalArgumentException("packageName can't be null");
            }
            return new LocationServiceConfiguration(
                    appName,
                    channel==null
                            ? "channel_01"
                            : channel,
                    packageName,
                    notificationImportance==-1000
                        ? 3//NotificationManager.IMPORTANCE_DEFAULT
                        : notificationImportance,
                    intervalMs
            );
        }
    }
}
