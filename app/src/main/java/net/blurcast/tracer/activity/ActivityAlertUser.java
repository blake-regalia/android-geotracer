package net.blurcast.tracer.activity;

import net.blurcast.tracer.service.MainService;
import net.blurcast.tracer.R;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class ActivityAlertUser extends Activity {
	
	private static final String TAG = "ActivityAlertUser";
	
	private Activity self;
	
	private static final int ACTION_WIFI_FAIL  = 0x01;	
	private static final int ACTION_GPS_ENABLE = 0x02;
	
	private int GPS_ENABLED_REQUEST_CODE = 0x0A;

	@Override
	public void onCreate(Bundle savedInst) {
		super.onCreate(null);
		self = this;
		
		Intent intent = this.getIntent();
		if(intent != null) {
			String action = intent.getAction();			
			Log.d(TAG, "Started with action: "+action);
			
			if(action.equals("gps-enable")) {
				actionGpsEnable();
			}
			else if(action.equals("wifi-fail")) {
				actionWifiFail();
			}
		}
	}

	private void uninstall() {
		Uri packageURI = Uri.parse("package:"+this.getClass().getPackage().getName());
		Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
		startActivity(uninstallIntent);
	}

	private void startMainService(String objective) {
		Intent intent = new Intent(this, MainService.class);
		intent.putExtra("objective", objective);
		self.finish();
		startService(intent);
	}

	private void actionWifiFail() {
		this.setContentView(R.layout.wifi_fail);
		Button uninstallButton = (Button) this.findViewById(R.id.uninstallServiceButton); 
		uninstallButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				uninstall();				
			}
		});
	}
	
	private void actionGpsEnable() {
		this.setContentView(R.layout.gps_enable);
		Button enableGpsButton  = (Button) this.findViewById(R.id.enableGpsButton); 
		enableGpsButton.setOnClickListener(new OnClickListener() {
			public void onClick(View view) {
				Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				self.startActivityForResult(intent, GPS_ENABLED_REQUEST_CODE);
			}
		});
		
		final LocationManager mLocationManager;
		mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// if is actually enabled 
		if(mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
			//startMainService(MainService.COMMAND.START_GPS);
		}
		else {
			Toast.makeText(this, R.string.gps_enable_toast, Toast.LENGTH_LONG).show();
			
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, 
					new LocationListener() {
						public void onLocationChanged(Location location) {	
						}
						public void onProviderDisabled(String provider) {	
						}
						public void onProviderEnabled(String provider) {
							mLocationManager.removeUpdates(this);
							self.finishActivity(GPS_ENABLED_REQUEST_CODE);
//							startMainService(MainService.COMMAND.START_GPS);
						}
						public void onStatusChanged(String provider, int status, Bundle extras) {	
						}
					}
			);
			
			Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			this.startActivityForResult(intent, GPS_ENABLED_REQUEST_CODE);
		}
	}
	
	
}
