package com.createchance.doorgod.lockfragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.createchance.doorgod.R;
import com.createchance.doorgod.database.LockInfo;
import com.createchance.doorgod.service.DoorGodService;
import com.createchance.doorgod.ui.AppListActivity;
import com.createchance.doorgod.ui.AuthFailed;
import com.createchance.doorgod.ui.DoorGodActivity;
import com.createchance.doorgod.util.FingerprintAuthResponse;
import com.createchance.doorgod.util.LogUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.litepal.crud.DataSupport;

public class PatternLockFragment extends Fragment {

    private static final String TAG = "PatternLockFragment";

    private TextView fingerprintInfo;
    private ImageView fingerprintIcon;
    private Button moreButton;

    private Lock9View patternView;

    private DoorGodService.ServiceBinder mService;

    private AuthFailed mCallback;

    private SharedPreferences mPrefs;
    private static final String LOCK_ENROLL_STATUS = "com.createchance.doorgod.LOCK_ENROLL_STATUS";
    private static final String LOCK_ANIM = "LOCK_ANIM";

    public PatternLockFragment() {
        // Required empty public constructor
    }

    public void setCallback(AuthFailed callback) {
        this.mCallback = callback;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        // get prefs
        mPrefs = getActivity().getSharedPreferences(LOCK_ENROLL_STATUS, Context.MODE_PRIVATE);

        EventBus.getDefault().register(this);

        // get service.
        mService = ((DoorGodActivity)getActivity()).getService();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pattern_lock, container, false);

        patternView = (Lock9View) view.findViewById(R.id.patternView);
        patternView.setNodeOnAnim(getSelectedPos() == 0 ? R.anim.node_on_scale : R.anim.node_on_trans);
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
                    // tell this error
                    mCallback.onFailed();
                    //patternView.setDisplayMode(PatternView.DisplayMode.Wrong);
                    //patternView.clearPattern(500);
                }
            }
        });

        // If this device has finger print sensor and enrolls one, we will show fingerprint info.
        fingerprintInfo = (TextView) view.findViewById(R.id.fingerprint_hint);
        fingerprintIcon = (ImageView) view.findViewById(R.id.fingerprint_icon);
        moreButton = (Button) view.findViewById(R.id.pattern_lock_more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setIcon(R.drawable.ic_settings_white_48dp)
                        .setTitle(R.string.pattern_more_settings_anim_title)
                        .setSingleChoiceItems(R.array.pattern_lock_anim,
                                getSelectedPos(),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == 0) {
                                            // scale anim
                                            patternView.setNodeOnAnim(R.anim.node_on_scale);
                                        } else if (which == 1) {
                                            // trans anim
                                            patternView.setNodeOnAnim(R.anim.node_on_trans);
                                        }
                                        SharedPreferences.Editor editor = mPrefs.edit();
                                        editor.putInt(LOCK_ANIM, which);
                                        editor.commit();
                                        dialog.dismiss();
                                    }
                                });
                builder.create().show();
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
                // tell this error.
                mCallback.onFailed();
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

    private int getSelectedPos() {
        return mPrefs.getInt(LOCK_ANIM, -1);
    }
}
