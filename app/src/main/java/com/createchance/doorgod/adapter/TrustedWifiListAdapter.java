package com.createchance.doorgod.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.createchance.doorgod.R;

import java.util.List;

/**
 * Created by gaochao on 25/07/2017.
 */
public class TrustedWifiListAdapter extends BaseAdapter {
    private List<String> mSavedWifi;
    private List<String> mTrustedWifi;
    private String mConnectedWifi;
    private Context context;
    private LayoutInflater inflater = null;
    private OnClick mCallback;

    public TrustedWifiListAdapter(List<String> savedWifi, List<String> trustedWifi,
                                  String connectedWifi, OnClick callback, Context context) {
        this.context = context;
        this.mSavedWifi = savedWifi;
        this.mTrustedWifi = trustedWifi;
        this.mConnectedWifi = connectedWifi;
        this.mCallback = callback;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mSavedWifi.size();
    }

    @Override
    public Object getItem(int position) {
        return mSavedWifi.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.wifi_list_item, null);
            holder.ssid = (TextView) convertView.findViewById(R.id.wifi_item_ssid);
            holder.isConnected = (TextView) convertView.findViewById(R.id.wifi_item_is_connected);
            holder.cb = (CheckBox) convertView.findViewById(R.id.wifi_item_cb);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        String ssid = mSavedWifi.get(position);
        holder.ssid.setText(ssid);
        holder.isConnected.setText(
                ssid.equals(mConnectedWifi) ? context.getString(R.string.settings_trust_wifi_connected) : null);
        holder.cb.setChecked(mTrustedWifi.contains(mSavedWifi.get(position)));
        holder.cb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallback != null) {
                    mCallback.onClick(mSavedWifi.get(position), ((CheckBox)view).isChecked());
                }
            }
        });
        return convertView;
    }

    public class ViewHolder {
        TextView ssid;
        TextView isConnected;
        CheckBox cb;

        public TextView getSsid() {
            return ssid;
        }

        public void setSsid(TextView ssid) {
            this.ssid = ssid;
        }

        public TextView getIsConnected() {
            return isConnected;
        }

        public void setIsConnected(TextView isConnected) {
            this.isConnected = isConnected;
        }

        public CheckBox getCb() {
            return cb;
        }

        public void setCb(CheckBox cb) {
            this.cb = cb;
        }
    }

    public interface OnClick {
        void onClick(String ssid, boolean trusted);
    }
}