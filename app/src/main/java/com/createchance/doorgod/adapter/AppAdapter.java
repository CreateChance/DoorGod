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

import java.util.List;

/**
 * App adapter
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private static final String TAG = "AppAdapter";

    private Context context;

    private DoorGodService.ServiceBinder mService;

    private List<String> protectedAppList;

    private List<AppInfo> appInfoList;

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
        this.mService = service;
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
                    protectedAppList.add(pkgName);
                } else {
                    LogUtil.v(TAG, "Ready to remove: " + pkgName);
                    protectedAppList.remove(pkgName);
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
        } else {
            holder.appCheckedView.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public List<String> getProtectedAppList() {
        return protectedAppList;
    }

    public boolean isConfigChanged() {
        // compare to saved configuration.
        List<String> appList = mService.getProtectedAppList();
        if (appList.size() == protectedAppList.size()) {
            for (int i = 0; i < appList.size(); i++) {
                if (!appList.get(i).equals(protectedAppList.get(i))) {
                    return true;
                }
            }
        } else {
            return true;
        }

        return false;
    }
}
