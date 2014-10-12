package net.blurcast.android.gvg;

import java.util.ArrayList;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.Log;


public class GvgLayer {
	public final String type;
	public final ArrayList<GvgDrawable> drawables;
	public final ArrayList<ControlPoint> controlPoints;
	
	private boolean visibility = true;

	public GvgLayer(String setType, ArrayList<GvgDrawable> setDrawables, ArrayList<ControlPoint> setControlPoints) {
		type = setType;
		drawables = setDrawables;
		controlPoints = setControlPoints;
	}
	
	public boolean isVisible() {
		return visibility;
	}
	
	public void setVisibility(boolean visibile) {
		visibility = visibile;
	}
	
	public void draw(Canvas canvas) {
		for(GvgDrawable drawable: drawables) {
			drawable.draw(canvas);
		}
	}
	
	public void prepare(RectF bounds, int width, int height) {
		for(GvgDrawable drawable: drawables) {
			drawable.prepare(bounds, width, height);
		}
		for(ControlPoint controlPoint: controlPoints) {
			controlPoint.prepare(bounds, width, height);
		}
	}
	
	public RectF getBounds() {
		RectF bounds = new RectF();
		for(GvgDrawable drawable: drawables) {
			RectF temp = drawable.getBounds();
			if(bounds == null) bounds = temp;
			else bounds.union(temp);
		}
		return bounds;
	}
	
	public void setMatrixAndIdentityManager(Matrix matrix, IdentityManager identityManager) {
		for(ControlPoint controlPoint: controlPoints) {
			controlPoint.setMatrixAndIdentityManager(matrix, identityManager);
		}
	}
}