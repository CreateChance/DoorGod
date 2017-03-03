package com.createchance.doorgod.lockfragments;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.createchance.doorgod.R;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.ui.AppListActivity;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.LogUtil;

import org.litepal.crud.DataSupport;

/**
 * Pin lock fragment.
 */

public class PinLockFragment extends BaseFragment {

    private static final String TAG = "PinLockFragment";

    private TextView fingerprintInfo;
    private ImageView fingerprintIcon;

    private PinLockView pinLockView;

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            LogUtil.d(TAG, "Pin complete: " + pin);
            LockInfo pinLockInfo = DataSupport.findFirst(LockInfo.class);
            if (pin.equals(pinLockInfo.getLockString())) {
                if (((DoorGodActivity)getActivity()).isStartByUser()) {
                    Intent intent = new Intent(getActivity(), AppListActivity.class);
                    startActivity(intent);
                } else {
                    ((DoorGodActivity) getActivity()).getService().addUnlockedApp();
                }
                cancelFingerprint();
                getActivity().finish();
            } else {
                LogUtil.d(TAG, "auth failed: " + pinLockInfo.getLockString());
            }
        }

        @Override
        public void onEmpty() {
            LogUtil.d(TAG, "Pin empty");
        }

        @Override
        public void onPinChange(int pinLength, String intermediatePin) {
            LogUtil.d(TAG, "Pin changed, new length " + pinLength + " with intermediate pin " + intermediatePin);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pin_lock, container, false);

        // If this device has finger print sensor and enrolls one, we will show fingerprint info.
        fingerprintInfo = (TextView) view.findViewById(R.id.fingerprint_hint);
        fingerprintIcon = (ImageView) view.findViewById(R.id.fingerprint_icon);

        pinLockView = (PinLockView) view.findViewById(R.id.pin_lock_view);
        pinLockView.setPinLockListener(mPinLockListener);

        return view;
    }

    @Override
    public void noFingerprintHardware() {
        fingerprintInfo.setVisibility(View.GONE);
        fingerprintIcon.setVisibility(View.GONE);
    }

    @Override
    public void noFingerprintEnrolled() {
        fingerprintIcon.setVisibility(View.GONE);
        fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
        fingerprintInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onFingerprintSuccess() {
        if (((DoorGodActivity)getActivity()).isStartByUser()) {
            Intent intent = new Intent(getActivity(), AppListActivity.class);
            startActivity(intent);
        } else {
            ((DoorGodActivity) getActivity()).getService().addUnlockedApp();
        }
        getActivity().finish();
    }

    @Override
    public void onFingerprintFailed() {
        fingerprintInfo.setText(R.string.fingerprint_auth_failed);
    }

    @Override
    public void onFingerprintError() {
        fingerprintInfo.setText(R.string.fingerprint_auth_error);
    }
}
