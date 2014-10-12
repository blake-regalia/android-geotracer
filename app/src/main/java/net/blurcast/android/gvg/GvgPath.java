package net.blurcast.android.gvg;

import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;

public abstract class GvgPath extends GvgDrawable {

	public final float[][] points;

	protected Path path = new Path();
	protected GvgPaintStyle style;
	
	public GvgPath(String setPoints, GvgPaintStyle setStyle) {
		style = setStyle;
		String[] crds = setPoints.split("[\t\r\n ]+");
		points = new float[crds.length][2];
		for(int i=0; i<crds.length; i++) {
			float[] xy = parseCoordinate(crds[i]);    		
			if(xy.length < 2) continue;
	
			float x = xy[0]; float y = xy[1];
			if(mBounds == null) {
				mBounds = new RectF(x, y, x, y);
			}
			else {
	    		if(x < mBounds.left) mBounds.left = x;
	    		if(x > mBounds.right) mBounds.right = x;
	    		if(y < mBounds.top) mBounds.top = y;
	    		if(y > mBounds.bottom) mBounds.bottom = y;
			}
			points[i] = xy;
		}
	}

	@Override
	public void prepare(RectF bounds, int width, int height) {
		float xFactor = width / bounds.width();
		float yFactor = height / bounds.height();
		
		int count = 0;
		path = new Path();
		for(int i=0; i<points.length; i++) {
			float x = (points[i][0] - bounds.left) * xFactor;
			float y = (points[i][1] - bounds.top) * yFactor;			
			if(i == 0) {
				path.moveTo(x, y);
			}
			else {
				path.lineTo(x, y);
			}
			count ++;
		}
		
		postPathSetup(bounds, width, height, count);
	}
	
	public void postPathSetup(RectF bounds, int width, int height, int count){}
	
	@Override
	public abstract void draw(Canvas canvas);
}
