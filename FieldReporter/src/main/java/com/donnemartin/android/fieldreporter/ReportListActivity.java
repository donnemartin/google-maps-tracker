package com.donnemartin.android.fieldreporter;

import android.support.v4.app.Fragment;

public class ReportListActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new ReportListFragment();
    }

}
