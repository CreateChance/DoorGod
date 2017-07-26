package com.createchance.doorgod.ui;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.createchance.doorgod.R;
import com.createchance.doorgod.adapter.IntrusionRecordDetailsAdapter;

public class IntrusionRecordDetailsActivity extends AppCompatActivity {

    private static final String TAG = "IntrusionRecordDetailsA";

    private RecyclerView mDetailList;
    private IntrusionRecordDetailsAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intrusion_record_details);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.settings_intru_rec_detail_title);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mDetailList = (RecyclerView) findViewById(R.id.intru_rec_details_list);
        mDetailList.setLayoutManager(new LinearLayoutManager(IntrusionRecordDetailsActivity.this));
        mAdapter = new IntrusionRecordDetailsAdapter(getExternalFilesDir(Environment.DIRECTORY_PICTURES));
        mDetailList.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
