package net.blurcast.tracer.sensor.wap;

import java.util.List;

import net.blurcast.android.hardware.HardwareCallback;
import net.blurcast.android.hardware.HardwareManager;
import net.blurcast.android.util.Timeout;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.sensor.SensorLooper;
import net.blurcast.tracer.sensor.SensorLooperCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

public class SensorLooper_WAP extends SensorLooper {

	private static final String TAG = "SensorLooper(WAP)";
	private static final String URI = SensorLooper_WAP.class.getName();
	private static final String BROADCAST_URI = URI+"#broadcast";

	public static class BUNDLE_EXTRA_NAME {
		public static final String LEVELS = "levels";
		public static final String SAMPLES = "samples";
	}

	private int timeoutNextScan = -1;
	private long intervalMinScanWifiMs = 0; // default finest resolution

	private WifiManager mWifiManager;
	private TraceManager_WAP mTraceManager;
	private HardwareManager mHardwareManager;
	private BroadcastReceiver mBroadcastReceiver;
	private SharedPreferences mSettings;


	/**
	 * Registers a broadcast receiver to be triggered when a scan completes
	 * @param receiver
	 */
	private void listenForBroadcasts(BroadcastReceiver receiver) {
		IntentFilter intent = new IntentFilter();
		intent.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mBroadcastReceiver = receiver;
		mContext.registerReceiver(receiver, intent);
	}

	/**
	 * initialize the trace manager and create a trace file before writing to it
	 * @return		true if this trace has started recording after this method executes
	 */
	@Override
	protected boolean preLooperMethod() {

		Log.d(TAG, "*sensor* pre-looper(); time started:"+sensorLocationProviderTimeStarted+" <> "+(System.currentTimeMillis()-sensorLocationProviderTimeStarted));
		
		mTraceManager = new TraceManager_WAP(mContext, sensorLocationProviderTimeStarted);
		if(!mTraceManager.openFile()) {
			Log.e(TAG, "Failed to create new trace file.");
			exitLoopNotifyOwner(REASON_IO_ERROR);
		}

		// register for wifi service
		listenForBroadcasts(new SaveResults());

		return true;
	}

	/**
	 * bind a receiver for scan results, save start time & begin scan
	 */
	@Override
	protected void sensorLoopMethod() {

		looperBlocking = BLOCKING_ON;
		scanTimeStarted = System.currentTimeMillis();
		mWifiManager.startScan();
	}

	/**
	 * the owner of this object requested the looper terminate,
	 * release resources and close files
	 */
	@Override
	protected synchronized void terminateLooper(int reason) {
		// do not start a new scan if there is a timeout for it
		if(timeoutNextScan != -1) {
			Timeout.clearTimeout(timeoutNextScan);
		}

		Log.e(TAG, "terminateLooper(): "+reason);

		// assure safe shutdown
		closeResources();
	}

	/**
	 * close files & release hardware
	 */
	private void closeResources() {
		Log.d(TAG, "closeResources()");

		// unregister wifi scan receiever
		unregisterReceiver();

		// close the trace file
		if(!mTraceManager.closeFile()) {
			exitLoopNotifyOwner(REASON_IO_ERROR);
		}
	}

	/**
	 * release hardware 
	 */
	private void unregisterReceiver() {
		if(mBroadcastReceiver != null) {
			mContext.unregisterReceiver(mBroadcastReceiver);
		}
		mBroadcastReceiver = null;
	}

	private String shutdownReasonAsString() {
		switch(shutdownReason) {
		case REASON_BOUNDARY_CONDITION:
			return "Boundary condition";
		case REASON_DATA_FULL:
			return "Data full";
		case REASON_IO_ERROR:
			return "IO Error";
		case REASON_LOCATION_LOST:
			return "Location lost";
		case REASON_NONE:
			return "NO REASON";
		case REASON_UNKNOWN:
			return "Unknown reason";
		case REASON_USER:
			return "User initiated shutdown";
		default:
			return "WTF?! OTHER!";
		}
	}

	private class SaveResults extends BroadcastReceiver {
		@Override
		public void onReceive(Context c, Intent i) {
			if(looperStatus != STATUS_RUNNING || isPaused()) return;

			scanTimeStopped = System.currentTimeMillis();
			List<ScanResult> scanResults = mWifiManager.getScanResults();
			int reason = mTraceManager.recordEvent(scanResults, getScanTimeAverageOffset());

			broadcastWapEvent(scanResults);

			// recordEvent() is causing a shutdown
			if(reason != REASON_NONE) {
				shutdownReason = reason;
				Log.e(TAG, "trace manager returned error: "+shutdownReasonAsString());
			}

			// something initiated a shutdown
			if(shutdownReason != REASON_NONE) {
				Log.w(TAG, "shutdown initiated: "+shutdownReasonAsString());

				// assure safe shutdown
				closeResources();

				// exit loop and notify owner
				exitLoopNotifyOwner(reason);

				// do not continue
				return;
			}

			// wait for the designated interval time to pass before scanning again 
			long lastScanTime = scanTimeStopped - scanTimeStarted;
			long waitTime = (intervalMinScanWifiMs - lastScanTime);
			if(waitTime < 10) {
				looperBlocking = BLOCKING_OFF;
			}
			else {
				Log.d(TAG, "waiting "+waitTime+"ms before next scan");
				timeoutNextScan = Timeout.setTimeout(new Runnable() {
					public synchronized void run() {
						timeoutNextScan = -1;
						looperBlocking = BLOCKING_OFF;	
					}
				}, waitTime);
			}
		}
	}

	public SensorLooper_WAP(int _sensorLooperIndex, SensorLooperCallback sensorLooperCallback, Context context)  {
		super(_sensorLooperIndex, sensorLooperCallback, context);
		mContext = context;
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mHardwareManager = new HardwareManager(context);
	}

	@Override
	public void attemptEnableHardware(HardwareCallback callback, int index) {
		mHardwareManager.enable(HardwareManager.RESOURCE_WIFI, callback, index);
	}

	private void broadcastWapEvent(List<ScanResult> scanResults) {
		Bundle detail = new Bundle();

		int scanResultsSize = scanResults.size();

		int[] levels = new int[scanResultsSize];
		for(int i=0; i<scanResultsSize; i++) {
			ScanResult result = scanResults.get(i);
			levels[i] = result.level;
		}

		detail.putIntArray(BUNDLE_EXTRA_NAME.LEVELS, levels);
		notifyOwnerRecordingEvent(detail);
	}


}
