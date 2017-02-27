package com.createchance.doorgod.ui;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.createchance.doorgod.R;
import com.createchance.doorgod.adapter.AppAdapter;
import com.createchance.doorgod.adapter.AppInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private DrawerLayout drawerLayout;

    private NavigationView navigationView;

    private FloatingActionButton doneBtn;

    private PackageManager mPm;

    private List<AppInfo> mAppInfoList = new ArrayList<>();

    private AppAdapter mAppAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPm = getPackageManager();

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
                Snackbar.make(view, getString(R.string.snack_info), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.snack_undo), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Invert Selections
                            }
                        }).show();
            }
        });

        initAppList();
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.app_list_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        mAppAdapter = new AppAdapter(mAppInfoList);
        recyclerView.setAdapter(mAppAdapter);
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

    private void initAppList() {
        List<ApplicationInfo> applications = mPm
                .getInstalledApplications(PackageManager.GET_META_DATA);
        Collections.sort(applications,
                new ApplicationInfo.DisplayNameComparator(mPm));

        for (ApplicationInfo application:applications) {
            AppInfo info = new AppInfo();
            info.setAppIcon(application.loadIcon(mPm));
            info.setAppName((String) application.loadLabel(mPm));
            info.setAppPackageName(application.packageName);
            mAppInfoList.add(info);
        }
    }
}
