package net.blurcast.tracer.location_provider.map;

import net.blurcast.android.gvg.ControlPoint;
import net.blurcast.android.view.TouchMatrixViewGVG;
import net.blurcast.tracer.R;
import net.blurcast.tracer.activity.BroadcastIntentReceiver;
import net.blurcast.tracer.activity.BroadcastIntentReceiver.BroadcastCallback;
import net.blurcast.tracer.activity.ServiceIntentManager;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderDefinition;
import net.blurcast.tracer.sensor.wap.SensorLooper_WAP;
import net.blurcast.tracer.service.MainService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MapProviderActivity extends Activity implements TouchMatrixViewGVG.Callback {

    /* xml inflated views */
	private TouchMatrixViewGVG mTouchMatrixView;	
	private TextView mMapStatusTop;
	private Button mRecordingControl;
	private TextView mWapInfo;

    /* helper objects */
	private ServiceIntentManager mServiceIntentManager;
	private BroadcastIntentReceiver mBroadcastIntentReceiver;
	
	private LocationProviderDefinition mDefinition;
	private String mapFile;
	private int numSamples = 0;

	private float[] location = new float[]{0.f, 0.f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_provider_map);
        
        
        /* xml inflated views */
        
        mTouchMatrixView = (TouchMatrixViewGVG) this.findViewById(R.id.TouchMatrixViewGVG);
        mTouchMatrixView.setControlPointCallback(this);
        
        mMapStatusTop = (TextView) this.findViewById(R.id.textView_mapStatusTop);
        
        mRecordingControl = (Button) this.findViewById(R.id.button_recordingControl);
        mRecordingControl.setOnClickListener(new OnClickListener() {			
			public void onClick(View v) {
		        mServiceIntentManager.sendCommandToService(MainService.COMMAND.RESUME_RECORDING, location);
			}
		});
        
        mWapInfo = (TextView) this.findViewById(R.id.textView_wapInfo);
        
        
        /* helper objects */ 
        
        mBroadcastIntentReceiver = new BroadcastIntentReceiver(this, MainService.BROADCAST_URI, new BroadcastCallback() {
        	@Override
        	public void locationProviderPaused() {
        		mRecordingControl.setEnabled(true);
        		mRecordingControl.setText("Sample this Location");
        		numSamples = 0;
        	}
        	
        	@Override
        	public void locationProviderResumed() {
        		mRecordingControl.setEnabled(false);
        		mRecordingControl.setText("Sampling...");
    			mWapInfo.setText("...");
        	}
        	
        	@Override
        	public void sensorLooperEvent(String sensorLooperClassUri, Bundle detail) {
        		numSamples += 1;
        		
        		// if this is a wap event
        		if(sensorLooperClassUri.equals(SensorLooper_WAP.class.getName())) {
        			Log.i("Map Provider Activity", "event received from wap event!");
        			int[] levels = detail.getIntArray(SensorLooper_WAP.BUNDLE_EXTRA_NAME.LEVELS);
        			mWapInfo.setText("sample #"+numSamples+". "+levels.length+" Wap(s)");
        		}
        	}
		});

        mServiceIntentManager = new ServiceIntentManager(this, MainService.class);
        mServiceIntentManager.sendCommandToService(MainService.COMMAND.REQUEST_RECORDING_STATUS);
        
        
        SharedPreferences settings = DefineLocationProviders.MAP.getSettings(this);
        mapFile = settings.getString(SensorLocationProvider_Map.PREF_URI_MAP_FILE, null);
        
        if(mapFile == null) {
            Intent intent = new Intent();
            intent.setType("file/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select SVG"), 1);
        }
        else {
        	mTouchMatrixView.loadGVG(mapFile);
        }
    }
    

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == RESULT_OK) {
    		if(requestCode == 1) {
    			
    			if(data == null)
    				return;
    			
    			Uri imgUri = data.getData();
    			
    			String fmgrPath = imgUri.getPath();
        		String imgPath = getPathOfUri(imgUri);
        		
        		if(imgPath != null) {
        			mTouchMatrixView.loadGVG(imgPath);
        		}
        		else {
        			mTouchMatrixView.loadGVG(fmgrPath);
        		}
    		}
    	}
    }
    
    /** borrowed from stack-overflow
     * http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
     */
    public String getPathOfUri(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor != null) {
        	int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        	cursor.moveToFirst();
        	return cursor.getString(column_index);
        }
        else return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_map_provider, menu);
        return true;
    }


	public void controlPointSelected(ControlPoint controlPoint) {
		String name = controlPoint.getName();
		if(name == null) {
			name = "?";
		}
		location[0] = controlPoint.getX();
		location[1] = controlPoint.getY();
		mMapStatusTop.setText(name);
		
		mServiceIntentManager.sendCommandToService(MainService.COMMAND.REQUEST_RECORDING_STATUS);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mBroadcastIntentReceiver.close();
	}
}
