package com.dji.P4MissionsDemo;

import dji.sdk.missionmanager.DJIMission.DJIMissionProgressStatus;
import dji.sdk.missionmanager.DJIMissionManager;
import dji.sdk.missionmanager.DJIMissionManager.MissionProgressStatusCallback;
import dji.sdk.missionmanager.DJITapFlyMission;
import dji.sdk.missionmanager.DJITapFlyMission.DJITapFlyMissionProgressStatus;
import dji.common.util.DJICommonCallbacks.DJICompletionCallback;
import dji.sdk.base.DJIBaseProduct;
import dji.common.error.DJIError;

import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class PointingTestActivity extends DemoBaseActivity implements SurfaceTextureListener, OnClickListener, OnTouchListener, MissionProgressStatusCallback, DJICompletionCallback {
    
    private static final String TAG = "PointingTestActivity";
    
    private DJIMissionManager mMissionManager;
    private DJITapFlyMission mTapFlyMission;

    private ImageButton mPushDrawerIb;
    private SlidingDrawer mPushDrawerSd;
    private Button mStartBtn;
    private ImageButton mStopBtn;
    private TextView mPushTv;
    private RelativeLayout mBgLayout;
    private ImageView mRstPointIv;
    private TextView mAssisTv;
    private Switch mAssisSw;
    private TextView mSpeedTv;
    private SeekBar mSpeedSb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setContentView(R.layout.activity_pointing_test);
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

    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string) {
        PointingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(PointingTestActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setResultToText(final String string) {
        if (mPushTv == null) {
            setResultToToast("Push info tv has not be init...");
        }
        PointingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushTv.setText(string);
            }
        });
    }

    private void setVisible(final View v, final boolean visible) {
        if (v == null) return;
        PointingTestActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                v.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        });
    }

    private void initUI() {
        mPushDrawerIb = (ImageButton)findViewById(R.id.pointing_drawer_control_ib);
        mPushDrawerSd = (SlidingDrawer)findViewById(R.id.pointing_drawer_sd);
        mStartBtn = (Button)findViewById(R.id.pointing_start_btn);
        mStopBtn = (ImageButton)findViewById(R.id.pointing_stop_btn);
        mPushTv = (TextView)findViewById(R.id.pointing_push_tv);
        mBgLayout = (RelativeLayout)findViewById(R.id.pointing_bg_layout);
        mRstPointIv = (ImageView)findViewById(R.id.pointing_rst_point_iv);
        mAssisTv = (TextView)findViewById(R.id.pointing_assistant_tv);
        mAssisSw = (Switch)findViewById(R.id.pointing_assistant_sw);
        mSpeedTv = (TextView)findViewById(R.id.pointing_speed_tv);
        mSpeedSb = (SeekBar)findViewById(R.id.pointing_speed_sb);

        mPushDrawerIb.setOnClickListener(this);
        mStartBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mBgLayout.setOnTouchListener(this);
        mSpeedSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSpeedTv.setText(progress + 1 + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                DJITapFlyMission.setAutoFlightSpeed(getSpeed(), new DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast(error == null ? "Success" : error.getDescription());
                    }
                });
            }
        });
    }
    
    private void initMissionManager() {
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        
        if (product == null || !product.isConnected()) {
            setResultToToast("Disconnected");
            mMissionManager = null;
            return;
        } else {
            mMissionManager = product.getMissionManager();
            mMissionManager.setMissionProgressStatusCallback(this);
            mMissionManager.setMissionExecutionFinishedCallback(this);
        }
        mTapFlyMission = new DJITapFlyMission();
    }

    /**
     * @Description : MissionExecutionFinishedCallback Method
     */
    @Override
    public void onResult(DJIError error) {
        setResultToText("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        setVisible(mRstPointIv, false);
        setVisible(mStopBtn, false);
        setVisible(mAssisTv, true);
        setVisible(mAssisSw, true);
    }

    /**
     * @Description MissionProgressStatusCallback Method
     */
    @Override
    public void missionProgressStatus(DJIMissionProgressStatus progressStatus) {
        if (progressStatus instanceof DJITapFlyMissionProgressStatus) {
            DJITapFlyMissionProgressStatus pointingStatus = (DJITapFlyMissionProgressStatus)progressStatus;
            StringBuffer sb = new StringBuffer();
            Utils.addLineToSB(sb, "Flight state", pointingStatus.getExecutionState().name());
            Utils.addLineToSB(sb, "pointing direction X", pointingStatus.getDirection().x);
            Utils.addLineToSB(sb, "pointing direction Y", pointingStatus.getDirection().y);
            Utils.addLineToSB(sb, "pointing direction Z", pointingStatus.getDirection().z);
            Utils.addLineToSB(sb, "point x", pointingStatus.getImageLocation().x);
            Utils.addLineToSB(sb, "point y", pointingStatus.getImageLocation().y);
            Utils.addLineToSB(sb, "Bypass state", pointingStatus.getBypassDirection().name());
            Utils.addLineToSB(sb, "Error", pointingStatus.getError() == null ? "No Errors" : pointingStatus.getError().getDescription());
            setResultToText(sb.toString());
            showPointByTapFlyPoint(pointingStatus.getImageLocation(), mRstPointIv);
        }
    }

    private PointF getTapFlyPoint(View iv) {
        if (iv == null) return null;
        View parent = (View)iv.getParent();
        float centerX = iv.getLeft() + iv.getX()  + ((float)iv.getWidth()) / 2;
        float centerY = iv.getTop() + iv.getY() + ((float)iv.getHeight()) / 2;
        centerX = centerX < 0 ? 0 : centerX;
        centerX = centerX > parent.getWidth() ? parent.getWidth() : centerX;
        centerY = centerY < 0 ? 0 : centerY;
        centerY = centerY > parent.getHeight() ? parent.getHeight() : centerY;
        
        return new PointF(centerX / parent.getWidth(), centerY / parent.getHeight());
    }
    
    private void showPointByTapFlyPoint(final PointF point, final ImageView iv) {
        if (point == null || iv == null) {
            return;
        }
        final View parent = (View)iv.getParent();
         PointingTestActivity.this.runOnUiThread(new Runnable() {

             @Override
             public void run() {
                 iv.setX(point.x * parent.getWidth() - iv.getWidth() / 2);
                 iv.setY(point.y * parent.getHeight() - iv.getHeight() / 2);
                 iv.setVisibility(View.VISIBLE);
                 iv.requestLayout();
             }
         });
    }
    
    private float getSpeed() {
        if (mSpeedSb == null) return Float.NaN;
        return mSpeedSb.getProgress() + 1;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.pointing_bg_layout) {
            
            switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mMissionManager != null) {
                    mStartBtn.setVisibility(View.VISIBLE);
                    mStartBtn.setX(event.getX() - mStartBtn.getWidth() / 2);
                    mStartBtn.setY(event.getY() - mStartBtn.getHeight() / 2);
                    mStartBtn.requestLayout();
                    mTapFlyMission.imageLocationToCalculateDirection = getTapFlyPoint(mStartBtn);
                    mMissionManager.prepareMission(mTapFlyMission, null, new DJICompletionCallback() {
                        
                        @Override
                        public void onResult(DJIError error) {
                            if (error == null) {
                                setVisible(mStartBtn, true);
                            } else {
                                setVisible(mStartBtn, false);
                            }
                            setResultToToast(error == null ? "Success" : error.getDescription());
                        }
                    });
                } else {
                    setResultToToast("Mission manager is null");
                }
                break;

            default:
                break;
            }
        }
        return true;
    }
 
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.pointing_drawer_control_ib) {
            if (mPushDrawerSd.isOpened()) {
                mPushDrawerSd.animateClose();
            } else {
                mPushDrawerSd.animateOpen();
            }
            return;
        }
        if (mMissionManager != null) {
            switch (v.getId()) { 
            case R.id.pointing_start_btn:
                mTapFlyMission.autoFlightSpeed = getSpeed();
                mTapFlyMission.isHorizontalObstacleAvoidanceEnabled = mAssisSw.isChecked();

                mMissionManager.startMissionExecution(new DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        if (error == null) {
                            setVisible(mStartBtn, false);
                            setVisible(mStopBtn, true);
                            setVisible(mAssisTv, false);
                            setVisible(mAssisSw, false);
                        } else {
                            setVisible(mStartBtn, true);
                            setVisible(mStopBtn, false);
                            setVisible(mAssisTv, true);
                            setVisible(mAssisSw, true);
                        }
                        setResultToToast("Start: " + (error == null ? "Success" : error.getDescription()));
                    }
                });
                break;
            case R.id.pointing_stop_btn:
                mMissionManager.stopMissionExecution(new DJICompletionCallback() {

                    @Override
                    public void onResult(DJIError error) {
                        setResultToToast("Stop: " + (error == null ? "Success" : error.getDescription()));
                    } 
                });
                break; 
 
            default:
                break;
            }
        } else {
            setResultToToast("Mission manager is null");
        }
    }

}
