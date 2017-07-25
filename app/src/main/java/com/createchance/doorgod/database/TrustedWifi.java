package com.createchance.doorgod.database;

import org.litepal.crud.DataSupport;

/**
 * Created by gaochao on 25/07/2017.
 */

public class TrustedWifi extends DataSupport {
    private String ssid;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    @Override
    public String toString() {
        return "TrustedWifi{" +
                "ssid='" + ssid + '\'' +
                '}';
    }
}
