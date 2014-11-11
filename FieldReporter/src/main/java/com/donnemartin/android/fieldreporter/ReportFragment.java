package com.donnemartin.android.fieldreporter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class ReportFragment extends Fragment {
    private static final String TAG = "ReportFragment";
    private static final String ARG_REPORT_ID = "REPORT_ID";
    private static final int LOAD_REPORT = 0;
    private static final int LOAD_LOCATION = 1;
    
    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {

        @Override
        protected void onLocationReceived(Context context, Location loc) {
            if (!mReportManager.isTrackingReport(mReport))
                return;
            mLastLocation = loc;
            if (isVisible()) 
                updateUI();
        }
        
        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
        
    };
    
    private ReportManager mReportManager;
    
    private Report mReport;
    private Location mLastLocation;

    private Button mStartButton, mStopButton, mMapButton;
    private TextView mStartedTextView, mLatitudeTextView, 
        mLongitudeTextView, mAltitudeTextView, mDurationTextView;
    
    public static ReportFragment newInstance(long reportId) {
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        ReportFragment rf = new ReportFragment();
        rf.setArguments(args);
        return rf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mReportManager = ReportManager.get(getActivity());

        // check for a Report ID as an argument, and find the report
        Bundle args = getArguments();
        if (args != null) {
            long reportId = args.getLong(ARG_REPORT_ID, -1);
            if (reportId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_REPORT, args, new ReportLoaderCallbacks());
                lm.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        
        mStartedTextView = (TextView)view.findViewById(R.id.report_startedTextView);
        mLatitudeTextView = (TextView)view.findViewById(R.id.report_latitudeTextView);
        mLongitudeTextView = (TextView)view.findViewById(R.id.report_longitudeTextView);
        mAltitudeTextView = (TextView)view.findViewById(R.id.report_altitudeTextView);
        mDurationTextView = (TextView)view.findViewById(R.id.report_durationTextView);
        
        mStartButton = (Button)view.findViewById(R.id.report_startButton);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReport == null) {
                    mReport = mReportManager.startNewReport();
                } else {
                    mReportManager.startTrackingReport(mReport);
                }
                updateUI();
            }
        });
        
        mStopButton = (Button)view.findViewById(R.id.report_stopButton);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mReportManager.stopReport();
                updateUI();
            }
        });
        
        mMapButton = (Button)view.findViewById(R.id.report_mapButton);
        mMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), ReportMapActivity.class);
                i.putExtra(ReportMapActivity.EXTRA_REPORT_ID, mReport.getId());
                startActivity(i);
            }
        });
        
        updateUI();
        
        return view;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mLocationReceiver, 
                new IntentFilter(ReportManager.ACTION_LOCATION));
    }
    
    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mLocationReceiver);
        super.onStop();
    }
    
    private void updateUI() {
        boolean started = mReportManager.isTrackingReport();
        boolean trackingThisReport = mReportManager.isTrackingReport(mReport);
        
        if (mReport != null)
            mStartedTextView.setText(mReport.getStartDate().toString());
        
        int durationSeconds = 0;
        if (mReport != null && mLastLocation != null) {
            durationSeconds = mReport.getDurationSeconds(mLastLocation.getTime());
            mLatitudeTextView.setText(Double.toString(mLastLocation.getLatitude()));
            mLongitudeTextView.setText(Double.toString(mLastLocation.getLongitude()));
            mAltitudeTextView.setText(Double.toString(mLastLocation.getAltitude()));
            mMapButton.setEnabled(true);
        } else {
            mMapButton.setEnabled(false);
        }
        mDurationTextView.setText(Report.formatDuration(durationSeconds));
        
        mStartButton.setEnabled(!started);
        mStopButton.setEnabled(started && trackingThisReport);
    }
    
    private class ReportLoaderCallbacks implements LoaderCallbacks<Report> {
        
        @Override
        public Loader<Report> onCreateLoader(int id, Bundle args) {
            return new ReportLoader(getActivity(), args.getLong(ARG_REPORT_ID));
        }

        @Override
        public void onLoadFinished(Loader<Report> loader, Report report) {
            mReport = report;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Report> loader) {
            // do nothing
        }
    }

    private class LocationLoaderCallbacks implements LoaderCallbacks<Location> {
        
        @Override
        public Loader<Location> onCreateLoader(int id, Bundle args) {
            return new LastLocationLoader(getActivity(), args.getLong(ARG_REPORT_ID));
        }

        @Override
        public void onLoadFinished(Loader<Location> loader, Location location) {
            mLastLocation = location;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Location> loader) {
            // do nothing
        }
    }
}
