package com.createchance.doorgod.lockfragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.createchance.doorgod.fingerprint.CryptoObjectHelper;
import com.createchance.doorgod.fingerprint.MyAuthCallback;
import com.createchance.doorgod.util.LogUtil;
import com.createchance.doorgod.util.MsgUtil;

public abstract class BaseFragment extends Fragment {
    private static final String TAG = "BaseFragment";

    private FingerprintManagerCompat fingerprintManager;
    private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;
    private boolean isFingerPrintPass = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            LogUtil.d(TAG, "msg: " + msg.what + " ,arg1: " + msg.arg1);
            switch (msg.what) {
                case MsgUtil.MSG_AUTH_SUCCESS:
                    LogUtil.d(TAG, "is canceled: " + cancellationSignal.isCanceled());
                    isFingerPrintPass = true;
                    onFingerprintSuccess();
                    cancellationSignal = null;
                    break;
                case MsgUtil.MSG_AUTH_FAILED:
                    onFingerprintFailed();
                    cancellationSignal = null;
                    break;
                case MsgUtil.MSG_AUTH_ERROR:
                    onFingerprintError();
                    if (cancellationSignal.isCanceled()) {
                        getActivity().finish();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fingerprintManager = FingerprintManagerCompat.from(getActivity());

        try {
            myAuthCallback = new MyAuthCallback(mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (fingerprintManager.isHardwareDetected()) {
            if (fingerprintManager.hasEnrolledFingerprints()) {

                // start fingerprint auth here.
                try {
                    CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
                    if (cancellationSignal == null) {
                        cancellationSignal = new CancellationSignal();
                    }
                    LogUtil.d(TAG, "Now we start listen for finger print auth.");
                    fingerprintManager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
                            cancellationSignal, myAuthCallback, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.obtainMessage(MsgUtil.MSG_AUTH_ERROR).sendToTarget();
                }
            } else {
                noFingerprintEnrolled();
            }
        } else {
            noFingerprintHardware();
        }
    }

    public void cancelFingerprint() {
        if (!isFingerPrintPass && cancellationSignal != null) {
            LogUtil.d(TAG, "cancel finger print.#############################");
            // cancel fingerprint auth here.
            cancellationSignal.cancel();
        }
    }

    public abstract void noFingerprintHardware();

    public abstract void noFingerprintEnrolled();

    public abstract void onFingerprintSuccess();

    public abstract void onFingerprintFailed();

    public abstract void onFingerprintError();
}
