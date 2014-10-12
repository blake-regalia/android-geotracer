package net.blurcast.tracer.location_provider;

import net.blurcast.android.hardware.HardwareManager;
import net.blurcast.tracer.sensor.RecorderCallback;
import net.blurcast.tracer.sensor.SensorLooper;
import net.blurcast.tracer.sensor.SensorLooperCallback;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public abstract class SensorLocationProvider extends SensorLooper {
	
	protected HardwareManager mHardwareManager;
	protected LocationProviderDefinition mDefinition;
	protected SharedPreferences mSettings;

	public SensorLocationProvider(SensorLooperCallback callback, Context context, LocationProviderDefinition definition) {
		super(-1, callback, context);
		mHardwareManager = new HardwareManager(context);
		allowsPausing = false;
		mDefinition = definition;
		mSettings = definition.getSettings(context);
	}

	public void startRecording(RecorderCallback callback) {
		startRecording(callback, -1, System.currentTimeMillis());
	}
	
	public void stopRecording(RecorderCallback callback) {
		stopRecording(callback, -1);
	}

	public void setGpsToggleExploitMotivation(boolean motivation) {
		mHardwareManager.setGpsToggleExploitMotivation(motivation);
	}
	
	public void resumeRecording(float x, float y) {}

	public void recordEvent(Class<?> sensorLooperClass, Bundle detail) {}
	
}
