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

import java.util.List;

/**
 * App adapter
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    public static final int TYPE_PROTECTED = 100;
    public static final int TYPE_UNPROTECTED = 101;

    private static final String TAG = "AppAdapter";

    private Context context;

    private int type;

    private OnClickCallback mCallback;

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

    public AppAdapter(int type, List<AppInfo> list, OnClickCallback callback) {
        this.type = type;
        this.appInfoList = list;
        this.mCallback = callback;
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
                AppInfo appInfo = appInfoList.get(holder.getAdapterPosition());
                mCallback.onClick(appInfo);
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
        if (type == TYPE_PROTECTED) {
            holder.appCheckedView.setChecked(true);
        } else if (type == TYPE_UNPROTECTED) {
            holder.appCheckedView.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }

    public interface OnClickCallback {
        void onClick(AppInfo info);
    }
}
