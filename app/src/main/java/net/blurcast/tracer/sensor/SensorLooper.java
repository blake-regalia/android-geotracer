package net.blurcast.tracer.sensor;

import net.blurcast.android.hardware.HardwareCallback;
import net.blurcast.android.hardware.HardwareManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Blake
 *
 */
public abstract class SensorLooper {

	public static final String TAG = "SensorLooper";
	
	protected static final int STATUS_STOPPED = 0xAA00;
	protected static final int STATUS_RUNNING = 0xAA01;
	protected static final int STATUS_BLOCKED = 0xAA02;
	protected static final int STATUS_FAILED  = 0xAA03;

	protected static final int BLOCKING_OFF = 0xAB00;
	protected static final int BLOCKING_ON  = 0xAB01;

	public static final int REASON_NONE               = -1;
	public static final int REASON_UNKNOWN            = 0xAC00;
	public static final int REASON_DATA_FULL          = 0xAC01;
	public static final int REASON_IO_ERROR           = 0xAC02;
	public static final int REASON_BOUNDARY_CONDITION = 0xAC03;
	public static final int REASON_LOCATION_LOST      = 0xAC04;
	public static final int REASON_USER               = 0xAC05;
	public static final int REASON_BAD_PREFERENCE     = 0xAC06;

	private boolean looperPaused;
	private boolean pauseAcquired;
	private boolean resumeAcquired;
	protected boolean allowsPausing;
	
	protected int looperStatus;
	protected int looperBlocking;
	protected int sensorLooperIndex;
	protected int shutdownReason;

	protected long scanTimeStarted;
	protected long scanTimeStopped;
	protected long sensorLocationProviderTimeStarted;
	protected int recorderCallbackIndex;

	protected Context mContext;
	protected HardwareManager mHardwareManager;
	protected Thread mThread;
	protected SensorLooperCallback mSensorLooperCallback;
	protected RecorderCallback mRecorderCallback;

	public SensorLooper(int _sensorLooperIndex, SensorLooperCallback sensorLooperCallback, Context context) {
		looperPaused = false;
		pauseAcquired = false;
		resumeAcquired = true;
		allowsPausing = true;
		
		looperStatus = STATUS_STOPPED;
		looperBlocking = BLOCKING_OFF;
		sensorLooperIndex = _sensorLooperIndex;
		shutdownReason = REASON_NONE;
		recorderCallbackIndex = -1;

		mSensorLooperCallback = sensorLooperCallback;
		mContext = context;
		mThread = new Thread(looper);
	}


	/**
	 * a method to execute first, before entering the loop and making multiple calls to <i>sensorLoopMethod()</i> .
	 * @return		true if the service handler should be notified that recording started, false if the handler will be called later
	 */
	protected abstract boolean preLooperMethod();

	/**
	 * the method to execute each loop iteration
	 * @return		either <i>BLOCKING_ON</i> or <i>BLOCKING_OFF</i>
	 */
	protected abstract void sensorLoopMethod();

	/**
	 * method to call when owner of this object forces a shutdown
	 * @param reason
	 */
	protected abstract void terminateLooper(int reason);

	
	protected void openLooper() {
		mSensorLooperCallback.sensorLooperOpened(sensorLooperIndex);
	}

	protected void closeLooper() {
		mSensorLooperCallback.sensorLooperClosed(sensorLooperIndex);
	}


