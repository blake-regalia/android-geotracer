package net.blurcast.tracer.service;

import android.app.Service;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.util.Log;

public class MainService extends Service {
	
	private static final String TAG = "MainService";
	
	public static final String URI = "net.blurcast.service"; //MainServiceHandler.class.getName();
	public static final String BROADCAST_URI = URI+"#broadcast";

	private static boolean serviceIsRunning = false;

	/** commands for the activities to use for purpose of starting the service **/
	public static class COMMAND {
		public static final String REQUEST_SERVICE_STATUS   = "request service status";
		public static final String REQUEST_RECORDING_STATUS = "request recording status";
		public static final String DISABLE_SERVICE          = "disable service";
		public static final String ENABLE_SERVICE           = "enable service";
		public static final String START_RECORDING          = "start recording";
		public static final String RESUME_RECORDING          = "resume recording";
		public static final String STOP_RECORDING           = "stop recording";
	}
	
	/** intent actions that the service handler will broadcast to activities **/
	public static class BROADCAST_INTENT_ANNOUNCEMENT_TYPE {
		public static final int NONE                  = 0x0500;
		public static final int DEBUG_PRINT           = 0x0501;
		public static final int ERROR_PROVIDER_EXISTS = 0x0502;
		public static final int SERVICE_DISABLED      = 0x0503;
		public static final int SERVICE_STARTING      = 0x0504;
		public static final int SERVICE_ENABLED       = 0x0505;
		public static final int RECORDING_STARTED     = 0x0506;
		public static final int RECORDING_STOPPED     = 0x0507;
		public static final int RECORDING_PAUSED      = 0x0508;
		public static final int RECORDING_RESUMED     = 0x0509;
		public static final int SENSOR_LOOPER_EVENT   = 0x050A;
	}
	
	/** names of the intent extras **/
	public static class INTENT_NAME_MESSAGE {
		public static final String INT		 = URI+"#intent.int";
		public static final String STRING    = URI+"#intent.string";
		public static final String COMMAND   = URI+"#intent.command";
		public static final String BUNDLE    = URI+"#intent.bundle";
		public static final String CLASS_URI = URI+"#intent.class_uri";
	}
	
	/** names of the bundle extras **/
	public static class BUNDLE_MESSAGE {
		public static final String FLOAT_ARRAY = URI+"#bundle.float_array";
	}
	
	/** messages that this service sends to the service handler **/
	public static class MESSAGE {
		public static final int NONE                     = 0x0600;
		public static final int REQUEST_SERVICE_STATUS   = 0x0601;
		public static final int REQUEST_RECORDING_STATUS = 0x0602;
		public static final int DISABLE_SERVICE          = 0x0604;
		public static final int ENABLE_SERVICE           = 0x0605;
		public static final int START_RECORDING          = 0x0606;
		public static final int STOP_RECORDING           = 0x0607;
		public static final int RESUME_RECORDING         = 0x0608;
	}

	private Looper mServiceLooper;
	private MainServiceHandler mServiceHandler;

	// a target for clients to send messsages to the service handler
	private Messenger mServiceMessenger;
	
	// service is sticky if it wants to be started after OS frees up enough memory
	private final boolean serviceIsSticky = false;

	/** this gets called once the service is born **/
	@Override
	public void onCreate() {
		// Start up the thread running the service. we create a separate thread because the service normally runs in the process's
		// main thread, which we don't want to block (Activity's UI thread). we also make it background priority
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		Log.d(TAG, "**create**");

		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new MainServiceHandler(mServiceLooper, this);
	}

	
	/** this gets called any time an Activity explicity starts the service **/
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		// by default, assume nothing
		int what = MESSAGE.NONE;
		Object extra = null;
		
		// the intent given was null
		if(intent == null) {
			
			// if this service is sticky, then this means the OS freed enough memory to start this app again
			if(serviceIsSticky) {
				// TODO: respawn service
			}
			else {
				// ignore
			}
			
			return serviceIsSticky? START_STICKY: START_NOT_STICKY;
		}

		// get the objective with which this service was started: as passed in by the calling Activity
		String intentAction = intent.getAction();
		
		// log that this service was called to start
		Log.i(TAG, "startService("+intentAction+")");

		
		// if the service was started with an intent objective, translate the command into a message (string => int)
		if(intentAction != null) {
			
			// service is being commanded to start
			if(intentAction.equals(COMMAND.ENABLE_SERVICE)) {
				what = MESSAGE.ENABLE_SERVICE;
				extra = intent.getStringExtra(INTENT_NAME_MESSAGE.STRING);
			}
			
			// activity is commanding service to stop
			else if(intentAction.equals(COMMAND.DISABLE_SERVICE)) {
				what = MESSAGE.DISABLE_SERVICE;
			}

			// activity is commanding service to start recording
			else if(intentAction.equals(COMMAND.START_RECORDING)) {
				what = MESSAGE.START_RECORDING;
			}

			// activity is commanding service to start recording
			else if(intentAction.equals(COMMAND.RESUME_RECORDING)) {
				what = MESSAGE.RESUME_RECORDING;
				extra = intent.getBundleExtra(INTENT_NAME_MESSAGE.BUNDLE);
			}
			
			// service is being commanded to stop recording
			else if(intentAction.equals(COMMAND.STOP_RECORDING)) {
				what = MESSAGE.STOP_RECORDING;
			}

			// service is being requested to broadcast service status
			else if(intentAction.equals(COMMAND.REQUEST_SERVICE_STATUS)) {
				what = MESSAGE.REQUEST_SERVICE_STATUS;
			}
			
			// service is being requested to broadcast recording status
			else if(intentAction.equals(COMMAND.REQUEST_RECORDING_STATUS)) {
				what = MESSAGE.REQUEST_RECORDING_STATUS;
			}
			
		}
		
		// don't worry about unique start ids, this service should only be running one process at a time
		Message msg = mServiceHandler.obtainMessage(what, extra);
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return serviceIsSticky? START_STICKY: START_NOT_STICKY;
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		// We don't provide binding, so return null
		return null;
	}

	@Override
	public void onDestroy() {
	}
	
	public static boolean getServiceStatus() {
		return serviceIsRunning;
	}
}

