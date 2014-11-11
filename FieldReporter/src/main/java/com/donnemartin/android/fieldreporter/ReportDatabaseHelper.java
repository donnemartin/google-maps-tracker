package com.donnemartin.android.fieldreporter;

import java.util.Date;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

public class ReportDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "reports.sqlite";
    private static final int VERSION = 1;
    
    private static final String TABLE_REPORT = "report";
    private static final String COLUMN_REPORT_ID = "_id";
    private static final String COLUMN_REPORT_START_DATE = "start_date";

    private static final String TABLE_LOCATION = "location";
    private static final String COLUMN_LOCATION_LATITUDE = "latitude";
    private static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
    private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_PROVIDER = "provider";
    private static final String COLUMN_LOCATION_REPORT_ID = "report_id";

    public ReportDatabaseHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create the "report" table
        db.execSQL("create table report (_id integer primary key autoincrement, start_date integer)");
        // create the "location" table
        db.execSQL("create table location (" +
                " timestamp integer, latitude real, longitude real, altitude real," +
                " provider varchar(100), report_id integer references report(_id))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // implement schema changes and data massage here when upgrading
    }
    
    public long insertReport(Report report) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_REPORT_START_DATE, report.getStartDate().getTime());
        return getWritableDatabase().insert(TABLE_REPORT, null, cv);
    }
    
    public long insertLocation(long reportId, Location location) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_LOCATION_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LOCATION_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_LOCATION_ALTITUDE, location.getAltitude());
        cv.put(COLUMN_LOCATION_TIMESTAMP, location.getTime());
        cv.put(COLUMN_LOCATION_PROVIDER, location.getProvider());
        cv.put(COLUMN_LOCATION_REPORT_ID, reportId);
        return getWritableDatabase().insert(TABLE_LOCATION, null, cv);
    }

    public ReportCursor queryReports() {
        // equivalent to "select * from report order by start_date asc"
        Cursor wrapped = getReadableDatabase().query(TABLE_REPORT,
                null, null, null, null, null, COLUMN_REPORT_START_DATE + " asc");
        return new ReportCursor(wrapped);
    }
    
    public ReportCursor queryReport(long id) {
        Cursor wrapped = getReadableDatabase().query(TABLE_REPORT,
                null, // all columns 
                COLUMN_REPORT_ID + " = ?", // look for a report ID
                new String[]{ String.valueOf(id) }, // with this value
                null, // group by
                null, // order by
                null, // having
                "1"); // limit 1 row
        return new ReportCursor(wrapped);
    }

    public LocationCursor queryLastLocationForReport(long reportId) {
        Cursor wrapped = getReadableDatabase().query(TABLE_LOCATION, 
                null, // all columns 
                COLUMN_LOCATION_REPORT_ID + " = ?", // limit to the given report
                new String[]{ String.valueOf(reportId) },
                null, // group by
                null, // having
                COLUMN_LOCATION_TIMESTAMP + " desc", // order by latest first
                "1"); // limit 1
        return new LocationCursor(wrapped);
    }

    public LocationCursor queryLocationsForReport(long reportId) {
        Cursor wrapped = getReadableDatabase().query(TABLE_LOCATION,
                null,
                COLUMN_LOCATION_REPORT_ID + " = ?", // limit to the given report
                new String[]{ String.valueOf(reportId) },
                null, // group by
                null, // having
                COLUMN_LOCATION_TIMESTAMP + " asc"); // order by timestamp
        return new LocationCursor(wrapped);
    }

    /**
     * A convenience class to wrap a cursor that returns rows from the "report" table.
     * The {@link getReport()} method will give you a Report instance representing the current row.
     */
    public static class ReportCursor extends CursorWrapper {
        
        public ReportCursor(Cursor c) {
            super(c);
        }
        
        /**
         * Returns a Report object configured for the current row, or null if the current row is invalid.
         */
        public Report getReport() {
            if (isBeforeFirst() || isAfterLast())
                return null;
            Report report = new Report();
            report.setId(getLong(getColumnIndex(COLUMN_REPORT_ID)));
            report.setStartDate(new Date(getLong(getColumnIndex(COLUMN_REPORT_START_DATE))));
            return report;
        }
    }
    
    public static class LocationCursor extends CursorWrapper {
        
        public LocationCursor(Cursor c) {
            super(c);
        }
        
        public Location getLocation() {
            if (isBeforeFirst() || isAfterLast())
                return null;
            // first get the provider out so we can use the constructor
            String provider = getString(getColumnIndex(COLUMN_LOCATION_PROVIDER));
            Location loc = new Location(provider);
            // populate the remaining properties
            loc.setLongitude(getDouble(getColumnIndex(COLUMN_LOCATION_LONGITUDE)));
            loc.setLatitude(getDouble(getColumnIndex(COLUMN_LOCATION_LATITUDE)));
            loc.setAltitude(getDouble(getColumnIndex(COLUMN_LOCATION_ALTITUDE)));
            loc.setTime(getLong(getColumnIndex(COLUMN_LOCATION_TIMESTAMP)));
            return loc;
        }
    }

}
