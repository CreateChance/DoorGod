package com.createchance.doorgod.lockfragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.ui.AppListActivity;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.FingerprintAuthResponse;
import com.createchance.doorgod.util.LogUtil;
import com.takwolf.android.lock9.Lock9View;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

public class PatternLockFragment extends Fragment {

    private static final String TAG = "PatternLockFragment";

    private TextView fingerprintInfo;
    private ImageView fingerprintIcon;

    private Lock9View patternView;

    private DoorGodService.ServiceBinder mService;

    public PatternLockFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        EventBus.getDefault().register(this);

        // get service.
        mService = ((DoorGodActivity)getActivity()).getService();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pattern_lock, container, false);

        // If this device has finger print sensor and enrolls one, we will show fingerprint info.
        fingerprintInfo = (TextView) view.findViewById(R.id.fingerprint_hint);
        fingerprintIcon = (ImageView) view.findViewById(R.id.fingerprint_icon);

        patternView = (Lock9View) view.findViewById(R.id.patternView);
        patternView.setCallBack(new Lock9View.CallBack() {
            @Override
            public void onFinish(String password) {
                LogUtil.d(TAG, "pattern detected.");
                LockInfo lockInfo = DataSupport.findFirst(LockInfo.class);
                if (password.equals(lockInfo.getLockString())) {
                    if (((DoorGodActivity)getActivity()).isLaunchFromHome()) {
                        Intent intent = new Intent(getActivity(), AppListActivity.class);
                        startActivity(intent);
                    } else {
                        ((DoorGodActivity) getActivity()).getService().addUnlockedApp();
                    }
                    mService.cancelFingerprint();
                    getActivity().finish();
                } else {
                    Toast.makeText(getActivity(),
                            R.string.fragment_pattern_view_pattern_error, Toast.LENGTH_SHORT).show();
                    //patternView.setDisplayMode(PatternView.DisplayMode.Wrong);
                    //patternView.clearPattern(500);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        LogUtil.d(TAG, "onResume");
        if (!mService.hasFingerprintHardware()) {
            LogUtil.v(TAG, "No fingerprint hardware.");
            fingerprintInfo.setVisibility(View.GONE);
            fingerprintIcon.setVisibility(View.GONE);
        } else if (!mService.isFingerprintEnrolled()) {
            LogUtil.v(TAG, "No fingerprint enrolled.");
            fingerprintIcon.setVisibility(View.GONE);
            fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
            fingerprintInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                    startActivity(intent);
                }
            });
        } else {
            LogUtil.v(TAG, "Find hardware and enrolled.");
            fingerprintIcon.setVisibility(View.VISIBLE);
            fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint);
            fingerprintInfo.setOnClickListener(null);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFingerprintAuthResult(FingerprintAuthResponse response) {
        LogUtil.d(TAG, "Fingerprint auth result: " + response.getResult());
        switch (response.getResult()) {
            case FingerprintAuthResponse.MSG_AUTH_SUCCESS:
                if (((DoorGodActivity)getActivity()).isLaunchFromHome()) {
                    Intent intent = new Intent(getActivity(), AppListActivity.class);
                    startActivity(intent);
                } else {
                    ((DoorGodActivity) getActivity()).getService().addUnlockedApp();
                }
                getActivity().finish();
                break;
            case FingerprintAuthResponse.MSG_AUTH_FAILED:
                fingerprintInfo.setText(R.string.fingerprint_auth_failed);
                break;
            case FingerprintAuthResponse.MSG_AUTH_ERROR:
                fingerprintInfo.setText(R.string.fingerprint_auth_error);
                break;
            case FingerprintAuthResponse.MSG_AUTH_HELP:
                // show failed info for now.
                fingerprintInfo.setText(R.string.fingerprint_auth_failed);
                break;
            default:
                break;
        }
    }
}