	private Runnable looper = new Runnable() {
		public synchronized void run() {
			Log.i(TAG, "*looper begins* ["+sensorLooperIndex+"]");
			
			openLooper();
			if(sensorLooperIndex == -1) {
				Log.d(TAG, "*location provider* => status_running: "+(looperStatus==STATUS_RUNNING)+"; blocking_off: "+(looperBlocking==BLOCKING_OFF)+"; paused: "+(looperPaused));
			}
			while(looperStatus == STATUS_RUNNING) {
				if(looperBlocking == BLOCKING_OFF) {
					if(looperPaused == false) {
						if(resumeAcquired == false) {
							resumeAcquired = true;
							mRecorderCallback.recordingResumed(recorderCallbackIndex);
						}
						sensorLoopMethod();
					}
					else if(pauseAcquired == false) {
						pauseAcquired = true;
						mRecorderCallback.recordingPaused(recorderCallbackIndex);
					}
				}
			}
			Log.i(TAG, "*looper breaks* ("+sensorLooperIndex+")");
			
			looperPaused = false;
			
			if(looperStatus == STATUS_BLOCKED) {
				looperStatus = STATUS_STOPPED;
			}
			if(looperStatus != STATUS_FAILED) {
				closeLooper();
			}
		}
	};

	protected int getScanTimeAverageOffset() {
		return (int) (((scanTimeStarted + scanTimeStopped) / 2) - sensorLocationProviderTimeStarted);
	}

	/**
	 * notify the owner of this instance that the looper initiated a shutdown
	 * @param reason
	 */
	protected void exitLoopNotifyOwner(int reason) {
		Log.w(TAG, "shutdown initiated");
		looperStatus = STATUS_FAILED;
		mSensorLooperCallback.sensorLooperFailed(sensorLooperIndex, reason);
	}

	public int getReason() {
		return shutdownReason;
	}

	public void attemptEnableHardware(HardwareCallback callback) {
		callback.hardwareFailedToEnable(-1, HardwareCallback.REASON_UNIMPLEMENTED);
	}

	public void attemptEnableHardware(HardwareCallback callback, int index) {
		callback.hardwareFailedToEnable(index, HardwareCallback.REASON_UNIMPLEMENTED);
	}

	public void startRecording(RecorderCallback callback, int index, long timeStarted) {
		if(looperStatus != STATUS_STOPPED) {
			callback.recordingFailedToStart(index, RecorderCallback.REASON_RUNNING);
		}
		else {
			looperStatus = STATUS_RUNNING;
			sensorLocationProviderTimeStarted = timeStarted;
			
			// setup callback to notify owner later
			mRecorderCallback = callback;
			recorderCallbackIndex = index;
			
			if(preLooperMethod()) {
				// notify owner recording started
				callback.recordingStarted(index, sensorLocationProviderTimeStarted);
			}
			mThread.start();
		}
	}
	
	protected void notifyOwnerRecordingStarted() {
		if(mRecorderCallback != null) {
			mRecorderCallback.recordingStarted(recorderCallbackIndex, sensorLocationProviderTimeStarted);
		}
		mRecorderCallback = null;
	}
	

	
	public boolean pauseRecording() {
		if(allowsPausing) {
			pauseAcquired = false;
			looperPaused = true;
			return true;
		}
		else {
			return false;
		}
	}

	
	public boolean resumeRecording() {
		if(looperPaused) {
			resumeAcquired = false;
			looperPaused = false;
			return true;
		}
		else {
			return false;
		}
	}
	
	protected boolean isPaused() {
		return looperPaused;
	}

	/**
	 * for owner of this object: request trace file be closed, halt looping & release hardware
	 * @param callback
	 * @param index
	 */
	public synchronized void stopRecording(RecorderCallback callback, int index) {
		// if the loop was already off, no need to do anything else
		if(looperStatus == STATUS_STOPPED) {
			callback.recordingStopped(index, RecorderCallback.REASON_STOPPED);
		}
		
		else if(looperStatus == STATUS_RUNNING) {
			// close open files, release system resources
			terminateLooper(REASON_USER);
			
			// prevent any more calls to loop method by breaking out of loop
			looperStatus = STATUS_BLOCKED;
		}
		
		mThread = new Thread(looper);
	}	
	
	protected void notifyOwnerRecordingEvent(Bundle detail) {
		if(mRecorderCallback != null) {
			mRecorderCallback.recordingEvent(recorderCallbackIndex, detail);
		}
	}

}
