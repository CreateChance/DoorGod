package com.createchance.doorgod.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.createchance.doorgod.R;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * App adapter
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private static final String TAG = "AppAdapter";

    private Context context;

    private List<String> protectedAppList;

    private List<AppInfo> appInfoList;

    private List<String> addedApp = new ArrayList<>();
    private List<String> removedApp = new ArrayList<>();

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView appIconView;
        TextView appNameView;
        TextView appPackageNameView;
        CheckBox appCheckedView;

        public ViewHolder(View view) {
            super(view);

            cardView = (CardView) view;
            appIconView = (ImageView) view.findViewById(R.id.app_icon);
            appNameView = (TextView) view.findViewById(R.id.app_name);
            appPackageNameView = (TextView) view.findViewById(R.id.app_package_name);
            appCheckedView = (CheckBox) view.findViewById(R.id.app_checked);
        }
    }

    public AppAdapter(List<AppInfo> list, DoorGodService.ServiceBinder service) {
        this.appInfoList = list;
        this.protectedAppList = service.getProtectedAppList();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }

        View view = LayoutInflater.from(context).inflate(R.layout.app_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.appCheckedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pkgName = appInfoList.get(holder.getAdapterPosition()).getAppPackageName();
                if (((CheckBox)view).isChecked()) {
                    LogUtil.v(TAG, "Ready to add: " + pkgName);
                    addedApp.add(pkgName);
                } else {
                    LogUtil.v(TAG, "Ready to remove: " + pkgName);
                    removedApp.add(pkgName);
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppInfo info = appInfoList.get(position);
        holder.appIconView.setImageDrawable(info.getAppIcon());
        holder.appNameView.setText(info.getAppName());
        holder.appPackageNameView.setText(info.getAppPackageName());
        if (protectedAppList.contains(info.getAppPackageName())) {
            holder.appCheckedView.setChecked(true);
        }
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public List<String> getAddedAppList() {
        return addedApp;
    }

    public List<String> getRemovedAppList() {
        return removedApp;
    }
}
