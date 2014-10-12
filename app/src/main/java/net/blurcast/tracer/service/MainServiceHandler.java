package net.blurcast.tracer.service;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Vector;

import net.blurcast.android.hardware.HardwareCallback;
import net.blurcast.tracer.activity.ActivityAlertUser;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderDefinition;
import net.blurcast.tracer.location_provider.SensorLocationProvider;
import net.blurcast.tracer.sensor.RecorderCallback;
import net.blurcast.tracer.sensor.SensorLooper;
import net.blurcast.tracer.sensor.SensorLooperCallback;
import net.blurcast.tracer.service.MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE;
import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class MainServiceHandler extends Handler implements HardwareCallback, RecorderCallback, SensorLooperCallback {

	private static final String TAG = "MainServiceHandler";
	

	private static final int STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_DISABLED = 0x00;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_STARTING = 0x01;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_ENABLED  = 0x02;

	private static final int STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_PAUSED      = 0x20;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_RESUMED     = 0x21;
	private static final int STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_NOT_STARTED = 0x22;
//	private static final int STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_ENABLED  = 0x22;

	private static final int OBJECTIVE_NONE      = 0x00;
	private static final int OBJECTIVE_RECOVER   = 0x03;
	private static final int OBJECTIVE_SHUTDOWN  = 0x05;

	private int sensorLocationProviderServiceStatus;
	private int sensorLocationProviderRecordingStatus;
	
	private long sensorLocationProviderTimeStarted;
	private long sensorLoopersBitmask;
	private int objectiveClose;
	private Class<?>[] sensorLooperMap; 
	
	private int lastBroadcast = MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.NONE;

	private Looper mLooper;
	private Context mContext;

	private SensorLocationProvider mSensorLocationProvider;
	private LocationProviderDefinition mDefinition;
	private Vector<SensorLooper> mSensorLoopers;
	private CallbackHandler mCallbackHandler;

	public MainServiceHandler(Looper looper, Context context) {

		// initialize primitive data type fields
		sensorLocationProviderServiceStatus = STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_DISABLED;
		sensorLocationProviderRecordingStatus = STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_NOT_STARTED;
		sensorLoopersBitmask = 0;
		objectiveClose = OBJECTIVE_NONE;

		// initialize object fields
		mLooper = looper;
		mContext = context;
		mSensorLoopers = new Vector<SensorLooper>();

		// setup an object to handle callback events from the sensorLoopers
		mCallbackHandler = new CallbackHandler(mSensorLoopers);
	}


	@Override
	public void handleMessage(Message msg) {

		// what is the message about
		switch(msg.what) {

		
			// service is being commanded to enable
		case MainService.MESSAGE.ENABLE_SERVICE:
			
			// update the status of the location provider
			sensorLocationProviderServiceStatus = STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_STARTING;
			
			// broadcast that the service is busy
			broadcastServiceStatusUpdate();
			
			// output that the service is attempting to enable
			debug("Attempting to enable service");
			
			// attempt to establish which location provider to use and enable it
			if(setLocationProvider((String) msg.obj)) {
				
				// if that worked, setup all the sensor loopers
				setSensorLoopers();
				
				debug("Enabling location provider hardware...");
				mSensorLocationProvider.attemptEnableHardware(this);
			}
			else {
				error("Location provider already enabled");
			}
			break;

			
			// service is being requested to broadcast status
		case MainService.MESSAGE.REQUEST_SERVICE_STATUS:
			
			// broadcast the status of the service
			broadcastServiceStatusUpdate();
			break;
			

			// service is being commanded to disable
		case MainService.MESSAGE.DISABLE_SERVICE:
			debug("Disabling service...");
			shutdownLoopersAndStopLocationProviderRecording(OBJECTIVE_SHUTDOWN);
			
			// broadcast that the service is disabled
			broadcastServiceStatusUpdate();
			break;
			
			
			// activity posted request to start recording
		case MainService.MESSAGE.RESUME_RECORDING:
			debug("Recording commanded to resume");
			
			if(sensorLocationProviderRecordingStatus == STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_PAUSED) {
				Bundle bundle = (Bundle) msg.obj;
				float[] xy = bundle.getFloatArray(MainService.BUNDLE_MESSAGE.FLOAT_ARRAY);
				mSensorLocationProvider.resumeRecording(xy[0], xy[1]);
			}
			break;
			

			// service is being requested to broadcast status
		case MainService.MESSAGE.REQUEST_RECORDING_STATUS:
			
			// broadcast the status of the service
			broadcastRecordingStatusUpdate();
			break;
			
		default:
			Log.w(TAG, "Out of context: "+msg.what);
			return;
		}
	}

	/**
	 * set the type of class/method to use for providing a location to the app
	 * @param locationProviderType
	 */
	private boolean setLocationProvider(String locationProviderUri) {
		
		// if the location provider is already active
		if(mSensorLocationProvider != null) {
			return false;
		}

		// set the location provider accordingly
		mDefinition = DefineLocationProviders.getLocationProviderDefinition(locationProviderUri);
		if(mDefinition != null) {
			try {
				mSensorLocationProvider = 
						(SensorLocationProvider) mDefinition.getProvider().getConstructor(SensorLooperCallback.class, Context.class, Looper.class, LocationProviderDefinition.class)
							.newInstance(this, mContext, mLooper, mDefinition);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		}
		else {
			error("location provider index not defined: "+locationProviderUri);
			return false;
		}
	}
	
	/**
	 * set the sensor loopers as defined by package defaults and  
	 */
	private void setSensorLoopers() {
		
		ArrayList<Class<?>> sensorLooperList = mDefinition.getSensorLoopers();
		
		sensorLooperMap = new Class<?>[sensorLooperList.size()];
		
		// initialize array of modular sensorLoopers
		int sensorLooperIndex = 0;
		for(Class<?> sensorLooperClass : sensorLooperList) {
			try {
				mSensorLoopers.add(
						(SensorLooper) sensorLooperClass.getConstructor(Integer.TYPE, SensorLooperCallback.class, Context.class)
							.newInstance(sensorLooperIndex, this, mContext)
						);
				sensorLooperMap[sensorLooperIndex] = sensorLooperClass;
				sensorLooperIndex += 1;
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * attempt to shutdown the service
	 * @param objective
	 */
	public void shutdownLoopersAndStopLocationProviderRecording(int objective) {
		// save the objective
		objectiveClose = objective;
		
		// loop through sensors and stop recording
		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.stopRecording(mCallbackHandler, loopIndex);
			loopIndex += 1;
		}
		
		// stop the location provider
		mSensorLocationProvider.stopRecording(this);
	}

	/**
	 * the sensorLocationProvider hardware enables 
	 */
	public void hardwareEnabled(int ignore) {
		debug("Location provider hardware enabled");
		debug("Starting to record...");
		
		mSensorLocationProvider.startRecording(this);

		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.attemptEnableHardware(mCallbackHandler, loopIndex);
			loopIndex += 1;
		}
	}

	/**
	 * the sensorLocationProvider hardware failed to enable
	 */
	public void hardwareFailedToEnable(int ignore, int reason) {
		debug("sensorLocationProvider hardware failed to enable");

		switch(reason) {
		case HardwareCallback.REASON_GPS_USER:
			// notification: PROVIDER_GPS must be enabled
			NotificationInterface.post(mContext, ActivityAlertUser.class, "gps-enable", false, "GPS needs to be enabled", "GPS must remain enabled for app to run");
			break;

		case HardwareCallback.REASON_GPS_TOGGLE_EXPLOIT_ATTEMPT:
			// restart hardware attempt
			mSensorLocationProvider.setGpsToggleExploitMotivation(false);
			mSensorLocationProvider.attemptEnableHardware(this);
			break;
		}
	}

	/**
	 * the sensorLocationProvider hardware was disabled
	 */
	public void hardwareDisabled(int ignore, int reason) {
		broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_DISABLED);
		debug("sensorLocationProvider hardware was disabled");

		mSensorLocationProvider.stopRecording(this);

		switch(reason) {
		case HardwareCallback.REASON_GPS_USER:
			// notification: You disabled PROVIDER_GPS! PROVIDER_GPS must remain enabled
			Log.e(TAG, "GPS was disabled");
			break;

		}
	}



	public void sensorLooperOpened(int index) {
		this.sensorLoopersBitmask |= (1 << index);
	}

	public void sensorLooperClosed(int index) {
		
		debug("sensorLooper("+index+") closed.");
		
		this.sensorLoopersBitmask &= ~(1 << index);

		if(this.sensorLoopersBitmask == 0) {
			switch(objectiveClose) {
			
			// this looper was intending to shutdown
			case OBJECTIVE_SHUTDOWN:
				debug("location provider stopped recording");
				mSensorLocationProvider.stopRecording(this);
				break;

				// this looper failed and is attempting recovery
			case OBJECTIVE_RECOVER:
				debug("Attempting to recover...");
				break;

				// this looper closed unexpectedly (sensorLoopers closed first) 
			case OBJECTIVE_NONE:
				/*
				switch(mSensorLoopers.get(index).getReason()) {
				case SensorLooper.REASON_DATA_FULL:
					break;
				case SensorLooper.REASON_IO_ERROR:
					break;
				}
				 */
				break;

			default:
				break;
			}
		}
	}

	/**
	 * the sensorLocationProvider has failed, shutdown all sensorLoopers
	 */
	public void sensorLooperFailed(int index, int reason) {
		shutdownLoopersAndStopLocationProviderRecording(OBJECTIVE_RECOVER);
		debug("Loopers were shut down");
	}

	/**
	 * triggered when the locationSensorProvider starts recording - this indicates service officially started
	 */
	public synchronized void recordingStarted(int ignore, long timestamp) {
		
		// output status update 
		debug("Recording started");

		// update the local status record of the service
		sensorLocationProviderServiceStatus = STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_ENABLED;
		sensorLocationProviderTimeStarted = timestamp;

		// broadcast that the service enabled
		broadcastServiceStatusUpdate();

		// attempt to start recording any sensor loopers if they are not already recording
		int loopIndex = 0;
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.startRecording(mCallbackHandler, loopIndex, sensorLocationProviderTimeStarted);
			loopIndex += 1;
		}
	}

	public void recordingFailedToStart(int ignore, int reason) {

		String reasonStr = null;
		switch(reason) {
		case RecorderCallback.REASON_UNIMPLEMENTED: reasonStr = "Unimplemented"; break;
		case RecorderCallback.REASON_RUNNING: reasonStr = "Running"; break;
		case RecorderCallback.REASON_STOPPED: reasonStr = "Stopped"; break;
		}

		debug("SensorLocationProvider failed to start recording: "+reasonStr);
	}
	
	/**
	 * gets called by location provider - used by map provider when the provider signals it is ready to record a new position 
	 */
	public void recordingPaused(int ignore) {
		
		debug("LocationProvider signaled a pause. This could indicate MapProvider is ready to record new position.");
		
		// flag the location provider is waiting for message from activity
		sensorLocationProviderRecordingStatus = STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_PAUSED;
		
		// broadcast that the looper is ready to handle recording events
		broadcastRecordingStatusUpdate();
	}

	/**
	 * gets called by location provider - used by map provider when the provider signals it started recording again
	 */
	public void recordingResumed(int index) {

		debug("SensorLocationProvider resumed recording.");

		// flag the location provider is busy recording from sensors
		sensorLocationProviderRecordingStatus = STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_RESUMED;
		
		// broadcast that the looper has resumed recording events
		broadcastRecordingStatusUpdate();
	}

	/**
	 * triggered when the locationSensorProvider stops recording
	 */
	public void recordingStopped(int ignore, int reason) {

		debug("SensorLocationProvider stopped recording.");
		broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_DISABLED);
	}
	
	/**
	 * triggered when the locationSensorProvider commands to pause the sensor loopers
	 */
	public void pauseSensorLoopers() {
		
		debug("LocationProvider demands: pause sensor loopers");
		
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.pauseRecording();
		}
	}


	public void resumeSensorLoopers() {
		// TODO Auto-generated method stub

		debug("Resuming sensor loopers");
		
		for(SensorLooper tSensorLooper : mSensorLoopers) {
			tSensorLooper.resumeRecording();
		}
	}


	public void recordingEvent(int index, Bundle detail) {}
	
	
	/**
	 * CallbackHandler
	 * @author Blake
	 *
	 * handles all events for the SensorLoopers
	 */
	private class CallbackHandler implements HardwareCallback, RecorderCallback {
		private Vector<SensorLooper> mSensorLoopers;

		public CallbackHandler(Vector<SensorLooper> sensorLoopers) {
			mSensorLoopers = sensorLoopers;
		}

		public void hardwareEnabled(int index) {
			debug("sensorLooper("+index+") hardware enabled");

			// attempt to start recording if the sensor location provider is recording
			if(sensorLocationProviderServiceStatus == STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_ENABLED) {
				
				debug("LocationProvider is running. SensorLooper will start recording");
				mSensorLoopers.get(index).startRecording(this, index, sensorLocationProviderTimeStarted);
			}
		}

		public void hardwareFailedToEnable(int index, int reason) {
			debug("sensorLooper("+index+") hardware failed to enable");

			switch(reason) {
			case HardwareCallback.REASON_WIFI_UNABLE:
				// your device is unable to use WiFi!
				break;

			case HardwareCallback.REASON_WIFI_USER:
				// notify: something is preventing wifi from starting. you must allow this app to enable wifi
				break;
			}

		}

		public void hardwareDisabled(int index, int reason) {
			debug("sensorLooper("+index+") hardware was disabled");

			switch(reason) {
			case HardwareCallback.REASON_WIFI_USER:
				mSensorLoopers.get(index).stopRecording(this, index);
				// notify: you disabled WiFi! Any sensors depending on it have been shutdown
//				NotificationInterface.post(mContext, ActivityControl.class, "wifi-disabled", false, "Wi-Fi was disabled", "Tracer has shut down", 0);
				break;
			}
		}

		public void recordingStarted(int index, long timestamp) {

			debug("*sensor* recordingStarted("+timestamp+") <> "+(System.currentTimeMillis()-timestamp)+"ms");

			debug("sensorLooper("+index+") started recording");
		}

		public void recordingFailedToStart(int index, int reason) {
			debug("sensorLooper("+index+") failed to start recording because: "+recordingReasonToString(reason));
		}
		
		public void recordingPaused(int index) {
			debug("sensorLooper("+index+") paused recording");
		}

		public void recordingStopped(int index, int reason) {
			switch(reason) {
			case RecorderCallback.REASON_STOPPED:
				// okay, it was volunteered to stop
				break;
			}
		}

		public void recordingResumed(int index) {
			// TODO Auto-generated method stub
			
		}
		
		public void recordingEvent(int index, Bundle detail) {
			Class<?> sensorLooper = sensorLooperMap[index];
			mSensorLocationProvider.recordEvent(sensorLooper, detail);
			broadcastIntentToActivities(sensorLooper.getName(), detail);
		}
		
		private String recordingReasonToString(int reason) {
			switch(reason) {
			case RecorderCallback.REASON_RUNNING:
				return "sensor looper is running";
				
			case RecorderCallback.REASON_STOPPED:
				return "sensor looper is stopped";
				
			case RecorderCallback.REASON_UNIMPLEMENTED:
				return "that method is unimplemented";
			}
			return "?reason unknown?";
		}

	}
	


	/**
	 * debug text to output(s)
	 * @param text
	 */
	private void debug(String text) {
		Log.d(TAG, text);
		broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.DEBUG_PRINT, text, false);
	}


	/**
	 * error to output(s)
	 * @param text
	 */
	private void error(String text) {
		Log.e(TAG, text);
		broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.DEBUG_PRINT, "error: "+text, false);
	}
	
	/**
	 * 
	 */
	private void broadcastServiceStatusUpdate() {
		switch(sensorLocationProviderServiceStatus) {
		
		case STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_DISABLED:
			broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_DISABLED);
			break;
			
		case STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_STARTING:
			broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_STARTING);
			break;
			
		case STATUS_SENSOR_LOCATION_PROVIDER_SERVICE_ENABLED:
			broadcastIntentToActivities(MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SERVICE_ENABLED);
			break;
		}
	}
	
	/**
	 * 
	 */
	private void broadcastRecordingStatusUpdate() {
		switch(sensorLocationProviderRecordingStatus) {
		
		case STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_PAUSED:
			broadcastIntentToActivities(BROADCAST_INTENT_ANNOUNCEMENT_TYPE.RECORDING_PAUSED);
			break;

		case STATUS_SENSOR_LOCATION_PROVIDER_RECORDING_RESUMED:
			broadcastIntentToActivities(BROADCAST_INTENT_ANNOUNCEMENT_TYPE.RECORDING_RESUMED);
			break;
			
		default:
			broadcastIntentToActivities(BROADCAST_INTENT_ANNOUNCEMENT_TYPE.RECORDING_STOPPED);
			break;
		}
	}
	
	/**
	 * broadcast a bundle to activities from sensor looper
	 * @param detail
	 */
	private void broadcastIntentToActivities(String classUri, Bundle detail) {
		Intent broadcastIntent = new Intent(MainService.BROADCAST_URI);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.INT, MainService.BROADCAST_INTENT_ANNOUNCEMENT_TYPE.SENSOR_LOOPER_EVENT);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.CLASS_URI, classUri);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.BUNDLE, detail);
		broadcastIntentToActivities(broadcastIntent);
	}

	/**
	 * broadcast an intent to any activities listening for broadcasts
	 * @param broadcastAction
	 */
	private void broadcastIntentToActivities(int broadcastCode) {
		lastBroadcast = broadcastCode;
		Intent broadcastIntent = new Intent(MainService.BROADCAST_URI);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.INT, broadcastCode);
		broadcastIntentToActivities(broadcastIntent);
	}

	/**
	 * broadcast an intent to any activities listening for broadcasts
	 * @param broadcastAction
	 * @param broadcastText
	 */
	private void broadcastIntentToActivities(int broadcastCode, String broadcastText, boolean setAsLastBroadcast) {
		if(setAsLastBroadcast) {
			lastBroadcast = broadcastCode;
		}
		Intent broadcastIntent = new Intent(MainService.BROADCAST_URI);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.INT, broadcastCode);
		broadcastIntent.putExtra(MainService.INTENT_NAME_MESSAGE.STRING, broadcastText);
		broadcastIntentToActivities(broadcastIntent);
	}

	/**
	 * broadcast an intent to any activities listening for broadcasts
	 * @param broadcastIntent
	 */
	private void broadcastIntentToActivities(Intent broadcastIntent) {
		mContext.sendBroadcast(broadcastIntent);
	}



}
