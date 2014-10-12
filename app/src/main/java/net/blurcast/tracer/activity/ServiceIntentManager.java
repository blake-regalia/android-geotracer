package net.blurcast.tracer.activity;

import net.blurcast.tracer.service.MainService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class ServiceIntentManager {
	
	private Context mContext;
	private Class<?> mService;
	
	public ServiceIntentManager(Context context, Class<?> service) {
		mContext = context;
		mService = service;
	}
	
	private Intent createIntent(String command) {
		
		// create a new intent for the service
		Intent serviceIntent = new Intent(mContext, mService);

		return serviceIntent.setAction(command);
	}

	/**
	 * 
	 * @param command
	 */
	public void sendCommandToService(String command) {
		
		// create a new intent for the service
		Intent serviceIntent = createIntent(command);

		// send the intent to the service
		mContext.startService(serviceIntent);
	}
	

	/**
	 * 
	 * @param command
	 */
	public void sendCommandToService(String command, int arg) {
		
		// create a new intent for the service
		Intent serviceIntent = createIntent(command);

		// set the extra int argument
		serviceIntent.putExtra(MainService.INTENT_NAME_MESSAGE.INT, arg);

		// send the intent to the service
		mContext.startService(serviceIntent);
	}
		
	/**
	 * 
	 * @param command
	 */
	public void sendCommandToService(String command, float[] args) {
		
		// create a new intent for the service
		Intent serviceIntent = createIntent(command);
		
		// create a bundle using float array
		Bundle bundle = new Bundle();
		bundle.putFloatArray(MainService.BUNDLE_MESSAGE.FLOAT_ARRAY, args);

		// set the extra bundle
		serviceIntent.putExtra(MainService.INTENT_NAME_MESSAGE.BUNDLE, bundle);

		// send the intent to the service
		mContext.startService(serviceIntent);
	}
}
