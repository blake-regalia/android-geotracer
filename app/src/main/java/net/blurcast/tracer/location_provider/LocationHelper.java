package net.blurcast.tracer.location_provider;

import java.util.Date;
import java.util.List;

import net.blurcast.android.util.Timeout;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

public class LocationHelper {

	private static final String TAG = "LocationHelper::";

	public static final int PROVIDER_GPS  = (1 << 0);
	public static final int PROVIDER_WIFI = (1 << 1);

	// minimum 100m accuracy
	protected static final int BOUNDARY_CHECK_MIN_ACCURACY_M = 100;
	// good enough for boundary check: 60m accuracy
	protected static final int BOUNDARY_CHECK_GOOD_ACCURACY_M = 60;
	// maximum 90s old fix
	protected static final long BOUNDARY_CHECK_MAX_AGE_MS = 1000 * 90;
	// max number of location updates
	protected static final int BOUNDARY_CHECK_MAX_NUM_EVENTS = 7;
	// timeout duration: how long to wait for a better location (20s)
	protected static final long BOUNDARY_CHECK_TIMEOUT_MS = 1000 * 20;

	// at most: 60m accuracy
	protected static final int TRACE_LOCATION_WORST_ACCURACY_M = 60;
	// maximum 20s old fix
	protected static final long TRACE_LOCATION_MAX_AGE_MS = 1000 * 20;
	// good enough to use wifi location
	protected static final int TRACE_LOCATION_GOOD_ACCURACY_M = 6;
	// decent enough to use wifi location when gps chip is lagging
	protected static final long TRACE_LOCATION_DECENT_ACCURACY_M = 15;
	// only use wifi location if gps is 10s old
	protected static final long TRACE_LOCATION_GPS_OLD_MS = 1000 * 10;


	// for boundary checks
	protected static final int LOCATION_TOO_INACCURATE = 250; 

	public static final int LISTENER_TRACE_LOCATION = 0;
	public static final int LISTENER_CHECK_BOUNDARY = 1;

	private boolean use_gps = false;
	private boolean use_wifi = false;

	private Context context;
	private Looper mMainThread;

	private LocationManager mLocationManager;

	private BasicLocationListener mActiveListener = null;
	private Location mLocation = null;

