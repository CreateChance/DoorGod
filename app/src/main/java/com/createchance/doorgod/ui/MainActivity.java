package com.createchance.doorgod.ui;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.adapter.AppAdapter;
import com.createchance.doorgod.adapter.AppInfo;
import com.createchance.doorgod.service.DoorGodService;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    private FloatingActionButton doneBtn;

    private List<AppInfo> mAppInfoList;

    private AppAdapter mAppAdapter;

    private DoorGodService.ServiceBinder mService;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            if (mService != null) {
                mAppInfoList = mService.getAppList();
                RecyclerView recyclerView = (RecyclerView) findViewById(R.id.app_list_view);
                LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
                recyclerView.setLayoutManager(layoutManager);
                mAppAdapter = new AppAdapter(mAppInfoList, mService);
                recyclerView.setAdapter(mAppAdapter);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.nav_settings:
                        // do settings.
                        break;
                    case R.id.nav_about:
                        // show about info.
                        break;
                    default:
                        break;
                }

                drawerLayout.closeDrawers();
                return true;
            }
        });

        doneBtn = (FloatingActionButton) findViewById(R.id.fab_done);
        doneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.addProtectedApp(mAppAdapter.getProtectedAppList());
                Snackbar.make(view, getString(R.string.snack_info), Snackbar.LENGTH_LONG)
                        .show();
            }
        });

        // start and bind service.
        Intent intent = new Intent(MainActivity.this, DoorGodService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindService(mConnection);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAppAdapter.isConfigChanged() && (keyCode == KeyEvent.KEYCODE_BACK ||
                keyCode == KeyEvent.KEYCODE_HOME)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_warning_white_48dp)
                    .setTitle(R.string.dialog_title_warning)
                    .setCancelable(false)
                    .setMessage(R.string.dialog_content_config_changed)
                    .setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // User choose to discard changes, so we just quit.
                            dialog.dismiss();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.dialog_action_no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
