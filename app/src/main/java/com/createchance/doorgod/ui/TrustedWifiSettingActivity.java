package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RadioButton;

import com.createchance.doorgod.R;
import com.createchance.doorgod.adapter.TrustedWifiListAdapter;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LogUtil;

import java.util.List;

public class TrustedWifiSettingActivity extends AppCompatActivity implements View.OnClickListener, TrustedWifiListAdapter.OnClick {

    private static final String TAG = "TrustedWifiSettingActiv";

    private ListView mWifiList;
    private TrustedWifiListAdapter mAdapter;

    private List<String> savedWifi;

    private DoorGodService.ServiceBinder mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            if (mService != null) {
                savedWifi = mService.getSavedWifiList();

                mAdapter = new TrustedWifiListAdapter(savedWifi, mService.getTrustedWifi(),
                        mService.getConnectedWifiSsid(), TrustedWifiSettingActivity.this,
                        TrustedWifiSettingActivity.this);
                mWifiList.setAdapter(mAdapter);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trusted_wifi_setting);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(R.string.settings_trust_wifi);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mWifiList = (ListView) findViewById(R.id.wifi_list);

        Intent intent = new Intent(TrustedWifiSettingActivity.this, DoorGodService.class);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
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

    @Override
    public void onClick(View view) {
        String ssid = ((RadioButton)view).getText().toString();

        LogUtil.d(TAG, ssid);
    }

    @Override
    public void onClick(String ssid, boolean trusted) {
        if (trusted) {
            mService.setTrustedWifi(ssid);
        } else {
            mService.removeTrustedWifi(ssid);
        }
    }
}
