package com.createchance.doorgod.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.createchance.doorgod.R;

import java.util.List;

/**
 * App adapter
 */

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private Context context;

    private List<AppInfo> appInfoList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView appIconView;
        TextView appNameView;
        TextView appPackageNameView;

        public ViewHolder(View view) {
            super(view);

            cardView = (CardView) view;
            appIconView = (ImageView) view.findViewById(R.id.app_icon);
            appNameView = (TextView) view.findViewById(R.id.app_name);
            appPackageNameView = (TextView) view.findViewById(R.id.app_package_name);
        }
    }

    public AppAdapter(List<AppInfo> list) {
        this.appInfoList = list;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (context == null) {
            context = parent.getContext();
        }

        View view = LayoutInflater.from(context).inflate(R.layout.app_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        AppInfo info = appInfoList.get(position);
        holder.appIconView.setImageDrawable(info.getAppIcon());
        holder.appNameView.setText(info.getAppName());
        holder.appPackageNameView.setText(info.getAppPackageName());
    }

    @Override
    public int getItemCount() {
        return appInfoList.size();
    }
}
