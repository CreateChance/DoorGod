package com.createchance.doorgod.lockfragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.createchance.doorgod.R;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.ui.AppListActivity;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.LogUtil;
import com.eftimoff.patternview.PatternView;

import org.litepal.crud.DataSupport;

public class PatternLockFragment extends BaseFragment {

    private static final String TAG = "PatternLockFragment";

    private TextView fingerprintInfo;
    private ImageView fingerprintIcon;

    private PatternView patternView;

    public PatternLockFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pattern_lock, container, false);

        // If this device has finger print sensor and enrolls one, we will show fingerprint info.
        fingerprintInfo = (TextView) view.findViewById(R.id.fingerprint_hint);
        fingerprintIcon = (ImageView) view.findViewById(R.id.fingerprint_icon);

        patternView = (PatternView) view.findViewById(R.id.patternView);
        patternView.setOnPatternDetectedListener(new PatternView.OnPatternDetectedListener() {
            @Override
            public void onPatternDetected() {
                LogUtil.d(TAG, "pattern detected.");
                LockInfo lockInfo = DataSupport.findFirst(LockInfo.class);
                if (patternView.getPatternString().equals(lockInfo.getLockString())) {
                    if (((DoorGodActivity)getActivity()).isStartByUser()) {
                        Intent intent = new Intent(getActivity(), AppListActivity.class);
                        startActivity(intent);
                    } else {
                        ((DoorGodActivity) getActivity()).getService().addUnlockedApp();
                    }
                    cancelFingerprint();
                    getActivity().finish();
                } else {
                    patternView.setDisplayMode(PatternView.DisplayMode.Wrong);
                    patternView.clearPattern(1000);
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
