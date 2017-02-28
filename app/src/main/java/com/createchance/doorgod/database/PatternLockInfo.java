package com.createchance.doorgod.database;

import org.litepal.crud.DataSupport;

/**
 * Pattern lock info class.
 */

public class PatternLockInfo extends DataSupport {
    private String patternString;

    public String getPatternString() {
        return patternString;
    }

    public void setPatternString(String patternString) {
        this.patternString = patternString;
    }
}
