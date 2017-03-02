package com.createchance.doorgod.database;

import org.litepal.crud.DataSupport;

/**
 * Pattern lock info class.
 */

public class LockInfo extends DataSupport {

    private String lockString;

    private int lockType;

    public String getLockString() {
        return lockString;
    }

    public void setLockString(String lockString) {
        this.lockString = lockString;
    }

    public int getLockType() {
        return lockType;
    }

    public void setLockType(int lockType) {
        this.lockType = lockType;
    }
}
