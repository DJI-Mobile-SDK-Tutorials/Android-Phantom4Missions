package com.dji.P4MissionsDemo;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import dji.sdk.SDKManager.DJISDKManager;
import dji.sdk.base.DJIBaseProduct;
import dji.sdk.base.DJIBaseProduct.DJIVersionCallback;

public class MainActivity extends DemoBaseActivity implements View.OnClickListener, DJIVersionCallback {
	
    public static final String TAG = MainActivity.class.getName();
    
    public String mString = null;
    private DJIBaseProduct mProduct;
        
    private ArrayList<DemoInfo> demos = new ArrayList<DemoInfo>();

    private ListView mListView;
    
    private DemoListAdapter mDemoListAdapter = new DemoListAdapter();
    
    private TextView mFirmwareVersionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mConnectStatusTextView = (TextView) findViewById(R.id.ConnectStatusTextView);
        
        mListView = (ListView)findViewById(R.id.listView); 
        mListView.setAdapter(mDemoListAdapter);
        
        mFirmwareVersionView = (TextView)findViewById(R.id.version_tv);
                
        loadDemoList();
        mDemoListAdapter.notifyDataSetChanged();
        
        updateVersion();
        DJIBaseProduct product = DJISDKManager.getInstance().getDJIProduct();
        if(product != null) {
            product.setDJIVersionCallback(this);
        }
        
    }    
    
    private void loadDemoList() {
        mListView.setOnItemClickListener(new OnItemClickListener() {  
            public void onItemClick(AdapterView<?> arg0, View v, int index, long arg3) {  
                onListItemClick(index);
            }
        });
        demos.clear();
        // SDK_TODO Just for testing
        demos.add(new DemoInfo(R.string.title_activity_tracking_test, R.string.demo_desc_tracking, TrackingTestActivity.class));
        demos.add(new DemoInfo(R.string.title_activity_pointing_test, R.string.demo_desc_pointing, PointingTestActivity.class));
    }
    
    private void onListItemClick(int index) {
        Intent intent = null;
        intent = new Intent(MainActivity.this, demos.get(index).demoClass);
        this.startActivity(intent);
    }

    @Override
    public void onClick(View v) {
        
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    
    public void onReturn(View view){
        Log.d(TAG ,"onReturn");  
        this.finish();
    }
    
    public void showMessage(String title, String msg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
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
        DJIBaseProduct product = DJISDKManager.getInstance().getDJIProduct();
        if(product != null) {
            product.setDJIVersionCallback(this);
        }
    }
    
    
    private boolean checkProduct() {
		try {
            mProduct = DJIDemoApplication.getProductInstance();
        } catch (Exception exception) {
            mProduct = null;
            return false;
        }
        
        try {
	        mProduct = DJIDemoApplication.getProductInstance();
	    } catch (Exception exception) {
	        mProduct = null;
	        return false;
	    }
        return true;
	}

    String version = null;
    
    private void updateVersion() {

        DJIBaseProduct product = DJISDKManager.getInstance().getDJIProduct();
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

    @Override
    public void onProductVersionChange(String oldVersion, String newVersion) {
        updateVersion();
    }
    
    
}
