package com.dji.P4MissionsDemo;

import dji.sdk.MissionManager.DJIMission.DJIMissionProgressStatus;
import dji.sdk.MissionManager.DJIMissionManager;
import dji.sdk.MissionManager.DJIMissionManager.MissionProgressStatusCallback;
import dji.sdk.MissionManager.DJIActiveTrackMission;
import dji.sdk.MissionManager.DJIActiveTrackMission.*;
import dji.sdk.base.DJIBaseComponent.DJICompletionCallback;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIError;

import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class TrackingTestActivity extends DemoBaseActivity implements SurfaceTextureListener, OnClickListener, OnTouchListener, MissionProgressStatusCallback, DJICompletionCallback {

    private static final String TAG = "TrackingTestActivity";

    private DJIMissionManager mMissionManager;

    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushInfoSd;
    private ImageButton mStopBtn;
    private Button mConfirmBtn;
    private RelativeLayout mBgLayout;
    private TextView mPushInfoTv;
    private TextView mPushBackTv;
    private Switch mPushBackSw;
    private ImageView mSendRectIV;

    // flags
    private boolean isDrawingRect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_tracking_test);
        super.onCreate(savedInstanceState);
        initUI();

    }

    @Override
    protected void onResume() {
        super.onResume();
        initMissionManager();
    }

    @Override
    protected void onDestroy() {

        if(mCodecManager != null){
            mCodecManager.destroyCodec();
        }

        super.onDestroy();
    }

    public void onReturn(View view){
        this.finish();
    }

    private void setResultToToast(final String string) {
        TrackingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TrackingTestActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

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

    private void initUI() {
        mPushDrawerIb = (ImageButton)findViewById(R.id.tracking_drawer_control_ib);
        mPushInfoSd = (SlidingDrawer)findViewById(R.id.tracking_drawer_sd);
        mStopBtn = (ImageButton)findViewById(R.id.tracking_stop_btn);
        mConfirmBtn = (Button)findViewById(R.id.tracking_confirm_btn);
        mBgLayout = (RelativeLayout)findViewById(R.id.tracking_bg_layout);
        mPushInfoTv = (TextView)findViewById(R.id.tracking_push_tv);
        mSendRectIV = (ImageView)findViewById(R.id.tracking_send_rect_iv);
        mPushBackSw = (Switch)findViewById(R.id.tracking_pull_back_sw);
        mPushBackTv = (TextView)findViewById(R.id.tracking_backward_tv);

        mStopBtn.setOnClickListener(this);
        mConfirmBtn.setOnClickListener(this);
        mBgLayout.setOnTouchListener(this);
        mPushDrawerIb.setOnClickListener(this);
    }

    private void initMissionManager() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();

        if (product == null || !product.isConnected()) {
            setResultToToast("Disconnected");
            mMissionManager = null;
        } else {
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(this);
            mMissionManager.setMissionExecutionFinishedCallback(this);
        }
    }

    /**
     * @Description : MissionExecutionFinishedCallback Method
     */
    @Override
    public void onResult(DJIError error) {
        setResultToText("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        TrackingTestActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                mConfirmBtn.setVisibility(View.INVISIBLE);
                mStopBtn.setVisibility(View.INVISIBLE);
                mStopBtn.setClickable(false);
                mPushBackTv.setVisibility(View.VISIBLE);
                mPushBackSw.setVisibility(View.VISIBLE);
            }
        });

    }

    /**
     * @Description MissionProgressStatusCallback Method
     */
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus progressStatus) {
        if (progressStatus instanceof DJIActiveTrackMissionProgressStatus) {
            DJIActiveTrackMissionProgressStatus trackingStatus = (DJIActiveTrackMissionProgressStatus)progressStatus;
            StringBuffer sb = new StringBuffer();
            Utils.addLineToSB(sb, "center x", trackingStatus.getTrackingRect().centerX());
            Utils.addLineToSB(sb, "center y", trackingStatus.getTrackingRect().centerY());
            Utils.addLineToSB(sb, "width", trackingStatus.getTrackingRect().width());
            Utils.addLineToSB(sb, "height", trackingStatus.getTrackingRect().height());
            Utils.addLineToSB(sb, "Executing State", trackingStatus.getExecutionState().name());
            Utils.addLineToSB(sb, "is human", trackingStatus.isHuman());
            Utils.addLineToSB(sb, "Error", trackingStatus.getError() == null ? "No Errors" : trackingStatus.getError().getDescription());
            setResultToText(sb.toString());
            updateActiveTrackRect(mConfirmBtn, trackingStatus);
        }
    }

    private RectF getActiveTrackRect(View iv) {
        View parent = (View)iv.getParent();
        return new RectF(
                ((float)iv.getLeft() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getTop() + iv.getY()) / (float)parent.getHeight(),
                ((float)iv.getRight() + iv.getX()) / (float)parent.getWidth(),
                ((float)iv.getBottom() + iv.getY()) / (float)parent.getHeight()
        );
    }

    private void updateActiveTrackRect(final TextView iv, final DJIActiveTrackMissionProgressStatus progressStatus) {
        if (iv == null || progressStatus == null) return;
        View parent = (View)iv.getParent();
        RectF trackingRect = progressStatus.getTrackingRect();

        final int l = (int)((trackingRect.centerX() - trackingRect.width() / 2) * parent.getWidth());
        final int t = (int)((trackingRect.centerY() - trackingRect.height() / 2) * parent.getHeight());
        final int r = (int)((trackingRect.centerX() + trackingRect.width() / 2) * parent.getWidth());
        final int b = (int)((trackingRect.centerY() + trackingRect.height() / 2) * parent.getHeight());

        TrackingTestActivity.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (progressStatus.getExecutionState() == DJIActiveTrackMissionExecutionState.TrackingWithLowConfidence ||
                        progressStatus.getExecutionState() == DJIActiveTrackMissionExecutionState.CannotContinue) {
                    iv.setBackgroundColor(0x55ff0000);
                    iv.setClickable(false);
                    iv.setText("");
                } else if (progressStatus.getExecutionState() == DJIActiveTrackMissionExecutionState.WaitingForConfirmation) {
                    iv.setBackgroundColor(0x5500ff00);
                    iv.setClickable(true);
                    iv.setText("OK");
                } else {
                    iv.setBackgroundResource(R.drawable.visual_track_now);
                    iv.setClickable(false);
                    iv.setText("");
                }
                if (progressStatus.getExecutionState() == DJIActiveTrackMissionExecutionState.TargetLost) {
                    iv.setVisibility(View.INVISIBLE);
                } else {
                    iv.setVisibility(View.VISIBLE);
                }
                iv.setX(l);
                iv.setY(t);
                iv.getLayoutParams().width = r - l;
                iv.getLayoutParams().height = b - t;
                iv.requestLayout();
            }
        });

    }

    float downX;
    float downY;

    private double calcManhattanDistance(double point1X, double point1Y, double point2X, double point2Y) {
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
                if (calcManhattanDistance(downX, downY, event.getX(), event.getY()) < 20 && !isDrawingRect) {
                    return true;
                }
                isDrawingRect = true;
                mSendRectIV.setVisibility(View.VISIBLE);
                int l = (int)(downX < event.getX() ? downX : event.getX());
                int t = (int)(downY < event.getY() ? downY : event.getY());
                int r = (int)(downX >= event.getX() ? downX : event.getX());
                int b = (int)(downY >= event.getY() ? downY : event.getY());
                mSendRectIV.setX(l);
                mSendRectIV.setY(t);
                mSendRectIV.getLayoutParams().width = r - l;
                mSendRectIV.getLayoutParams().height = b - t;
                mSendRectIV.requestLayout();

                break;

            case MotionEvent.ACTION_UP:
                if (mMissionManager != null) {
                    DJIActiveTrackMission activeTrackMission = isDrawingRect ? new DJIActiveTrackMission(getActiveTrackRect(mSendRectIV))
                            : new DJIActiveTrackMission(new PointF(downX / mBgLayout.getWidth(), downY / mBgLayout.getHeight()));
                    activeTrackMission.isRetreatEnabled = mPushBackSw.isChecked();
                    mMissionManager.prepareMission(activeTrackMission, null, new DJICompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                mMissionManager.startMissionExecution(new DJICompletionCallback() {

                                    @Override
                                    public void onResult(final DJIError error) {
                                        TrackingTestActivity.this.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (error == null) {
                                                    mStopBtn.setVisibility(View.VISIBLE);
                                                    mPushBackSw.setVisibility(View.INVISIBLE);
                                                    mPushBackTv.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        });
                                        setResultToToast("Start: " + (error == null ? "Success" : error.getDescription()));
                                    }
                                });
                            } else {
                                setResultToToast("Prepare: " + (error == null ? "Success" : error.getDescription()));
                            }
                        }
                    });
                } else {
                    setResultToToast("No mission manager!!!");
                }
                mSendRectIV.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }

        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tracking_stop_btn:
                mMissionManager.stopMissionExecution(new DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "Success!" : error.getDescription());
                    }
                });
                break;
            case R.id.tracking_confirm_btn:
                DJIActiveTrackMission.acceptConfirmation(new DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "Success!" : error.getDescription());
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

}
