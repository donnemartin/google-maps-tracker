package com.donnemartin.android.fieldreporter;

import android.content.Context;

class ReportLoader extends DataLoader<Report> {
    private long mReportId;
    
    public ReportLoader(Context context, long reportId) {
        super(context);
        mReportId = reportId;
    }
    
    @Override
    public Report loadInBackground() {
        return ReportManager.get(getContext()).getReport(mReportId);
    }
}