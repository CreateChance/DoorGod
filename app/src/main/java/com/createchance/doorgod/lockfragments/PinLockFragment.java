package com.createchance.doorgod.lockfragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.andrognito.pinlockview.PinLockListener;
import com.andrognito.pinlockview.PinLockView;
import com.createchance.doorgod.R;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.LogUtil;

import org.litepal.crud.DataSupport;

/**
 * Pin lock fragment.
 */

public class PinLockFragment extends Fragment {

    private static final String TAG = "PinLockFragment";

    private PinLockView pinLockView;

    private PinLockListener mPinLockListener = new PinLockListener() {
        @Override
        public void onComplete(String pin) {
            LogUtil.d(TAG, "Pin complete: " + pin);
            LockInfo pinLockInfo = DataSupport.findFirst(LockInfo.class);
            if (pin.equals(pinLockInfo.getLockString())) {
                ((DoorGodActivity)getActivity()).getService().addUnlockedApp();
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

        pinLockView = (PinLockView) view.findViewById(R.id.pin_lock_view);
        pinLockView.setPinLockListener(mPinLockListener);

        return view;
    }
}
