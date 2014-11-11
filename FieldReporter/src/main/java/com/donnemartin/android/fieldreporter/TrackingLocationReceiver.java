package com.donnemartin.android.fieldreporter;

import android.content.Context;
import android.location.Location;

public class TrackingLocationReceiver extends LocationReceiver {
    
    @Override
    protected void onLocationReceived(Context c, Location loc) {
        ReportManager.get(c).insertLocation(loc);
    }

}
