package net.blurcast.tracer.activity;

import net.blurcast.tracer.service.MainService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class BroadcastIntentReceiver extends BroadcastReceiver {

	private static final String TAG = "BroadcastIntentReceiver";
	
	private static final String EXTRA_NAME_INT_ANNOUNCEMENT = MainService.INTENT_NAME_MESSAGE.INT;
	private static final int    DEFAULT_VALUE_ANNOUNCEMENT  = MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.NONE;
	
	private final String intentFilterUri;
	private Context mContext;
	private BroadcastCallback mCallback;

	public BroadcastIntentReceiver(Context context, String uri, BroadcastCallback callback) {
		intentFilterUri = uri;
		mContext = context;
		mCallback = callback;
		mContext.registerReceiver(this, new IntentFilter(intentFilterUri));
	}
	
	public void reRegister() {
		mContext.registerReceiver(this, new IntentFilter(intentFilterUri));
	}
	
	public void close() {
		mContext.unregisterReceiver(this);
	}
	
	public static abstract class BroadcastCallback {
		public void debugPrint(String print){}
		public void errorProviderExists(){}
		public void serviceDisabled(){}
		public void serviceStarting(){}
		public void serviceEnabled(){}
		public void locationProviderPaused(){}
		public void locationProviderResumed(){}
		public void sensorLooperEvent(String sensorLooperClassUri, Bundle detail) {}
	}

	/**
	 * handle broadcasts from service
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		// 
		String action = intent.getAction();

		if(action.equals(intentFilterUri)) {

			int announcement = intent.getIntExtra(EXTRA_NAME_INT_ANNOUNCEMENT, DEFAULT_VALUE_ANNOUNCEMENT);

			switch(announcement) {

				// service is sending debug message
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.DEBUG_PRINT:
				String text = intent.getStringExtra(MainService.INTENT_NAME_MESSAGE.STRING);
				mCallback.debugPrint(text);
				break;

				// service encountered provider exists error
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.ERROR_PROVIDER_EXISTS:
				Log.i(TAG, "err provider exists");
				mCallback.errorProviderExists();
				break;

				// service is disabled
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_DISABLED:
				Log.i(TAG, "service disabled");
				mCallback.serviceDisabled();
				break;

				// service is enabled
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_STARTING:
				Log.i(TAG, "service starting");
				mCallback.serviceStarting();
				break;

				// service is enabled
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_ENABLED:
				Log.i(TAG, "service enabled");
				mCallback.serviceEnabled();
				break;
				
				// recording is paused
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.RECORDING_PAUSED:
				Log.i(TAG, "recording paused");
				mCallback.locationProviderPaused();
				break;
				
				// recording is resumed
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.RECORDING_RESUMED:
				Log.i(TAG, "recording resumed");
				mCallback.locationProviderResumed();
				break;
				
			case MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SENSOR_LOOPER_EVENT:
				Log.i(TAG, "sensor looper event");
				String sensorLooperClassUri = intent.getStringExtra(MainService.INTENT_NAME_MESSAGE.CLASS_URI);
				Bundle detail = intent.getBundleExtra(MainService.INTENT_NAME_MESSAGE.BUNDLE);
				mCallback.sensorLooperEvent(sensorLooperClassUri, detail);
				break;

			default:
				Log.i(TAG, "unknown: "+announcement);
				break;
			}
		}
	}
}
