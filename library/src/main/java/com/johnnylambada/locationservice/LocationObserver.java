package com.johnnylambada.locationservice;

import android.location.Location;

public interface LocationObserver {
    void onLocation(Location location);
}
