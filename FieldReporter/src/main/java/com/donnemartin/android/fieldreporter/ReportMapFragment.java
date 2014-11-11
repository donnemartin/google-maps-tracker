package com.donnemartin.android.fieldreporter;

import java.util.Date;

import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.donnemartin.android.fieldreporter.ReportDatabaseHelper.LocationCursor;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class ReportMapFragment extends SupportMapFragment implements LoaderCallbacks<Cursor> {
    private static final String ARG_REPORT_ID = "REPORT_ID";
    private static final int LOAD_LOCATIONS = 0;
    
    private GoogleMap mGoogleMap;
    private LocationCursor mLocationCursor;

    public static ReportMapFragment newInstance(long reportId) {
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        ReportMapFragment rf = new ReportMapFragment();
        rf.setArguments(args);
        return rf;
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // check for a Report ID as an argument, and find the report
        Bundle args = getArguments();
        if (args != null) {
            long reportId = args.getLong(ARG_REPORT_ID, -1);
            if (reportId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_LOCATIONS, args, this);
            }
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, parent, savedInstanceState);
        
        // stash a reference to the GoogleMap
        mGoogleMap = getMap();
        // show the user's location
        mGoogleMap.setMyLocationEnabled(true);

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        
        return v;
    }

    private void updateUI() {
        if (mGoogleMap == null || mLocationCursor == null)
            return;
        
        // set up an overlay on the map for this report's locations
        // create a polyline with all of the points
        PolylineOptions line = new PolylineOptions();
        // also create a LatLngBounds so we can zoom to fit
        LatLngBounds.Builder latLngBuilder = new LatLngBounds.Builder();
        // iterate over the locations
        mLocationCursor.moveToFirst();
        while (!mLocationCursor.isAfterLast()) {
            Location loc = mLocationCursor.getLocation();
            LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
            
            // if this is the first location, add a marker for it
            if (mLocationCursor.isFirst()) {
                String startDate = new Date(loc.getTime()).toString();
                MarkerOptions startMarkerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.report_start))
                    .snippet(getResources().getString(R.string.report_started_at_format, startDate));
                mGoogleMap.addMarker(startMarkerOptions);
            } else if (mLocationCursor.isLast()) {
                // if this is the last location, and not also the first, add a marker
                String endDate = new Date(loc.getTime()).toString();
                MarkerOptions finishMarkerOptions = new MarkerOptions()
                    .position(latLng)
                    .title(getResources().getString(R.string.report_finish))
                    .snippet(getResources().getString(R.string.report_finished_at_format, endDate));
                mGoogleMap.addMarker(finishMarkerOptions);
            }
            
            line.add(latLng);
            latLngBuilder.include(latLng);
            mLocationCursor.moveToNext();
        }
        // add the polyline to the map
        mGoogleMap.addPolyline(line);
        // make the map zoom to show the track, with some padding
        // use the size of the current display in pixels as a bounding box
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        // construct a movement instruction for the map camera
        CameraUpdate movement = CameraUpdateFactory.newLatLngBounds(latLngBuilder.build(),
                display.getWidth(), display.getHeight(), 15);
        mGoogleMap.moveCamera(movement);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new LocationListCursorLoader(getActivity(), args.getLong(ARG_REPORT_ID, -1));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mLocationCursor = (LocationCursor)cursor;
        updateUI();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // stop using the data
        mLocationCursor.close();
        mLocationCursor = null;
    }
    
}
