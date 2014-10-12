package net.blurcast.android.gvg;

import java.util.ArrayList;
import java.util.HashMap;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.RectF;
import android.graphics.drawable.PictureDrawable;
import android.util.Log;

public class GVG  {

	protected static final String TAG = "GVG";
	
	public HashMap<String, String> attrs;
	public ArrayList<GvgLayer> layers;
	
	private GvgLayer mSelectedLayer = null;
	
	public GVG(ArrayList<GvgLayer> setLayers) {
		layers = setLayers;
	}
	
	public void setLayer(int layerIndex, Matrix matrix, IdentityManager identityManager) {
		int index = 0;
		for(GvgLayer layer: layers) {
			if(index == layerIndex) {
				layer.setVisibility(true);
				mSelectedLayer = layer;
			}
			else {
				layer.setVisibility(false);
			}
		}
		if(mSelectedLayer != null) {
			mSelectedLayer.setMatrixAndIdentityManager(matrix, identityManager);
		}
	}
	
	public void draw(Canvas canvas) {
		for(GvgLayer layer: layers) {
			if(layer.isVisible()) {
				layer.draw(canvas);
			}
		}
	}

	public PictureDrawable createPictureDrawable(int width, int height) {
		return new PictureDrawable(getPicture(width, height));
	}
	
	public ArrayList<ControlPoint> getControlPoints() {
		Log.d("GET CONTROL POINTS","getting control points");
		return mSelectedLayer.controlPoints;
	}
	
	public Picture getPicture(int width, int height) {
		Picture picture = new Picture();
		Canvas canvas = picture.beginRecording(width, height);

		RectF bounds = null;
		for(GvgLayer layer: layers) {
			RectF temp = layer.getBounds();
			if(bounds == null) bounds = temp;
			else bounds.union(temp);
		}
		
		float boundsAR = bounds.width() / bounds.height();
		float dimensAR = width / ((float) height);

		// debug output
		Log.d(TAG, "mBounds: "+bounds);
		Log.d(TAG, "picture aspect ratio: "+bounds.width()+":"+bounds.height()+" => "+boundsAR);
		Log.d(TAG, "screen aspect ratio: "+width+":"+height+" => "+dimensAR);
		
		// adjust the dimensions to wrap the picture to scale
		if(dimensAR > boundsAR) {
			width = (int) (height * boundsAR);
		}
		else {
			height = (int) (width / boundsAR);
		}
				
		Log.d(TAG, "mBounds: ["+width+", "+height+"]");
		
		for(GvgLayer layer: layers) {
			Log.d(TAG, "found layer: "+layer.isVisible()+" visibility");
			if(layer.isVisible()) {
				layer.prepare(bounds, width, height);
				layer.draw(canvas);
			}
		}
		return picture;
	}

}
