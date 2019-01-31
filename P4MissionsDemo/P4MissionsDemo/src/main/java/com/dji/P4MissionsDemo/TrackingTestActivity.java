package com.dji.P4MissionsDemo;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJIError;
import dji.common.mission.activetrack.ActiveTrackMission;
import dji.common.mission.activetrack.ActiveTrackMissionEvent;
import dji.common.mission.activetrack.ActiveTrackMode;
import dji.common.mission.activetrack.ActiveTrackState;
import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.common.mission.activetrack.ActiveTrackTrackingState;
import dji.common.mission.activetrack.SubjectSensingState;
import dji.common.util.CommonCallbacks;
import dji.keysdk.CameraKey;
import dji.keysdk.callback.ActionCallback;
import dji.keysdk.callback.SetCallback;
import dji.sdk.mission.activetrack.ActiveTrackMissionOperatorListener;
import dji.sdk.mission.activetrack.ActiveTrackOperator;
import dji.sdk.sdkmanager.DJISDKManager;

import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.MotionEvent;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import dji.common.mission.activetrack.QuickShotMode;
import dji.common.util.CommonCallbacks.CompletionCallback;
import dji.common.util.CommonCallbacks.CompletionCallbackWith;
import dji.keysdk.DJIKey;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import dji.log.DJILog;
import dji.midware.media.DJIVideoDataRecver;
import dji.sdk.mission.MissionControl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrackingTestActivity extends DemoBaseActivity implements SurfaceTextureListener, OnClickListener, OnTouchListener, CompoundButton.OnCheckedChangeListener, ActiveTrackMissionOperatorListener {

    private static final String TAG = "TrackingTestActivity";
    private static final int MAIN_CAMERA_INDEX = 0;
    private static final int INVAVID_INDEX = -1;
    private static final int MOVE_OFFSET = 20;

    private RelativeLayout.LayoutParams layoutParams;
    private Switch mAutoSensingSw;
    private Switch mQuickShotSw;
    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushInfoSd;
    private ImageButton mStopBtn;
    private ImageView mTrackingImage;
    private RelativeLayout mBgLayout;
    private TextView mPushInfoTv;
    private Switch mPushBackSw;
    private Switch mGestureModeSw;
    private ImageView mSendRectIV;
    private Button mConfigBtn;
    private Button mConfirmBtn;
    private Button mRejectBtn;

    private ActiveTrackOperator mActiveTrackOperator;
    private ActiveTrackMission mActiveTrackMission;
    private final DJIKey trackModeKey = FlightControllerKey.createFlightAssistantKey(FlightControllerKey.ACTIVE_TRACK_MODE);
    private ConcurrentHashMap<Integer, MultiTrackingView> targetViewHashMap = new ConcurrentHashMap<>();
    private int trackingIndex = INVAVID_INDEX;
    private boolean isAutoSensingSupported = false;
    private ActiveTrackMode startMode = ActiveTrackMode.TRACE;
    private QuickShotMode quickShotMode = QuickShotMode.UNKNOWN;

    private boolean isDrawingRect = false;

    /**
     * Toast
     *
     * @param string
     */
    private void setResultToToast(final String string) {
        TrackingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TrackingTestActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Push Status to TextView
     *
     * @param string
     */
    private void setResultToText(final String string) {
        if (mPushInfoTv == null) {
            setResultToToast("Push info tv has not be init...");
        }
        TrackingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushInfoTv.setText(string);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_tracking_test);
        super.onCreate(savedInstanceState);
        initUI();
    }

    /**
     * InitUI
     */
    private void initUI() {
        layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        mPushDrawerIb = (ImageButton) findViewById(R.id.tracking_drawer_control_ib);
        mPushInfoSd = (SlidingDrawer) findViewById(R.id.tracking_drawer_sd);
        mPushInfoTv = (TextView) findViewById(R.id.tracking_push_tv);
        mBgLayout = (RelativeLayout) findViewById(R.id.tracking_bg_layout);
        mSendRectIV = (ImageView) findViewById(R.id.tracking_send_rect_iv);
        mTrackingImage = (ImageView) findViewById(R.id.tracking_rst_rect_iv);
        mConfirmBtn = (Button) findViewById(R.id.confirm_btn);
        mStopBtn = (ImageButton) findViewById(R.id.tracking_stop_btn);
        mRejectBtn = (Button) findViewById(R.id.reject_btn);
        mConfigBtn = (Button) findViewById(R.id.recommended_configuration_btn);
        mAutoSensingSw = (Switch) findViewById(R.id.set_multitracking_enabled);
        mQuickShotSw = (Switch) findViewById(R.id.set_multiquickshot_enabled);
        mPushBackSw = (Switch) findViewById(R.id.tracking_pull_back_tb);
        mGestureModeSw = (Switch) findViewById(R.id.tracking_in_gesture_mode);

        mAutoSensingSw.setChecked(false);
        mGestureModeSw.setChecked(false);
        mQuickShotSw.setChecked(false);
        mPushBackSw.setChecked(false);

        mAutoSensingSw.setOnCheckedChangeListener(this);
        mQuickShotSw.setOnCheckedChangeListener(this);
        mPushBackSw.setOnCheckedChangeListener(this);
        mGestureModeSw.setOnCheckedChangeListener(this);

        mBgLayout.setOnTouchListener(this);
        mConfirmBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mRejectBtn.setOnClickListener(this);
        mConfigBtn.setOnClickListener(this);
        mPushDrawerIb.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initMissionManager();
    }

    /**
     * Init Mission parameter
     */
    private void initMissionManager() {
        mActiveTrackOperator = MissionControl.getInstance().getActiveTrackOperator();
        if (mActiveTrackOperator == null) {
            return;
        }

        mActiveTrackOperator.addListener(this);
        mAutoSensingSw.setChecked(mActiveTrackOperator.isAutoSensingEnabled());
        mQuickShotSw.setChecked(mActiveTrackOperator.isAutoSensingForQuickShotEnabled());
        mGestureModeSw.setChecked(mActiveTrackOperator.isGestureModeEnabled());
        mActiveTrackOperator.getRetreatEnabled(new CompletionCallbackWith<Boolean>() {
            @Override
            public void onSuccess(final Boolean aBoolean) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPushBackSw.setChecked(aBoolean);
                    }
                });
            }

            @Override
            public void onFailure(DJIError error) {
                setResultToToast("can't get retreat enable state " + error.getDescription());
            }
        });
    }


    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    @Override
    public void onReturn(View view) {
        DJILog.d(TAG, "onReturn");
        DJISDKManager.getInstance().getMissionControl().destroy();
        this.finish();
    }

    @Override
    protected void onDestroy() {
        isAutoSensingSupported = false;
        try {
            DJIVideoDataRecver.getInstance().setVideoDataListener(false, null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mActiveTrackOperator != null) {
            mActiveTrackOperator.removeListener(this);
        }

        if (mCodecManager != null) {
            mCodecManager.destroyCodec();
        }

        super.onDestroy();
    }

    float downX;
    float downY;

    /**
     * Calculate distance
     *
     * @param point1X
     * @param point1Y
     * @param point2X
     * @param point2Y
     * @return
     */
    private double calcManhattanDistance(double point1X, double point1Y, double point2X,
                                         double point2Y) {
        return Math.abs(point1X - point2X) + Math.abs(point1Y - point2Y);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDrawingRect = false;
                downX = event.getX();
                downY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                if (calcManhattanDistance(downX, downY, event.getX(), event.getY()) < MOVE_OFFSET && !isDrawingRect) {
                    trackingIndex = getTrackingIndex(downX, downY, targetViewHashMap);
                    if (targetViewHashMap.get(trackingIndex) != null) {
                        targetViewHashMap.get(trackingIndex).setBackgroundColor(Color.RED);
                    }
                    return true;
                }
                isDrawingRect = true;
                mSendRectIV.setVisibility(View.VISIBLE);
                int l = (int) (downX < event.getX() ? downX : event.getX());
                int t = (int) (downY < event.getY() ? downY : event.getY());
                int r = (int) (downX >= event.getX() ? downX : event.getX());
                int b = (int) (downY >= event.getY() ? downY : event.getY());
                mSendRectIV.setX(l);
                mSendRectIV.setY(t);
                mSendRectIV.getLayoutParams().width = r - l;
                mSendRectIV.getLayoutParams().height = b - t;
                mSendRectIV.requestLayout();
                break;

            case MotionEvent.ACTION_UP:
                if (mGestureModeSw.isChecked()) {
                    setResultToToast("Please try to start Gesture Mode!");
                } else if (!isDrawingRect) {
                    if (targetViewHashMap.get(trackingIndex) != null) {
                        setResultToToast("Selected Index: " + trackingIndex + ",Please Confirm it!");
                        targetViewHashMap.get(trackingIndex).setBackgroundColor(Color.TRANSPARENT);
                    }
                } else {
                    RectF rectF = getActiveTrackRect(mSendRectIV);
                    mActiveTrackMission = new ActiveTrackMission(rectF, startMode);
                    if (startMode == ActiveTrackMode.QUICK_SHOT) {
                        mActiveTrackMission.setQuickShotMode(quickShotMode);
                        checkStorageStates();
                    }
                    mActiveTrackOperator.startTracking(mActiveTrackMission, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                isDrawingRect = false;
                            }
                            setResultToToast("Start Tracking: " + (error == null
                                    ? "Success"
                                    : error.getDescription()));
                        }
                    });
                    mSendRectIV.setVisibility(View.INVISIBLE);
                    clearCurrentView();
                }
                break;

            default:
                break;
        }

        return true;
    }

    /**
     * @return
     */
    private int getTrackingIndex(final float x, final float y,
                                 final ConcurrentHashMap<Integer, MultiTrackingView> multiTrackinghMap) {
        if (multiTrackinghMap == null || multiTrackinghMap.isEmpty()) {
            return INVAVID_INDEX;
        }

        float l, t, r, b;
        for (Map.Entry<Integer, MultiTrackingView> vo : multiTrackinghMap.entrySet()) {
            int key = vo.getKey().intValue();
            MultiTrackingView view = vo.getValue();
            l = view.getX();
            t = view.getY();
            r = (view.getX() + (view.getWidth() / 2));
            b = (view.getY() + (view.getHeight() / 2));

            if (x >= l && y >= t && x <= r && y <= b) {
                return key;
            }
        }
        return INVAVID_INDEX;
    }

    @Override
    public void onClick(View v) {
        if (mActiveTrackOperator == null) {
            return;
        }
        switch (v.getId()) {
            case R.id.recommended_configuration_btn:
                mActiveTrackOperator.setRecommendedConfiguration(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast("Set Recommended Config " + (error == null ? "Success" : error.getDescription()));
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mConfigBtn.setVisibility(View.GONE);
                    }
                });
                break;

            case R.id.confirm_btn:
                boolean isAutoTracking =
                        isAutoSensingSupported &&
                                (mActiveTrackOperator.isAutoSensingEnabled() ||
                                        mActiveTrackOperator.isAutoSensingForQuickShotEnabled());
                if (isAutoTracking) {
                    startAutoSensingMission();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStopBtn.setVisibility(View.VISIBLE);
                            mRejectBtn.setVisibility(View.VISIBLE);
                            mConfirmBtn.setVisibility(View.INVISIBLE);
                        }
                    });
                } else {
                    trackingIndex = INVAVID_INDEX;
                    mActiveTrackOperator.acceptConfirmation(new CompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            setResultToToast(error == null ? "Accept Confirm Success!" : error.getDescription());
                        }
                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mStopBtn.setVisibility(View.VISIBLE);
                            mRejectBtn.setVisibility(View.VISIBLE);
                            mConfirmBtn.setVisibility(View.INVISIBLE);
                        }
                    });

                }
                break;

            case R.id.tracking_stop_btn:
                trackingIndex = INVAVID_INDEX;
                mActiveTrackOperator.stopTracking(new CompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "Stop track Success!" : error.getDescription());
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mTrackingImage != null) {
                            mTrackingImage.setVisibility(View.INVISIBLE);
                            mSendRectIV.setVisibility(View.INVISIBLE);
                            mStopBtn.setVisibility(View.INVISIBLE);
                            mRejectBtn.setVisibility(View.INVISIBLE);
                            mConfirmBtn.setVisibility(View.VISIBLE);
                        }
                    }
                });
                break;

            case R.id.reject_btn:
                trackingIndex = INVAVID_INDEX;
                mActiveTrackOperator.rejectConfirmation(new CompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {

                        setResultToToast(error == null ? "Reject Confirm Success!" : error.getDescription());
                    }
                });
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mStopBtn.setVisibility(View.VISIBLE);
                        mRejectBtn.setVisibility(View.VISIBLE);
                        mConfirmBtn.setVisibility(View.INVISIBLE);
                    }
                });
                break;

            case R.id.tracking_drawer_control_ib:
                if (mPushInfoSd.isOpened()) {
                    mPushInfoSd.animateClose();
                } else {
                    mPushInfoSd.animateOpen();
                }
                break;

            default:
                break;
        }

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, final boolean isChecked) {
        if (mActiveTrackOperator == null) {
            return;
        }
        switch (compoundButton.getId()) {
            case R.id.set_multitracking_enabled:
                startMode = ActiveTrackMode.TRACE;
                quickShotMode = QuickShotMode.UNKNOWN;
                setAutoSensingEnabled(isChecked);
                break;
            case R.id.set_multiquickshot_enabled:
                startMode = ActiveTrackMode.QUICK_SHOT;
                quickShotMode = QuickShotMode.CIRCLE;
                checkStorageStates();
                setAutoSensingForQuickShotEnabled(isChecked);
                break;
            case R.id.tracking_pull_back_tb:
                mActiveTrackOperator.setRetreatEnabled(isChecked, new CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mPushBackSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set Retreat Enabled: " + (error == null
                                ? "Success"
                                : error.getDescription()));
                    }
                });
                break;
            case R.id.tracking_in_gesture_mode:
                mActiveTrackOperator.setGestureModeEnabled(isChecked, new CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mGestureModeSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set GestureMode Enabled: " + (error == null
                                ? "Success"
                                : error.getDescription()));
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void onUpdate(ActiveTrackMissionEvent event) {
        StringBuffer sb = new StringBuffer();
        String errorInformation = (event.getError() == null ? "null" : event.getError().getDescription()) + "\n";
        String currentState = event.getCurrentState() == null ? "null" : event.getCurrentState().getName();
        String previousState = event.getPreviousState() == null ? "null" : event.getPreviousState().getName();

        ActiveTrackTargetState targetState = ActiveTrackTargetState.UNKNOWN;
        if (event.getTrackingState() != null) {
            targetState = event.getTrackingState().getState();
        }
        Utils.addLineToSB(sb, "CurrentState: ", currentState);
        Utils.addLineToSB(sb, "PreviousState: ", previousState);
        Utils.addLineToSB(sb, "TargetState: ", targetState);
        Utils.addLineToSB(sb, "Error:", errorInformation);

        Object value = KeyManager.getInstance().getValue(trackModeKey);
        if (value instanceof ActiveTrackMode) {
            Utils.addLineToSB(sb, "TrackingMode:", value.toString());
        }

        ActiveTrackTrackingState trackingState = event.getTrackingState();
        if (trackingState != null) {
            final SubjectSensingState[] targetSensingInformations = trackingState.getAutoSensedSubjects();
            if (targetSensingInformations != null) {
                for (SubjectSensingState subjectSensingState : targetSensingInformations) {
                    RectF trackingRect = subjectSensingState.getTargetRect();
                    if (trackingRect != null) {
                        Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX());
                        Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY());
                        Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width());
                        Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height());
                        Utils.addLineToSB(sb, "Reason", trackingState.getReason().name());
                        Utils.addLineToSB(sb, "Target Index: ", subjectSensingState.getIndex());
                        Utils.addLineToSB(sb, "Target Type", subjectSensingState.getTargetType().name());
                        Utils.addLineToSB(sb, "Target State", subjectSensingState.getState().name());
                        isAutoSensingSupported = true;
                    }
                }
            } else {
                RectF trackingRect = trackingState.getTargetRect();
                if (trackingRect != null) {
                    Utils.addLineToSB(sb, "Rect center x: ", trackingRect.centerX());
                    Utils.addLineToSB(sb, "Rect center y: ", trackingRect.centerY());
                    Utils.addLineToSB(sb, "Rect Width: ", trackingRect.width());
                    Utils.addLineToSB(sb, "Rect Height: ", trackingRect.height());
                    Utils.addLineToSB(sb, "Reason", trackingState.getReason().name());
                    Utils.addLineToSB(sb, "Target Index: ", trackingState.getTargetIndex());
                    Utils.addLineToSB(sb, "Target Type", trackingState.getType().name());
                    Utils.addLineToSB(sb, "Target State", trackingState.getState().name());
                    isAutoSensingSupported = false;
                }
                clearCurrentView();
            }
        }

        setResultToText(sb.toString());
        updateActiveTrackRect(mTrackingImage, event);
        updateButtonVisibility(event);
    }

    /**
     * Update ActiveTrack Rect
     *
     * @param iv
     * @param event
     */
    private void updateActiveTrackRect(final ImageView iv, final ActiveTrackMissionEvent event) {
        if (iv == null || event == null) {
            return;
        }

        ActiveTrackTrackingState trackingState = event.getTrackingState();
        if (trackingState != null) {
            if (trackingState.getAutoSensedSubjects() != null) {
                final SubjectSensingState[] targetSensingInformations = trackingState.getAutoSensedSubjects();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateMultiTrackingView(targetSensingInformations);
                    }
                });
            } else {
                RectF trackingRect = trackingState.getTargetRect();
                ActiveTrackTargetState trackTargetState = trackingState.getState();
                postResultRect(iv, trackingRect, trackTargetState);
            }
        }

    }

    private void updateButtonVisibility(final ActiveTrackMissionEvent event) {
        ActiveTrackState state = event.getCurrentState();
        if (state == ActiveTrackState.AUTO_SENSING ||
                state == ActiveTrackState.AUTO_SENSING_FOR_QUICK_SHOT ||
                state == ActiveTrackState.WAITING_FOR_CONFIRMATION) {
            TrackingTestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mStopBtn.setVisibility(View.VISIBLE);
                    mStopBtn.setClickable(true);
                    mConfirmBtn.setVisibility(View.VISIBLE);
                    mConfirmBtn.setClickable(true);
                    mRejectBtn.setVisibility(View.VISIBLE);
                    mRejectBtn.setClickable(true);
                    mConfigBtn.setVisibility(View.GONE);
                }
            });
        } else if (state == ActiveTrackState.AIRCRAFT_FOLLOWING ||
                state == ActiveTrackState.ONLY_CAMERA_FOLLOWING ||
                state == ActiveTrackState.FINDING_TRACKED_TARGET ||
                state == ActiveTrackState.CANNOT_CONFIRM ||
                state == ActiveTrackState.PERFORMING_QUICK_SHOT) {
            TrackingTestActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mStopBtn.setVisibility(View.VISIBLE);
                    mStopBtn.setClickable(true);
                    mConfirmBtn.setVisibility(View.INVISIBLE);
                    mConfirmBtn.setClickable(false);
                    mRejectBtn.setVisibility(View.VISIBLE);
                    mRejectBtn.setClickable(true);
                    mConfigBtn.setVisibility(View.GONE);
                }
            });
        } else {
            TrackingTestActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mStopBtn.setVisibility(View.INVISIBLE);
                    mStopBtn.setClickable(false);
                    mConfirmBtn.setVisibility(View.INVISIBLE);
                    mConfirmBtn.setClickable(false);
                    mRejectBtn.setVisibility(View.INVISIBLE);
                    mRejectBtn.setClickable(false);
                    mTrackingImage.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    /**
     * Get ActiveTrack RectF
     *
     * @param iv
     * @return
     */
    private RectF getActiveTrackRect(View iv) {
        View parent = (View) iv.getParent();
        return new RectF(
                ((float) iv.getLeft() + iv.getX()) / (float) parent.getWidth(),
                ((float) iv.getTop() + iv.getY()) / (float) parent.getHeight(),
                ((float) iv.getRight() + iv.getX()) / (float) parent.getWidth(),
                ((float) iv.getBottom() + iv.getY()) / (float) parent.getHeight());
    }

    /**
     * Post Result RectF
     *
     * @param iv
     * @param rectF
     * @param targetState
     */
    private void postResultRect(final ImageView iv, final RectF rectF,
                                final ActiveTrackTargetState targetState) {
        View parent = (View) iv.getParent();
        RectF trackingRect = rectF;

        final int l = (int) ((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int) ((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int) ((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int) ((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

        TrackingTestActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mTrackingImage.setVisibility(View.VISIBLE);
                if ((targetState == ActiveTrackTargetState.CANNOT_CONFIRM)
                        || (targetState == ActiveTrackTargetState.UNKNOWN)) {
                    iv.setImageResource(R.drawable.visual_track_cannotconfirm);
                } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
                    iv.setImageResource(R.drawable.visual_track_needconfirm);
                } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
                    iv.setImageResource(R.drawable.visual_track_lowconfidence);
                } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
                    iv.setImageResource(R.drawable.visual_track_highconfidence);
                }
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
            }
        });
    }

    /**
     * PostMultiResult
     *
     * @param iv
     * @param rectF
     * @param information
     */
    private void postMultiResultRect(final MultiTrackingView iv, final RectF rectF,
                                     final SubjectSensingState information) {
        View parent = (View) iv.getParent();
        RectF trackingRect = rectF;

        final int l = (int) ((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int) ((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int) ((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int) ((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

        TrackingTestActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mTrackingImage.setVisibility(View.INVISIBLE);
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
                iv.updateView(information);
            }
        });
    }

    /**
     * Update MultiTrackingView
     *
     * @param targetSensingInformations
     */
    private void updateMultiTrackingView(final SubjectSensingState[] targetSensingInformations) {
        ArrayList<Integer> indexs = new ArrayList<>();
        for (SubjectSensingState target : targetSensingInformations) {
            indexs.add(target.getIndex());
            if (targetViewHashMap.containsKey(target.getIndex())) {

                MultiTrackingView targetView = targetViewHashMap.get(target.getIndex());
                postMultiResultRect(targetView, target.getTargetRect(), target);
            } else {
                MultiTrackingView trackingView = new MultiTrackingView(TrackingTestActivity.this);
                mBgLayout.addView(trackingView, layoutParams);
                targetViewHashMap.put(target.getIndex(), trackingView);
            }
        }

        ArrayList<Integer> missingIndexs = new ArrayList<>();
        for (Integer key : targetViewHashMap.keySet()) {
            boolean isDisappeared = true;
            for (Integer index : indexs) {
                if (index.equals(key)) {
                    isDisappeared = false;
                    break;
                }
            }

            if (isDisappeared) {
                missingIndexs.add(key);
            }
        }

        for (Integer i : missingIndexs) {
            MultiTrackingView view = targetViewHashMap.remove(i);
            mBgLayout.removeView(view);
        }
    }


    /**
     * Enable MultiTracking
     *
     * @param isChecked
     */
    private void setAutoSensingEnabled(final boolean isChecked) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                startMode = ActiveTrackMode.TRACE;
                mActiveTrackOperator.enableAutoSensing(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mAutoSensingSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set AutoSensing Enabled " + (error == null ? "Success!" : error.getDescription()));
                    }
                });
            } else {
                disableAutoSensing();
            }
        }
    }

    /**
     * Enable QuickShotMode
     *
     * @param isChecked
     */
    private void setAutoSensingForQuickShotEnabled(final boolean isChecked) {
        if (mActiveTrackOperator != null) {
            if (isChecked) {
                mActiveTrackOperator.enableAutoSensingForQuickShot(new CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (error != null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mQuickShotSw.setChecked(!isChecked);
                                }
                            });
                        }
                        setResultToToast("Set QuickShot Enabled " + (error == null ? "Success!" : error.getDescription()));
                    }
                });

            } else {
                disableAutoSensing();
            }

        }
    }

    /**
     * Disable AutoSensing
     */
    private void disableAutoSensing() {
        if (mActiveTrackOperator != null) {
            mActiveTrackOperator.disableAutoSensing(new CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mConfirmBtn.setVisibility(View.INVISIBLE);
                                mStopBtn.setVisibility(View.INVISIBLE);
                                mRejectBtn.setVisibility(View.INVISIBLE);
                                mConfigBtn.setVisibility(View.VISIBLE);
                                isAutoSensingSupported = false;
                            }
                        });
                        clearCurrentView();
                    }
                    setResultToToast(error == null ? "Disable Auto Sensing Success!" : error.getDescription());
                }
            });
        }
    }


    /**
     * Confim Mission by Index
     */
    private void startAutoSensingMission() {
        if (trackingIndex != INVAVID_INDEX) {
            ActiveTrackMission mission = new ActiveTrackMission(null, startMode);
            mission.setQuickShotMode(quickShotMode);
            mission.setTargetIndex(trackingIndex);
            mActiveTrackOperator.startAutoSensingMission(mission, new CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        setResultToToast("Accept Confim index: " + trackingIndex + " Success!");
                        trackingIndex = INVAVID_INDEX;
                    } else {
                        setResultToToast(error.getDescription());
                    }
                }
            });
        }
    }


    /**
     * Change Storage Location
     */
    private void switchStorageLocation(final SettingsDefinitions.StorageLocation storageLocation) {
        KeyManager keyManager = KeyManager.getInstance();
        DJIKey storageLoactionkey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, MAIN_CAMERA_INDEX);

        if (storageLocation == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
            keyManager.setValue(storageLoactionkey, SettingsDefinitions.StorageLocation.SDCARD, new SetCallback() {
                @Override
                public void onSuccess() {
                    setResultToToast("Change to SD card Success!");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    setResultToToast(error.getDescription());
                }
            });
        } else {
            keyManager.setValue(storageLoactionkey, SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new SetCallback() {
                @Override
                public void onSuccess() {
                    setResultToToast("Change to Interal Storage Success!");
                }

                @Override
                public void onFailure(@NonNull DJIError error) {
                    setResultToToast(error.getDescription());
                }
            });
        }
    }

    /**
     * determine SD Card is or not Ready
     *
     * @param index
     * @return
     */
    private boolean isSDCardReady(int index) {
        KeyManager keyManager = KeyManager.getInstance();

        return ((Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INSERTED, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INITIALIZING, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_READ_ONLY, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_HAS_ERROR, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_FULL, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_BUSY, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_FORMATTING, index))
                && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_INVALID_FORMAT, index))
                && (Boolean) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_IS_VERIFIED, index))
                && (Long) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_AVAILABLE_CAPTURE_COUNT, index)) > 0L
                && (Integer) keyManager.getValue(CameraKey.create(CameraKey.SDCARD_AVAILABLE_RECORDING_TIME_IN_SECONDS, index)) > 0);
    }

    /**
     * determine Interal Storage is or not Ready
     *
     * @param index
     * @return
     */
    private boolean isInteralStorageReady(int index) {
        KeyManager keyManager = KeyManager.getInstance();

        boolean isInternalSupported = (boolean)
                keyManager.getValue(CameraKey.create(CameraKey.IS_INTERNAL_STORAGE_SUPPORTED, index));
        if (isInternalSupported) {
            return ((Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INSERTED, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INITIALIZING, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_READ_ONLY, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_HAS_ERROR, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_FULL, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_BUSY, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_FORMATTING, index))
                    && !(Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_INVALID_FORMAT, index))
                    && (Boolean) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_IS_VERIFIED, index))
                    && (Long) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_CAPTURE_COUNT, index)) > 0L
                    && (Integer) keyManager.getValue(CameraKey.create(CameraKey.INNERSTORAGE_AVAILABLE_RECORDING_TIME_IN_SECONDS, index)) > 0);
        }
        return false;
    }

    /**
     * Check Storage States
     */
    private void checkStorageStates() {
        KeyManager keyManager = KeyManager.getInstance();
        DJIKey storageLocationkey = CameraKey.create(CameraKey.CAMERA_STORAGE_LOCATION, MAIN_CAMERA_INDEX);
        Object storageLocationObj = keyManager.getValue(storageLocationkey);
        SettingsDefinitions.StorageLocation storageLocation = SettingsDefinitions.StorageLocation.INTERNAL_STORAGE;

        if (storageLocationObj instanceof SettingsDefinitions.StorageLocation){
            storageLocation = (SettingsDefinitions.StorageLocation) storageLocationObj;
        }

        if (storageLocation == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
            if (!isInteralStorageReady(MAIN_CAMERA_INDEX) && isSDCardReady(MAIN_CAMERA_INDEX)) {
                switchStorageLocation(SettingsDefinitions.StorageLocation.SDCARD);
            }
        }

        if (storageLocation == SettingsDefinitions.StorageLocation.SDCARD) {
            if (!isSDCardReady(MAIN_CAMERA_INDEX) && isInteralStorageReady(MAIN_CAMERA_INDEX)) {
                switchStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE);
            }
        }

        DJIKey isRecordingKey = CameraKey.create(CameraKey.IS_RECORDING, MAIN_CAMERA_INDEX);
        Object isRecording = keyManager.getValue(isRecordingKey);
        if (isRecording instanceof Boolean) {
            if (((Boolean) isRecording).booleanValue()) {
                keyManager.performAction(CameraKey.create(CameraKey.STOP_RECORD_VIDEO, MAIN_CAMERA_INDEX), new ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setResultToToast("Stop Recording Success!");
                    }

                    @Override
                    public void onFailure(@NonNull DJIError error) {
                        setResultToToast("Stop Recording Fail，Error " + error.getDescription());
                    }
                });
            }
        }
    }

    /**
     * Clear MultiTracking View
     */
    private void clearCurrentView() {
        if (targetViewHashMap != null && !targetViewHashMap.isEmpty()) {
            Iterator<Map.Entry<Integer, MultiTrackingView>> it = targetViewHashMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, MultiTrackingView> entry = it.next();
                final MultiTrackingView view = entry.getValue();
                it.remove();
                TrackingTestActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBgLayout.removeView(view);
                    }
                });
            }
        }
    }
}
