package net.blurcast.tracer.location_provider.map;

import net.blurcast.android.util.ByteBuilder;
import net.blurcast.android.util.Encoder;
import net.blurcast.tracer.sensor.SensorLooper;
import net.blurcast.tracer.sensor.TraceManager;
import android.content.Context;
import android.util.Log;

public class TraceManager_Map extends TraceManager {
	
	private static final String TAG = "TraceManager(Map)";
	private static final String traceType = "location";
	private static final String fileExt = ".bin";

	public TraceManager_Map(Context context, long timeStart) {
		super(context, timeStart);
	}

	@Override
	public boolean openFile() {
		String projectionType = "UTM";
		byte[] projection = projectionType.getBytes();

		ByteBuilder byteBuilder = new ByteBuilder(1+projection.length);
		byteBuilder.append((byte) projection.length);
		byteBuilder.append(projection);
		
		/* create file & write file header */
		String traceFileName = getDateString()+"."
				+traceType
				+fileExt;
		if(openTraceFile(traceFileName, ID_TYPE_TRACE_MAP, sensorLocationProviderTimeStart)) {
			return writeToTraceFile(byteBuilder.getBytes());
		}
		return false;
	}

	@Override
	public boolean closeFile() {
		Log.d(TAG, "closing trace file");

		return
				// close file
				closeTraceFile();
	}
	
	public int recordEvent(long eventTimeMs, float x, float y) {
		ByteBuilder byteBuilder = new ByteBuilder(4 + 4 + 4);
		
		// positive time offset (centi-seconds): 4 bytes
		byteBuilder.append_4(
				Encoder.encode_int(
						(int) ((eventTimeMs-sensorLocationProviderTimeStart) * CONVERT_MILLISECONDS_CENTISECONDS)
						)
				);

		// encode x-component
		byteBuilder.append_4(
				Encoder.encode_float(x)
				);

		// encode y-component
		byteBuilder.append_4(
				Encoder.encode_float(y)
				);

		// for some reason, this looper is shutting down
		if(shutdownReason != REASON_NONE) {
			switch(shutdownReason) {
			case REASON_IO_ERROR:
				return SensorLooper.REASON_IO_ERROR;

			case REASON_TIME_EXCEEDING:
				return SensorLooper.REASON_DATA_FULL;

			default:
				return SensorLooper.REASON_UNKNOWN;
			}
		}

		// attempt to write the current loop data to the trace file
		if(writeToTraceFile(byteBuilder.getBytes())) {
			Log.w(TAG, byteBuilder.length()+" bytes written to file");
			
			return SensorLooper.REASON_NONE;
		}
		else {
			return SensorLooper.REASON_UNKNOWN;
		}
	}

}
