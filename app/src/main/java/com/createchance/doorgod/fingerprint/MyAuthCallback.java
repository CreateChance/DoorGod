package com.createchance.doorgod.fingerprint;

import android.os.Handler;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

import com.createchance.doorgod.util.MsgUtil;

/**
 * Fingerprint auth result callback.
 */
public class MyAuthCallback extends FingerprintManagerCompat.AuthenticationCallback {

    private Handler handler = null;

    public MyAuthCallback(Handler handler) {
        super();

        this.handler = handler;
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        super.onAuthenticationError(errMsgId, errString);

        if (handler != null) {
            handler.obtainMessage(MsgUtil.MSG_AUTH_ERROR).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        super.onAuthenticationHelp(helpMsgId, helpString);

        if (handler != null) {
            handler.obtainMessage(MsgUtil.MSG_AUTH_HELP).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);

        if (handler != null) {
            handler.obtainMessage(MsgUtil.MSG_AUTH_SUCCESS).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();

        if (handler != null) {
            handler.obtainMessage(MsgUtil.MSG_AUTH_FAILED).sendToTarget();
        }
    }
}
