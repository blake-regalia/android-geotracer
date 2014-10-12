package net.blurcast.tracer.location_provider;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.blurcast.tracer.location_provider.gps.LocationProviderFragment_GPS;
import net.blurcast.tracer.location_provider.gps.SensorLocationProvider_GPS;
import net.blurcast.tracer.location_provider.map.LocationProviderFragment_Map;
import net.blurcast.tracer.location_provider.map.MapProviderActivity;
import net.blurcast.tracer.location_provider.map.SensorLocationProvider_Map;
import net.blurcast.tracer.sensor.wap.SensorLooper_WAP;

public class DefineLocationProviders {

	private static final String TAG = "DefineLocationProviders";

	/* static hashmaps to store the association of uris to definitions (and names for quicker access) */

	public static LinkedHashMap<String, LocationProviderDefinition> locationProviders_uriToDefinition = new LinkedHashMap<String, LocationProviderDefinition>();
	public static LinkedHashMap<String, String>                     locationProviders_uriToName       = new LinkedHashMap<String, String>();
	
	
	/* Define custom location providers here */
	
	public static final LocationProviderDefinition GPS = new LocationProviderDefinition(
			"GPS",
			"GPS Provider",
			SensorLocationProvider_GPS.class,
			LocationProviderFragment_GPS.class,
			null,
			(new SensorLooperRequirements())
				.add(SensorLooper_WAP.class)
			);


	public static final LocationProviderDefinition MAP = new LocationProviderDefinition(
			"Custom Map",
			"Use a Map",
			SensorLocationProvider_Map.class,
			LocationProviderFragment_Map.class,
			MapProviderActivity.class,
			(new SensorLooperRequirements())
				.add(SensorLooper_WAP.class)
			);
	
	public static LocationProviderDefinition getLocationProviderDefinition(String uri) {
		return locationProviders_uriToDefinition.get(uri);
	}

	public static LocationProviderDefinition getLocationProviderDefinition(int index) {
		LocationProviderDefinition definition;
		Iterator<LocationProviderDefinition> it = locationProviders_uriToDefinition.values().iterator();
		int i = 0;
		while(it.hasNext()) {
			definition = it.next();
			if(i == index) {
				return definition;
			}
			i += 1;
		}
		return null;
	}

	public static Iterator<Entry<String, String>> getLocationProviderUrisAndNames() {
		return locationProviders_uriToName.entrySet().iterator();
	}


}
