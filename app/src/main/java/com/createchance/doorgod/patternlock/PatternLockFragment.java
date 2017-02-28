package com.createchance.doorgod.patternlock;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.createchance.doorgod.R;
import com.createchance.doorgod.database.PatternLockInfo;
import com.createchance.doorgod.fingerprint.CryptoObjectHelper;
import com.createchance.doorgod.fingerprint.MyAuthCallback;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.LogUtil;
import com.createchance.doorgod.util.MsgUtil;
import com.eftimoff.patternview.PatternView;

import org.litepal.crud.DataSupport;

import java.util.List;

public class PatternLockFragment extends Fragment {

    private static final String TAG = "PatternLockFragment";

    private FingerprintManagerCompat fingerprintManager;
    private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;

    private TextView fingerprintInfo;

    private PatternView patternView;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            LogUtil.d(TAG, "msg: " + msg.what + " ,arg1: " + msg.arg1);
            switch (msg.what) {
                case MsgUtil.MSG_AUTH_SUCCESS:
                    getActivity().finish();
                    cancellationSignal = null;
                    break;
                case MsgUtil.MSG_AUTH_FAILED:
                    fingerprintInfo.setText(R.string.fingerprint_auth_failed);
                    cancellationSignal = null;
                    break;
                case MsgUtil.MSG_AUTH_ERROR:
                    fingerprintInfo.setText(R.string.fingerprint_auth_error);
                    break;
                default:
                    break;
            }
        }
    };

    public PatternLockFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fingerprintManager = FingerprintManagerCompat.from(getActivity());

        try {
            myAuthCallback = new MyAuthCallback(mHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pattern_lock, container, false);

        // If this device has finger print sensor and enrolls one, we will show fingerprint info.
        fingerprintInfo = (TextView) view.findViewById(R.id.fingerprint_hint);
        ImageView icon = (ImageView) view.findViewById(R.id.fingerprint_icon);
        if (fingerprintManager.isHardwareDetected()) {
            fingerprintInfo.setVisibility(View.VISIBLE);
            if (fingerprintManager.hasEnrolledFingerprints()) {
                icon.setVisibility(View.VISIBLE);

                // start fingerprint auth here.
                try {
                    CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
                    if (cancellationSignal == null) {
                        cancellationSignal = new CancellationSignal();
                    }
                    fingerprintManager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
                            cancellationSignal, myAuthCallback, null);
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.obtainMessage(MsgUtil.MSG_AUTH_ERROR).sendToTarget();
                }
            } else {
                fingerprintInfo.setText(R.string.fragment_pattern_view_fingerprint_no_enroll);
                fingerprintInfo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
                        startActivity(intent);
                    }
                });
            }
        }
        patternView = (PatternView) view.findViewById(R.id.patternView);
        patternView.setOnPatternDetectedListener(new PatternView.OnPatternDetectedListener() {
            @Override
            public void onPatternDetected() {
                LogUtil.d(TAG, "pattern detected.");
                List<PatternLockInfo> patternLockInfos = DataSupport.findAll(PatternLockInfo.class);
                for (PatternLockInfo info : patternLockInfos) {
                    if (patternView.getPatternString().equals(info.getPatternString())) {
                        ((DoorGodActivity)getActivity()).getService().addUnlockedApp();
                        getActivity().finish();
                        break;
                    }
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
    public void onDetach() {
        super.onDetach();
    }
}
