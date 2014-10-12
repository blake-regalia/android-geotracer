package net.blurcast.tracer.location_provider;

import java.util.ArrayList;

public class SensorLooperRequirements {
	
	private ArrayList<Class<?>> sensorLooperClasses;
	public SensorLooperRequirements() {
		sensorLooperClasses = new ArrayList<Class<?>>();
	}
	
	public SensorLooperRequirements add(Class<?> sensorLooperClass) {
		sensorLooperClasses.add(sensorLooperClass);
		return this;
	}
	
	public ArrayList<Class<?>> getSensorLooperClasses() {
		return sensorLooperClasses;
	}
}
