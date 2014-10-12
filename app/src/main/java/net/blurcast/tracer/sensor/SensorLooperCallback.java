package net.blurcast.tracer.sensor;

public interface SensorLooperCallback {

	public void sensorLooperOpened(int index);
	public void sensorLooperClosed(int index);
	public void sensorLooperFailed(int index, int reason);
	public void pauseSensorLoopers();
	public void resumeSensorLoopers();
	
}
