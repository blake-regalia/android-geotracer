package net.blurcast.tracer.activity;

import net.blurcast.tracer.R;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderDefinition;
import net.blurcast.tracer.location_provider.map.MapProviderActivity;
import net.blurcast.tracer.service.MainService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class ServiceControlActivity extends Activity {
	
	public static final String DB_KEY_LOCATION_PROVIDER = "locationProvider";
	
	public static final String URI = ServiceControlActivity.class.getName();
	public static final String PREFERENCE_URI = URI+"#preferences";
	public static final String URI_LOCATION_PROVIDER = "locationProvider";

	private Context mContext;
	private Activity mActivity;
	private BroadcastIntentReceiver mBroadcastIntentReceiver;

	private TextView mDebugPrint;
	private Switch mSwitchService;
	private TextView mSelectService;

	private StringBuffer debugPrintList;
	private String mLocationProviderUri;
	private int locationProviderIndex = 0;
	private LocationProviderDefinition mLocationProviderDefinition;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.service_controls);
		
		// reference the preferences of this activity
		SharedPreferences settings = getSharedPreferences(PREFERENCE_URI, Context.MODE_PRIVATE);
		

		/* switch */

		mSwitchService = (Switch) this.findViewById(R.id.switch_serviceControl);

		mSwitchService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			public synchronized void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				
				Log.i("Switch Button", "turned: "+isChecked+"; button view: "+buttonView.isChecked());

				// disable toggle button to force user to wait until service has finished command
				mSwitchService.setEnabled(false);

				// enable service
				if(isChecked) {
					mSelectService.setEnabled(false);
					sendCommandToService(MainService.COMMAND.ENABLE_SERVICE, mLocationProviderDefinition.getUri());
				}
				// disable service
				else {
					sendCommandToService(MainService.COMMAND.DISABLE_SERVICE, null);
				}
			}
		});
		
		
		// reference the clickable text view that shows the location provider
		mSelectService = (TextView) this.findViewById(R.id.textView_selectService);
		
		// obtain the uri of the established location provider if it exists 
		mLocationProviderUri = settings.getString(URI_LOCATION_PROVIDER, null);

		// if the location provider is set...
		if(mLocationProviderUri != null) {
			
			// point the location provider object to the given uri if it exists
			mLocationProviderDefinition = DefineLocationProviders.getLocationProviderDefinition(mLocationProviderUri);
			if(mLocationProviderDefinition != null) {
				mSelectService.setText(mLocationProviderDefinition.getName());
				mSwitchService.setEnabled(true);
			}
		}
		
		// setup the onclick listener
		mSelectService.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent selectLocationProviderIntent = new Intent(mContext, SelectLocationProviderActivity.class);
				startActivity(selectLocationProviderIntent);
			}
		});
		

		// register a broadcast receiver to receive intents from the service
		mBroadcastIntentReceiver = new BroadcastIntentReceiver(this, MainService.BROADCAST_URI, new BroadcastIntentReceiver.BroadcastCallback() {
			@Override
			public void debugPrint(String text) {
				debugPrintList.append(text+"\n");
				mDebugPrint.setText(debugPrintList.toString());
				int scrollAmt = mDebugPrint.getLayout().getLineTop(mDebugPrint.getLineCount()) - mDebugPrint.getHeight();
				if(scrollAmt > 0) {
					mDebugPrint.scrollTo(0,  scrollAmt);
				}
			}
			
			@Override
			public void serviceDisabled() {
				updateServiceStatusDisplay(false);
			}

			@Override
			public void serviceEnabled() {
				updateServiceStatusDisplay(false);
			}
			
			@Override
			public void serviceStarting() {
				mSwitchService.setEnabled(false);
			}
		});

		
		mContext = this;
		mActivity = this;

		mDebugPrint = (TextView) this.findViewById(R.id.textView_debugPrint);
		mDebugPrint.setMovementMethod(new ScrollingMovementMethod());

		debugPrintList = new StringBuffer();


		// reference the activity launch button and bind listeners
		TextView tv = (TextView) this.findViewById(R.id.textView_launchActivity);
		if(tv != null) {
			tv.setOnClickListener(new OnClickListener() {			
				public void onClick(View v) {
					Intent intent = new Intent(mContext, MapProviderActivity.class);
					startActivity(intent);
				}
			});
		}
		else {
			Log.e("WTF", "No text view found");
		}
	}

	
	/**
	 * 
	 * @param command
	 */
	public void sendCommandToService(String command, String uri) {
		// create a new intent for the service
		Intent serviceIntent = new Intent(this, MainService.class);

		// put the command in the intent
		serviceIntent.setAction(command);

		// set the extra int argument
		serviceIntent.putExtra(MainService.INTENT_NAME_MESSAGE.STRING, uri);

		// send the intent to the service
		this.startService(serviceIntent);
	}


	private void updateServiceStatusDisplay(boolean status) {
		 if(false) {
			// update the toggle status
			mSwitchService.setChecked(status);
		 }

		// enable the button again
		mSwitchService.setEnabled(true);
	}
	
	protected void onStart() { 
		super.onStart();
		mBroadcastIntentReceiver.reRegister();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		mBroadcastIntentReceiver.close();
	}


}
