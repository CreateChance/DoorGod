package com.createchance.doorgod.database;

import org.litepal.crud.DataSupport;

/**
 * Class for protected app.
 */

public class ProtectedApplication extends DataSupport {

    private String packageName;

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }
}
