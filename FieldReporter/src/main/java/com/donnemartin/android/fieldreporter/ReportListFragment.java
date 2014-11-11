package com.donnemartin.android.fieldreporter;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.donnemartin.android.fieldreporter.ReportDatabaseHelper.ReportCursor;

public class ReportListFragment extends ListFragment implements LoaderCallbacks<Cursor> {
    private static final int REQUEST_NEW_REPORT = 0;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // initialize the loader to load the list of reports
        getLoaderManager().initLoader(0, null, this);
    }
    
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // we only ever load the reports, so assume this is the case
        return new ReportListCursorLoader(getActivity());
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // create an adapter to point at this cursor
        ReportCursorAdapter adapter = new ReportCursorAdapter(getActivity(), (ReportCursor)cursor);
        setListAdapter(adapter);
    }
    
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // stop using the cursor (via the adapter)
        setListAdapter(null);
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.report_list_options, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_item_new_report:
            Intent i = new Intent(getActivity(), ReportActivity.class);
            startActivityForResult(i, REQUEST_NEW_REPORT);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (REQUEST_NEW_REPORT == requestCode) {
            // restart the loader to get any new report available
            getLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // the id argument will be the Report ID; CursorAdapter gives us this for free
        Intent i = new Intent(getActivity(), ReportActivity.class);
        i.putExtra(ReportActivity.EXTRA_REPORT_ID, id);
        startActivity(i);
    }

    private static class ReportListCursorLoader extends SQLiteCursorLoader {

        public ReportListCursorLoader(Context context) {
            super(context);
        }

        @Override
        protected Cursor loadCursor() {
            // query the list of reports
            return ReportManager.get(getContext()).queryReports();
        }
        
    }
    
    private static class ReportCursorAdapter extends CursorAdapter {
        
        private ReportCursor mReportCursor;
        
        public ReportCursorAdapter(Context context, ReportCursor cursor) {
            super(context, cursor, 0);
            mReportCursor = cursor;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // use a layout inflater to get a row view
            LayoutInflater inflater = 
                    (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // get the report for the current row
            Report report = mReportCursor.getReport();
            
            // set up the start date text view
            TextView startDateTextView = (TextView)view;
            startDateTextView.setText("Report at " + report.getStartDate());
        }
        
    }
}
