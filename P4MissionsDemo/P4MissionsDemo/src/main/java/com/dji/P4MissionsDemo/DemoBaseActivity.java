package com.dji.P4MissionsDemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import dji.midware.media.DJIVideoDataRecver;
import dji.sdk.Camera.DJICamera;
import dji.sdk.Camera.DJICamera.CameraReceivedVideoDataCallback;
import dji.sdk.Codec.DJICodecManager;
import dji.sdk.Products.DJIAircraft;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.Model;

public class DemoBaseActivity extends FragmentActivity implements SurfaceTextureListener {

    private DJIBaseProduct mProduct;
    private DJICamera mCamera;
    
    //To store index chosen in PopupNumberPicker listener
    protected static int[] INDEX_CHOSEN = {-1, -1, -1};
    
    protected TextView mConnectStatusTextView;
    
    protected TextureView mVideoSurface = null;
    protected CameraReceivedVideoDataCallback mReceivedVideoDataCallBack = null;
    protected DJICodecManager mCodecManager = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         
        IntentFilter filter = new IntentFilter();  
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mVideoSurface = (TextureView) findViewById(R.id.video_previewer_surface);
        
        if (null != mVideoSurface) {
            mVideoSurface.setSurfaceTextureListener(this);
            
            mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);

            mReceivedVideoDataCallBack = new CameraReceivedVideoDataCallback() {

                @Override
                public void onResult(byte[] videoBuffer, int size) {
                    if(mCodecManager != null){
                        mCodecManager.sendDataToDecoder(videoBuffer, size);
                    }
                }
            };
        }
        initPreviewer();
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            updateTitleBar();
            onProductChange();
        }
        
    };
    
    protected void onProductChange() {
        
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        updateTitleBar();
        
        
    }
    
    @Override
    protected void onPause() {
        try {
            DJIVideoDataRecver.getInstance().setVideoDataListener(false, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
        if(mCodecManager != null){
            mCodecManager.destroyCodec();
        }
        
        super.onPause();
    }
    
    
    
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }
    
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(DemoBaseActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void initPreviewer() {
        try {
            mProduct = DJIDemoApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
        }
        
        if (null == mProduct || !mProduct.isConnected()) {
            mCamera = null;
            showToast("Disconnect");
        } else {
        	if (!mProduct.getModel().equals(Model.UnknownAircraft)) {
	            mCamera = mProduct.getCamera();
	            if (mCamera != null){
	            	mCamera.setDJICameraReceivedVideoDataCallback(mReceivedVideoDataCallBack);
	            }
        	}
        }
    }
    
    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureAvailable(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        if (mCodecManager == null) {
            mCodecManager = new DJICodecManager(this, surface, width, height);
        }
    }

    /**
     * @param surface
     * @param width
     * @param height
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureSizeChanged(android.graphics.SurfaceTexture,
     *      int, int)
     */
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    /**
     * @param surface
     * @return
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureDestroyed(android.graphics.SurfaceTexture)
     */
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mCodecManager != null)
            mCodecManager.cleanSurface();
        return false;
    }

    /**
     * @param surface
     * @see android.view.TextureView.SurfaceTextureListener#onSurfaceTextureUpdated(android.graphics.SurfaceTexture)
     */
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
    
    private void updateTitleBar() {
        if(mConnectStatusTextView == null) return;
        boolean ret = false;
        DJIBaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null) {
            if(product.isConnected()) {
                mConnectStatusTextView.setText(DJIDemoApplication.getProductInstance().getModel().getDisplayName() + " Connected");
                ret = true;
            } else {
                if(product instanceof DJIAircraft) {
                    DJIAircraft aircraft = (DJIAircraft)product;
                    if(aircraft.getRemoteController() != null) {
                    }
                    if(aircraft.getRemoteController() != null && aircraft.getRemoteController().isConnected()) {
                        mConnectStatusTextView.setText("only RC Connected");
                        ret = true;
                    }
                }
            }
        }
        
        if(!ret) {
            mConnectStatusTextView.setText("Disconnected"); 
        }
    }
    
    /**
     * @Description : RETURN BTN RESPONSE FUNCTION
     * @author : andy.zhao
     * @param view
     * @return : void
     */
    public void onReturn(View view) {
        this.finish();
    }
    
    public void resetIndex() {
        INDEX_CHOSEN = new int[3];
        INDEX_CHOSEN[0] = -1;
        INDEX_CHOSEN[1] = -1;
        INDEX_CHOSEN[2] = -1;
    }
    
    public ArrayList<String> makeListHelper(Object[] o) {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < o.length - 1; i++) {
            list.add(o[i].toString());
        }
        return list;
    }
}
