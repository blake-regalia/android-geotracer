package net.blurcast.android.gvg;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;

public class GvgPolyline extends GvgPath {

	public GvgPolyline(String setPoints, GvgPaintStyle setStyle) {
		super(setPoints, setStyle);
	}

	@Override
	public void postPathSetup(RectF bounds, int width, int height, int count) {		
		Log.d(TAG, "polyline: ["+width+", "+height+"]; #"+count);
	}
	
	@Override
	public void draw(Canvas canvas) {
		if(style.hasStroke) {
			canvas.drawPath(path, style.getStrokePaint());
		}
	}
}
