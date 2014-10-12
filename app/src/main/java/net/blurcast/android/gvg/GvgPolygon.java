package net.blurcast.android.gvg;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.Log;


public class GvgPolygon extends GvgPath {

	public GvgPolygon(String setPoints, GvgPaintStyle setStyle) {
		super(setPoints, setStyle);
	}

	@Override
	public void postPathSetup(RectF bounds, int width, int height, int count) {
		path.close();
		
		Log.d(TAG, "polygon: ["+width+", "+height+"]; #"+count);
	}
	
	@Override
	public void draw(Canvas canvas) {
		if(style.hasFill) {
			canvas.drawPath(path, style.getFillPaint());
		}
	}
}
