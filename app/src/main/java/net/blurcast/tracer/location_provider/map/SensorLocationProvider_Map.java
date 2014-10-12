package net.blurcast.tracer.location_provider.map;

import net.blurcast.android.hardware.HardwareCallback;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderDefinition;
import net.blurcast.tracer.location_provider.SensorLocationProvider;
import net.blurcast.tracer.sensor.SensorLooperCallback;
import net.blurcast.tracer.sensor.wap.SensorLooper_WAP;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;

public class SensorLocationProvider_Map extends SensorLocationProvider {
	
	public static final String PREF_URI_MAP_FILE = "map file";
	public static final String PREF_SAMPLE_TYPE = "sample type";
	public static final String PREF_SAMPLE_COUNT = "sample count";

	public static class SAMPLE_TYPE_PREF {
		public static final String N_SAMPLES = "n samples";
		public static final String N_SECONDS = "n seconds";
		public static final String DEFAULT = "";
	}
	
	private static class SAMPLE_TYPE {
		public static final int N_SAMPLES = 0x0A00;
		public static final int N_SECONDS = 0x0A01;
		public static final int DEFAULT = N_SAMPLES;
	}
	
	private static class MODE {
		public static final int START_EVENT = 0x000E;
		public static final int END_EVENT   = 0x000F;
	}
	
	private TraceManager_Map mTraceManager;
    private SharedPreferences mSettings;
    
    private final int sampleType;
    private final int sampleCount;
    private int numSamples = 0;
	private int mode = MODE.START_EVENT;
	private float[] previousEvent;

	public SensorLocationProvider_Map(SensorLooperCallback callback, Context context, Looper looper, LocationProviderDefinition definition) {
		super(callback, context, definition);
		allowsPausing = true;
		previousEvent = new float[]{0.f, 0.f};

        mSettings = DefineLocationProviders.MAP.getSettings(context);

        String sampleTypePref = mSettings.getString(PREF_SAMPLE_TYPE, SAMPLE_TYPE_PREF.DEFAULT);
        if(sampleTypePref.equals(SAMPLE_TYPE_PREF.N_SAMPLES)) {
        	sampleType = SAMPLE_TYPE.N_SAMPLES;
        }
        else if(sampleTypePref.equals(SAMPLE_TYPE_PREF.N_SECONDS)) {
        	sampleType = SAMPLE_TYPE.N_SECONDS;
        }
        else {
        	sampleType = SAMPLE_TYPE.DEFAULT;
        }
        
        sampleCount = mSettings.getInt(PREF_SAMPLE_COUNT, 1);
	}
	
	@Override
	public void attemptEnableHardware(HardwareCallback callback) {
		callback.hardwareEnabled(-1);
	}
	
	@Override
	protected boolean preLooperMethod() {
		
		// before the sensor loopers start, post a pause on their loopers
		mSensorLooperCallback.pauseSensorLoopers();
		
		// open the trace manager
		mTraceManager = new TraceManager_Map(mContext, sensorLocationProviderTimeStarted);
		if(!mTraceManager.openFile()) {
			mRecorderCallback.recordingFailedToStart(-1, REASON_IO_ERROR);
		}
		super.pauseRecording();
		return true;
	}

	@Override
	protected void sensorLoopMethod() {
		if(sampleType == SAMPLE_TYPE.N_SAMPLES) {
			
			// when finished recording from sensor loopers...
			if(numSamples >= sampleCount) {
				numSamples = 0; // reset number of samples
				super.pauseRecording(); // pause recording again
			}

			// begin sampling
			else {
				looperBlocking = BLOCKING_ON;
			}
		}
	}

	@Override
	protected void terminateLooper(int reason) {
		mTraceManager.closeFile();
	}
	
	@Override
	public void resumeRecording(float x, float y) {
		previousEvent[0] = x; previousEvent[1] = y;
		mTraceManager.recordEvent(System.currentTimeMillis(), x, y);
		mSensorLooperCallback.resumeSensorLoopers();
		super.resumeRecording();
	}
	
	@Override
	public void recordEvent(Class<?> sensorLooperClass, Bundle detail) {
		
		// WAP recorder
		if(sensorLooperClass.equals(SensorLooper_WAP.class)) {
			int[] details = detail.getIntArray(SensorLooper_WAP.BUNDLE_EXTRA_NAME.LEVELS);
			numSamples += 1;
			
			// if this is monitoring for number of sample events
			if(sampleType == SAMPLE_TYPE.N_SAMPLES) {
				
				// once the prequisite has been satisfied 
				if(numSamples >= sampleCount) {
					
					// pause the sensor loopers
					mSensorLooperCallback.pauseSensorLoopers();
					
					/// record the previous event again
					mTraceManager.recordEvent(System.currentTimeMillis(), previousEvent[0], previousEvent[1]);
					
					// release this looper blocking
					looperBlocking = BLOCKING_OFF;
				}
			}
		}
	}

}
