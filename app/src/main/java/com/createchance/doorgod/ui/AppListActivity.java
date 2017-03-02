package com.createchance.doorgod.ui;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.provider.Settings;
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

public class AppListActivity extends AppCompatActivity {

    private static final String TAG = "AppListActivity";

    public static final int CODE_REQUEST_PERMISSION = 100;

    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    private FloatingActionButton doneBtn;

    private List<AppInfo> mAppInfoList;

    private AppAdapter mAppAdapter;

    private HomeKeyWatcher mHomeKeyWatcher;

    private DoorGodService.ServiceBinder mService;

    private SharedPreferences mPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ENROLLED = "ENROLLED";

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = (DoorGodService.ServiceBinder) service;

            if (mService != null) {
                if (!isPatternEnrolled()) {
                    Toast.makeText(AppListActivity.this,
                            R.string.first_start_info, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(AppListActivity.this, SettingsActivity.class);
                    startActivity(intent);
                }

                mAppInfoList = mService.getAppList();
                RecyclerView recyclerView = (RecyclerView) findViewById(R.id.app_list_view);
                LinearLayoutManager layoutManager = new LinearLayoutManager(AppListActivity.this);
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

        // get prefs
        mPrefs = getSharedPreferences(LOCK_ENROLL_STATUS, MODE_PRIVATE);

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
                        Intent intent = new Intent(AppListActivity.this, SettingsActivity.class);
                        startActivity(intent);
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
        Intent intent = new Intent(AppListActivity.this, DoorGodService.class);
        startService(intent);
        bindService(intent, mConnection, BIND_AUTO_CREATE);

        // check if we have PACKAGE_USAGE_STATS permission.
        if (!checkIfGetPermission()) {
            showPermissionRequestDialog();
        }

        // watch for home key press event.
        mHomeKeyWatcher = new HomeKeyWatcher(this);
        mHomeKeyWatcher.setOnHomePressedListener(new HomeKeyWatcher.OnHomePressedListener() {
            @Override
            public void onHomePressed() {
                if (mAppAdapter.isConfigChanged()) {
                    Toast.makeText(AppListActivity.this,
                            R.string.toast_info_config_not_saved, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onHomeLongPressed() {
                // do nothing for now.
            }
        });
        mHomeKeyWatcher.startWatch();
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

        mHomeKeyWatcher.stopWatch();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case CODE_REQUEST_PERMISSION:
                if (!checkIfGetPermission()) {
                    Toast.makeText(AppListActivity.this,
                            R.string.toast_info_request_permission_failed, Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAppAdapter.isConfigChanged() && (keyCode == KeyEvent.KEYCODE_BACK)) {
            showConfigChangedDialog();

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showConfigChangedDialog() {
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
    }

    private void showPermissionRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.ic_warning_white_48dp)
                .setTitle(R.string.dialog_title_warning)
                .setCancelable(false)
                .setMessage(R.string.dialog_content_request_permission)
                .setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                        AppListActivity.this.startActivityForResult(intent, CODE_REQUEST_PERMISSION);
                    }
                })
                .setNegativeButton(R.string.dialog_action_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(AppListActivity.this,
                                R.string.toast_info_request_permission_failed, Toast.LENGTH_SHORT).show();
                    }
                });
        builder.create().show();
    }

    private boolean checkIfGetPermission() {
        AppOpsManager appOps = (AppOpsManager) this
                .getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats",
                android.os.Process.myUid(), this.getPackageName());
        return (mode == AppOpsManager.MODE_ALLOWED);
    }

    private boolean isPatternEnrolled() {
        return mPrefs.getBoolean(LOCK_ENROLLED, false);
    }
}
