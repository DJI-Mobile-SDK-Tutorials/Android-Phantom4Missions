package com.dji.P4MissionsDemo;

import android.content.Context;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import dji.common.mission.activetrack.ActiveTrackTargetState;
import dji.common.mission.activetrack.SubjectSensingState;

public class MultiTrackingView extends RelativeLayout {

    private TextView mValueIndex;
    private ImageView mRectF;

    public MultiTrackingView(Context context) {
        super(context);
        View view = LayoutInflater.from(context).inflate(R.layout.layout_multi_tracking, null);
        this.addView(view);
        mValueIndex = (TextView) findViewById(R.id.index_textview);
        mRectF = (ImageView) findViewById(R.id.tracking_rectf_iv);
    }

    public void updateView(SubjectSensingState information) {
        ActiveTrackTargetState targetState = information.getState();

        if ((targetState == ActiveTrackTargetState.CANNOT_CONFIRM)
                || (targetState == ActiveTrackTargetState.UNKNOWN)) {
            mRectF.setImageResource(R.drawable.visual_track_cannotconfirm);
        } else if (targetState == ActiveTrackTargetState.WAITING_FOR_CONFIRMATION) {
            mRectF.setImageResource(R.drawable.visual_track_needconfirm);
        } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_LOW_CONFIDENCE) {
            mRectF.setImageResource(R.drawable.visual_track_lowconfidence);
        } else if (targetState == ActiveTrackTargetState.TRACKING_WITH_HIGH_CONFIDENCE) {
            mRectF.setImageResource(R.drawable.visual_track_highconfidence);
        }

        mValueIndex.setText("" + information.getIndex());
    }
}
