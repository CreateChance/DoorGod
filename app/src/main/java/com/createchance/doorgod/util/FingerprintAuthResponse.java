package com.createchance.doorgod.util;

/**
 * Fingerprint Auth Result Response.
 */

public class FingerprintAuthResponse {
    public static final int MSG_AUTH_SUCCESS = 100;
    public static final int MSG_AUTH_FAILED = 101;
    public static final int MSG_AUTH_ERROR = 102;
    public static final int MSG_AUTH_HELP = 103;

    private final int result;

    public FingerprintAuthResponse(int result) {
        this.result = result;
    }

    public int getResult() {
        return result;
    }
}
