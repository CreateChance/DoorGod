package com.createchance.doorgod.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.createchance.doorgod.R;
import com.createchance.doorgod.util.LogUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by gaochao on 26/07/2017.
 */

public class IntrusionRecordDetailsAdapter extends RecyclerView.Adapter<IntrusionRecordDetailsAdapter.ViewHolder> {

    private static final String TAG = "IntrusionRecordAdapter";

    private List<File> mPictureList;

    private Context mContext;

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView content;

        public ViewHolder(View view) {
            super(view);

            title = (TextView) view.findViewById(R.id.intru_details_title);
            content = (ImageView) view.findViewById(R.id.intru_details_content);
        }
    }

    public IntrusionRecordDetailsAdapter(File folder) {
        mPictureList = new ArrayList<>(Arrays.asList(folder.listFiles()));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }

        View view = LayoutInflater.from(mContext).inflate(R.layout.intru_details_item, parent, false);
        final IntrusionRecordDetailsAdapter.ViewHolder holder = new IntrusionRecordDetailsAdapter.ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(IntrusionRecordDetailsAdapter.ViewHolder holder, final int position) {
        LogUtil.d(TAG, mPictureList.get(position).getAbsolutePath());
        String time = getDateFromMills(mPictureList.get(position).getName().split("_")[0]);
        String appName = mPictureList.get(position).getName().split("_")[1].substring(0, mPictureList.get(position).getName().split("_")[1].lastIndexOf("."));
        holder.title.setText(mContext.getString(R.string.settings_intru_rec_detail_item_title, time, appName));
        Glide.with(mContext).load(mPictureList.get(position).getAbsoluteFile()).into(holder.content);
        holder.content.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showConfigChangedDialog(mPictureList.get(position));
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return mPictureList.size();
    }

    private void showConfigChangedDialog(final File picture) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(R.drawable.ic_warning_white_48dp)
                .setTitle(R.string.settings_intru_rec_detail_dialog_title)
                .setPositiveButton(R.string.dialog_action_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User choose to delete this picture.
                        picture.delete();
                        mPictureList.remove(picture);
                        dialog.dismiss();
                        IntrusionRecordDetailsAdapter.this.notifyDataSetChanged();
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

    private String getDateFromMills(String mills) {
        Date date = new Date(Long.valueOf(mills));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return formatter.format(date);
    }
}
