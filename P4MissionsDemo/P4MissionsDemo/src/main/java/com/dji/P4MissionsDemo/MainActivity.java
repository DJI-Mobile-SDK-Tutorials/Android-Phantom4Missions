package com.dji.P4MissionsDemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import dji.common.error.DJIError;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class MainActivity extends DemoBaseActivity {
	
    public static final String TAG = MainActivity.class.getName();
    
    public String mString = null;
    private BaseProduct mProduct;
        
    private ArrayList<DemoInfo> demos = new ArrayList<DemoInfo>();

    private ListView mListView;
    
    private DemoListAdapter mDemoListAdapter = new DemoListAdapter();
    
    private TextView mFirmwareVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // When the compile and target version is higher than 22, please request the
        // following permissions at runtime to ensure the
        // SDK work well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.VIBRATE,
                            Manifest.permission.INTERNET, Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.WAKE_LOCK, Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.SYSTEM_ALERT_WINDOW,
                            Manifest.permission.READ_PHONE_STATE,
                    }
                    , 1);
        }

        setContentView(R.layout.activity_main);
        
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        
        mListView = (ListView)findViewById(R.id.listView); 
        mListView.setAdapter(mDemoListAdapter);
        
        mFirmwareVersionView = (TextView)findViewById(R.id.version_tv);
                
        loadDemoList();

        mDemoListAdapter.notifyDataSetChanged();
        
        updateVersion();

        if ((UserAccountManager.getInstance().getUserAccountState() == UserAccountState.NOT_LOGGED_IN)
                || (UserAccountManager.getInstance().getUserAccountState() == UserAccountState.TOKEN_OUT_OF_DATE)
                || (UserAccountManager.getInstance().getUserAccountState() == UserAccountState.INVALID_TOKEN)){
            loginAccount();
        }
        
    }    
    
    private void loadDemoList() {
        mListView.setOnItemClickListener(new OnItemClickListener() {  
            public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3) {  
                onListItemClick(index);
            }
        });
        demos.clear();
        demos.add(new DemoInfo(R.string.title_activity_tracking_test, R.string.demo_desc_tracking, TrackingTestActivity.class));
        demos.add(new DemoInfo(R.string.title_activity_pointing_test, R.string.demo_desc_pointing, PointingTestActivity.class));
    }
    
    private void onListItemClick(int index) {
        Intent intent = null;
        intent = new Intent(MainActivity.this, demos.get(index).demoClass);
        this.startActivity(intent);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    public void onReturn(View view){
        Log.d(TAG ,"onReturn");  
        this.finish();
    }

    @SuppressLint("ViewHolder")
    private class DemoListAdapter extends BaseAdapter {
        public DemoListAdapter() {
            super();
        }

        @Override
        public View getView(int index, View convertView, ViewGroup parent) {
            convertView = View.inflate(MainActivity.this, R.layout.demo_info_item, null);
            TextView title = (TextView)convertView.findViewById(R.id.title);
            TextView desc = (TextView)convertView.findViewById(R.id.desc);

            title.setText(demos.get(index).title);
            desc.setText(demos.get(index).desc);
            return convertView;
        }
        @Override
        public int getCount() {
            return demos.size();
        }
        @Override
        public Object getItem(int index) {
            return  demos.get(index);
        }

        @Override
        public long getItemId(int id) {
            return id;
        }
    }
    
    private static class DemoInfo{
        private final int title;
        private final int desc;
        private final Class<? extends android.app.Activity> demoClass;

        public DemoInfo(int title , int desc,Class<? extends android.app.Activity> demoClass) {
            this.title = title;
            this.desc  = desc;
            this.demoClass = demoClass;
        }
    }
    
    @Override
    protected void onProductChange() {
        super.onProductChange();
        loadDemoList();
        mDemoListAdapter.notifyDataSetChanged();
        updateVersion();
    }

    private void setResultToToast(final String string) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this, new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
            @Override
            public void onSuccess(UserAccountState userAccountState) {
                Log.d(TAG ,"Login Success");
            }

            @Override
            public void onFailure(DJIError djiError) {
                setResultToToast("Login Failed: " +  djiError.getDescription());
            }
        });
    }

    String version = null;

    private void updateVersion() {

        BaseProduct product = DJISDKManager.getInstance().getProduct();
        if(product != null) {
            version = product.getFirmwarePackageVersion();
        }
        
        if(version == null) {
            version = "N/A";
        }
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFirmwareVersionView.setText("Firmware version: " + version);
            }
        });
        
    }
}
