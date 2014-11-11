package com.donnemartin.android.fieldreporter;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.donnemartin.android.fieldreporter.ReportDatabaseHelper.LocationCursor;
import com.donnemartin.android.fieldreporter.ReportDatabaseHelper.ReportCursor;

public class ReportManager {
    private static final String TAG = "ReportManager";

    private static final String PREFS_FILE = "reports";
    private static final String PREF_CURRENT_REPORT_ID = "ReportManager.currentReportId";

    public static final String ACTION_LOCATION = "com.donnemartin.android.fieldreporter.ACTION_LOCATION";
    
    private static final String TEST_PROVIDER = "TEST_PROVIDER";
    
    private static ReportManager sReportManager;
    private Context mAppContext;
    private LocationManager mLocationManager;
    private ReportDatabaseHelper mHelper;
    private SharedPreferences mPrefs;
    private long mCurrentReportId;
    
    private ReportManager(Context appContext) {
        mAppContext = appContext;
        mLocationManager = (LocationManager)mAppContext.getSystemService(Context.LOCATION_SERVICE);
        mHelper = new ReportDatabaseHelper(mAppContext);
        mPrefs = mAppContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        mCurrentReportId = mPrefs.getLong(PREF_CURRENT_REPORT_ID, -1);
    }
    
    public static ReportManager get(Context c) {
        if (sReportManager == null) {
            // we use the application context to avoid leaking activities
            sReportManager = new ReportManager(c.getApplicationContext());
        }
        return sReportManager;
    }

    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
        return PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
    }

    public void startLocationUpdates() {
        String provider = LocationManager.GPS_PROVIDER;
        // if we have the test provider and it's enabled, use it
        if (mLocationManager.getProvider(TEST_PROVIDER) != null && 
                mLocationManager.isProviderEnabled(TEST_PROVIDER)) {
            provider = TEST_PROVIDER;
        }
        Log.d(TAG, "Using provider " + provider);

        // get the last known location and broadcast it if we have one
        Location lastKnown = mLocationManager.getLastKnownLocation(provider);
        if (lastKnown != null) {
            // reset the time to now
            lastKnown.setTime(System.currentTimeMillis());
            broadcastLocation(lastKnown);
        }
        // start updates from the location manager
        PendingIntent pi = getLocationPendingIntent(true);
        mLocationManager.requestLocationUpdates(provider, 0, 0, pi);
    }
    
    public void stopLocationUpdates() {
        PendingIntent pi = getLocationPendingIntent(false);
        if (pi != null) {
            mLocationManager.removeUpdates(pi);
            pi.cancel();
        }
    }
    
    public boolean isTrackingReport() {
        return getLocationPendingIntent(false) != null;
    }
    
    public boolean isTrackingReport(Report report) {
        return report != null && report.getId() == mCurrentReportId;
    }
    
    private void broadcastLocation(Location location) {
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        mAppContext.sendBroadcast(broadcast);
    }
    
    public Report startNewReport() {
        // insert a report into the db
        Report report = insertReport();
        // start tracking the report
        startTrackingReport(report);
        return report;
    }
    
    public void startTrackingReport(Report report) {
        // keep the ID
        mCurrentReportId = report.getId();
        // store it in shared preferences
        mPrefs.edit().putLong(PREF_CURRENT_REPORT_ID, mCurrentReportId).commit();
        // start location updates
        startLocationUpdates();
    }
    
    public void stopReport() {
        stopLocationUpdates();
        mCurrentReportId = -1;
        mPrefs.edit().remove(PREF_CURRENT_REPORT_ID).commit();
    }
    
    private Report insertReport() {
        Report report = new Report();
        report.setId(mHelper.insertReport(report));
        return report;
    }

    public ReportCursor queryReports() {
        return mHelper.queryReports();
    }
    
    public Report getReport(long id) {
        Report report = null;
        ReportCursor cursor = mHelper.queryReport(id);
        cursor.moveToFirst();
        // if we got a row, get a report
        if (!cursor.isAfterLast())
            report = cursor.getReport();
        cursor.close();
        return report;
    }

    public void insertLocation(Location loc) {
        if (mCurrentReportId != -1) {
            mHelper.insertLocation(mCurrentReportId, loc);
        } else {
            Log.e(TAG, "Location received with no tracking report; ignoring.");
        }
    }
    
    public Location getLastLocationForReport(long reportId) {
        Location location = null;
        LocationCursor cursor = mHelper.queryLastLocationForReport(reportId);
        cursor.moveToFirst();
        // if we got a row, get a location
        if (!cursor.isAfterLast())
            location = cursor.getLocation();
        cursor.close();
        return location;
    }

    public LocationCursor queryLocationsForReport(long reportId) {
        return mHelper.queryLocationsForReport(reportId);
    }
}
