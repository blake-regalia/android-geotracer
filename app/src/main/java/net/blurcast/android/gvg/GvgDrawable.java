package net.blurcast.android.gvg;

import android.graphics.Canvas;
import android.graphics.RectF;

public abstract class GvgDrawable {
	protected static final String TAG = "Drawable";
	
	protected RectF mBounds = null;
	
	public void draw(Canvas canvas) {}
	public void prepare(RectF bounds, int width, int height){}
	
	public RectF getBounds() {
		return mBounds;
	}
	
	protected float[] parseCoordinate(String coordinates) {
		String[] pair = coordinates.split("[, ]+");
		if(pair.length < 2) {
			return new float[]{};
		}
		float x = Float.valueOf(pair[0]);
		float y = -Float.valueOf(pair[1]);
		return new float[]{x, y};
	}
}

