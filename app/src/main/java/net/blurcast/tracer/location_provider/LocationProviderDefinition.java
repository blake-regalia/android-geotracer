package net.blurcast.tracer.location_provider;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;


public class LocationProviderDefinition {
	
	private static final String URI = LocationProviderDefinition.class.getName();
	
	private String mUri;
	private String mName;
	private Class mProvider;
	private Class mPreferenceActivity;
	private Class mRunningActivity;
	private ArrayList<Class<?>> mSensorLoopers = null;

	public LocationProviderDefinition(String uri, String name, Class<?> provider, Class<?> settings, Class<?> activity, SensorLooperRequirements sensorLooperClasses) {
		mUri = uri;
		mName = name;
		mProvider = provider;
		mPreferenceActivity = settings;
		mRunningActivity = activity;
		if(sensorLooperClasses != null) {
			mSensorLoopers = sensorLooperClasses.getSensorLooperClasses();
		}

		DefineLocationProviders.locationProviders_uriToDefinition.put(uri, this);
		DefineLocationProviders.locationProviders_uriToName.put(uri, name);
	}

	public String getUri() {
		return mUri;
	}

	public String getName() {
		return mName;
	}

	public Class<?> getProvider() {
		return mProvider;
	}

	public Class<?> getPreferencesFragment() {
		return mPreferenceActivity;
	}
	
	public Class<?> getRunningActivity() {
		return mRunningActivity;
	}
	
	public SharedPreferences getSettings(Context context) {
		return context.getSharedPreferences(URI+"."+mUri, Context.MODE_PRIVATE);
	}
	
	public ArrayList<Class<?>> getSensorLoopers() {
		return mSensorLoopers;
	}
}
