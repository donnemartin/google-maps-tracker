package com.donnemartin.android.fieldreporter;

import android.content.Context;
import android.database.Cursor;

public class LocationListCursorLoader extends SQLiteCursorLoader {
    private long mReportId;
    
    public LocationListCursorLoader(Context c, long reportId) {
        super(c);
        mReportId = reportId;
    }

    @Override
    protected Cursor loadCursor() {
        return ReportManager.get(getContext()).queryLocationsForReport(mReportId);
    }
}