	public LocationHelper(Context _context, Looper looper) {
		context = _context;
		mMainThread = looper;

		// Acquire a reference to the system Location Manager
		mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public boolean useProvider(int provider) {
		use_gps = ((PROVIDER_GPS & provider) != 0);
		use_wifi = ((PROVIDER_WIFI & provider) != 0);

		return false;
	}

	public Location getLocation() {
		return mLocation;
	}

	public void obtainLocation(int listenerType, LocationResultsCallback callback)  {
		
		Log.d(TAG, "obtainLocation()");

		// make sure there are no listeners requestion location updates
		if(mActiveListener != null) {
			Log.w(TAG, "requesting update forced unbinding listeners");
			unbind();
		}

		// check if location manager is null AFTER unbinding previous listeners
		if(mLocationManager == null) {
			mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}
		
		boolean requires_only_one_location = false;

		// reset all listener variables, including mLocation
		switch(listenerType) {

		case LISTENER_CHECK_BOUNDARY:
			requires_only_one_location = true;
			mActiveListener = new BoundaryCheckListener(callback);
			break;

		case LISTENER_TRACE_LOCATION:
			mActiveListener = new TraceLocationListener(callback);
			break;
		
		default:
			Log.e(TAG, "active listener given was not a valid type!");
			return;
		}
		
		if(requires_only_one_location) {
			// start by passing the method the last known locations
			List<String> matchingProviders = mLocationManager.getAllProviders();
			for (String provider: matchingProviders) {
				Location location = mLocationManager.getLastKnownLocation(provider);
				if (location != null) {
					location.setProvider(provider);					
					// trigger a location changed event with an older saved location
					mActiveListener.onLocationChanged(location, true);
					// if the method accepted one of the last known locations and that's all we need
					if(mActiveListener == null) {
						return;
					}
				}
			}
		}

		// begin listening for updates
		if(use_wifi) {
			Log.d(TAG, "binding listener w/ WiFi: "+listenerType+"; looper null? "+(mMainThread==null)+"; mActiveListener null? "+(mActiveListener==null));
			mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mActiveListener, mMainThread);
		}
		if(use_gps) {
			Log.d(TAG, "binding listener w/ PROVIDER_GPS: "+listenerType);
			mActiveListener.notifyStart();
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mActiveListener, mMainThread);
		}
	}
	
	/** 
	 * Stops all active listeners and timeouts and releases any resources
	 * that may be associated with them
	 * 
	 */
	public void unbind() {
		if(mActiveListener != null) {
			Log.d(TAG, "unbinding listener");
			mActiveListener.close();
			if(mLocationManager != null) {
				mLocationManager.removeUpdates(mActiveListener);
				mLocationManager = null;
			}
			mActiveListener = null;
		}
	}



	/** basic location listener class
	 * 
	 *  **/
	public class BasicLocationListener implements LocationListener {

		protected LocationResultsCallback mCallback; 
		
		protected boolean objective_complete = false;
		protected int active_location_events = 0;
		protected int timeout_failure = -1;

		// for accepting a single location fix 
		protected void locationFixed() {
			if(mLocation == null) {
				positionLost(LocationResultsCallback.REASON_LOCATION_NULL);
				return;
			}
			objective_complete = true;
			mCallback.locationReady(mLocation);
		}

		// for accepting 1 of many location fixes
		protected void locationFixed(Location location) {
			mLocation = location;
			mCallback.locationReady(mLocation);
		}
		
		protected void positionLost(int reason) {
			mCallback.positionLost(reason);
		}
		
		protected synchronized void startTimeout(Runnable action, long delay) {
			Timeout.clearTimeout(timeout_failure);
			timeout_failure = Timeout.setTimeout(action, delay);
		}
		
		public void cancelTimeout() {
			timeout_failure = Timeout.clearTimeout(timeout_failure);
		}
		
		protected boolean completedObjective() {
			return objective_complete;
		}
		
		public BasicLocationListener(LocationResultsCallback callback) {
			mCallback = callback;
			mLocation = null;
		}

		public void close() {
			this.cancelTimeout();
		}
		
		public void notifyStart(){}
		
		public synchronized void onLocationChanged(Location location) {
			this.onLocationChanged(location, false);
		}

		public synchronized void onLocationChanged(Location location, boolean isOldLocation) {
			
			active_location_events += isOldLocation? 1: 0;
			
			if(mActiveListener == null) {
				Log.e(TAG, "basic listener:: location changed it when should have been unregistered");
				mLocationManager.removeUpdates(this);
				return;
			}

			if(!location.hasAccuracy()) {
				Log.e(TAG, "### location has no accuracy");
				// no accuracy!?
				return;
			}
			
			this.handleLocationUpdate(location, isOldLocation);
		}
		
		public synchronized void handleLocationUpdate(Location location, boolean isOldLocation) {}
		public synchronized void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String arg0) {}
		
		public synchronized void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			String str = "unknown";
			switch(arg1) {
			case LocationProvider.OUT_OF_SERVICE:
				str = arg0+" out of service";
				break;
			case LocationProvider.AVAILABLE:
				str = arg0+" available";
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				str = arg0+" temporarily unavailable";
				break;
			}

			Log.d(TAG, "status change event: "+str);
		}
	}



	public class BoundaryCheckListener extends BasicLocationListener {

		public BoundaryCheckListener(LocationResultsCallback callback) {
			super(callback);
		}
		protected void fixed() {
			unbind();
			locationFixed();
		}
		protected void lost(int reason) {
			unbind();
			positionLost(reason);
		}
		public void notifyStart() {
			// start a timeout thread to return the best location after a period of inactivity
			startTimeout(boundary_check_timeout, BOUNDARY_CHECK_TIMEOUT_MS);
		}
		public synchronized void handleLocationUpdate(Location location, boolean isOldLocation) {
			
			long now = System.currentTimeMillis();
			
			if(!isOldLocation) location.setTime(now);

			Log.d(TAG, "boundary check: location change event: "+active_location_events+" => "+location.getProvider());

			if(location.getAccuracy() > LOCATION_TOO_INACCURATE) {
				return;
			}

			long expiration = (now - BOUNDARY_CHECK_MAX_AGE_MS);

			// if this location is accurate enough
			if(location.getAccuracy() <= BOUNDARY_CHECK_MIN_ACCURACY_M
					// and new enough
					&& (location.getTime() > expiration)) {

				Log.d(TAG, "boundary check:: location is accurate & new enough to use");

				// save it if we don't have one saved
				if(mLocation == null
						// or if the one we have is too old
						|| (mLocation.getTime() > expiration)
						// or if this location is more accurate
						|| (mLocation.getAccuracy() <= location.getAccuracy()) ) {

					Log.d(TAG, "location was saved");
					mLocation = location;
				}
			}

			// as soon as there is a location that has good enough accurcy
			if(mLocation != null && mLocation.getAccuracy() <= BOUNDARY_CHECK_GOOD_ACCURACY_M) {

				Log.d(TAG, "boundary check:: location is good enough. using it for boundary check");

				fixed();
				return;
			}
			// if location listener has been called several times..
			else if(active_location_events >= BOUNDARY_CHECK_MAX_NUM_EVENTS) {

				Log.d(TAG, "boundary cehck:: location changed enough times, using best location");

				fixed();
				return;
			}

			// start a timeout thread to return the best location after a period of inactivity
			startTimeout(boundary_check_timeout, BOUNDARY_CHECK_TIMEOUT_MS);
		}

		private Runnable boundary_check_timeout = new Runnable() {
			public void run() {
				if(mLocation != null) {
					Log.d(TAG, "boundary check:: update timed out; using best estimate");
					fixed();
				}
				else {
					Log.d(TAG, "No good locations...");
					lost(LocationResultsCallback.REASON_POOR_RECEPTION);
				}
			}
		};
	}



	public class TraceLocationListener extends BasicLocationListener {
		
		public TraceLocationListener(LocationResultsCallback callback) {
			super(callback);
		}
		
		public synchronized void handleLocationUpdate(Location location, boolean isOldFix) {
			
			long now = System.currentTimeMillis();
			
			if(!isOldFix) location.setTime(now);

			double accuracy = location.getAccuracy();
			String provider = location.getProvider();

			// PROVIDER_GPS location
			if(provider.equals(LocationManager.GPS_PROVIDER)) {

				// report position lost when a poor accuracy breaks the desirable threshold
				if(accuracy > TRACE_LOCATION_WORST_ACCURACY_M  &&  isOldFix == false) {
					Log.d(TAG, "### location accuracy too poor: "+location.getAccuracy());
					positionLost(LocationResultsCallback.REASON_POOR_ACCURACY);
					return;
				}
				else {
					// store this location
					locationFixed(location);
				}

			}

			// Network-based location
			else if(provider.equals(LocationManager.NETWORK_PROVIDER)) {

				// if this location is VERY good (from wifi), then use it
				if(accuracy <= TRACE_LOCATION_GOOD_ACCURACY_M) {
					this.locationFixed(location);
				}

				// if we don't have a location yet, accept anything
				else if(mLocation == null) {
					this.locationFixed(location);
				}

				// if this position is accurate enough, and the PROVIDER_GPS location is too old, then reluctantly accept it
				else if(mLocation.getProvider().equals(LocationManager.GPS_PROVIDER)) {
					long gps_old = ((new Date()).getTime() - TRACE_LOCATION_GPS_OLD_MS);
					if((accuracy <= TRACE_LOCATION_DECENT_ACCURACY_M) && (mLocation.getTime() > gps_old)) {
						this.locationFixed(location);
					}
				}
			}

			// start a timeout thread to notify the service of position lost after period of inactivity
			startTimeout(trace_location_timeout, TRACE_LOCATION_MAX_AGE_MS);
		}


		private Runnable trace_location_timeout = new Runnable() {
			public void run() {
				timeout_failure = -1;
				positionLost(LocationResultsCallback.REASON_TIMED_OUT);
			}
		};
		
	}


}
