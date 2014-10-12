package net.blurcast.android.view;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import net.blurcast.android.gvg.ControlPoint;
import net.blurcast.android.gvg.GVG;
import net.blurcast.android.gvg.GvgParser;
import net.blurcast.android.gvg.IdentityManager;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.PictureDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class TouchMatrixViewGVG extends ImageView {

	private static final String TAG = "TouchMatrixViewGVG"; 
	private static final String POINT = "TouchMatrixViewGVG#Touch";
	
	private static final float THRESHOLD_CLICK_POINT = 28.0f;

	private PictureDrawable mPictureDrawable;

	private GVG mGvg;
	private Matrix mMatrix;
	private IdentityManager mIdentityManager;
	private ScaleListener mScaleListener;
	private ScaleGestureDetector mScaleGestureDetector;
	
	private ArrayList<ControlPoint> mControlPoints;
	private Callback mCallback = null;

	private int width = 0;
	private int height = 0;
	
	private int prevNumPointers = 0;
	private float prevX;
	private float prevY;
	private double prevSlope = 0.0;
	private boolean rotateActive = false;
	
	private float[] touchDown = new float[2];
	private float[] lastMove = new float[2];
	private boolean considerAsClick = false;
	private int controlPointCount = 0;
	
	private boolean dimensionsAreSet = false;
	private boolean pictureIsReadyToBeDrawn = false;

	public TouchMatrixViewGVG(Context context, AttributeSet attrs) {
		super(context, attrs);

		// establish that a matrix will be used to control the image view
		this.setScaleType(ImageView.ScaleType.MATRIX);

		// prepare an identity matrix
		mMatrix = new Matrix();

		// setup the scale detector & listener
		mScaleListener = new ScaleListener();
		mScaleGestureDetector = new ScaleGestureDetector(context, mScaleListener);
		
		
		mIdentityManager = new IdentityManager();
	}
	
	public void setControlPointCallback(Callback callback) {
		mCallback = callback;
	}
	
	@Override
	protected void onSizeChanged(int setWidth, int setHeight, int oldWidth, int oldHeight) {
		if(setWidth != 0 && setHeight != 0) {
			width = setWidth;
			height = setHeight;
			if(pictureIsReadyToBeDrawn) {
				drawPicture();
			}
			else {
				dimensionsAreSet = true;
			}
		}
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		this.setMeasuredDimension(width, height);
	}

	public boolean loadGVG(String filename) {
		InputStream input = null;
		try {
			input = new FileInputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		
		GvgParser parser = null;
		try {
			parser = new GvgParser(input);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// set the picture drawable for the canvas to draw
		mGvg = parser.getGvg();
		
		if(mGvg == null) {
			Log.e(TAG, "Parser failed to read GVG!");
		}
		
		if(dimensionsAreSet) {
			drawPicture();
		}
		pictureIsReadyToBeDrawn = true;
		
		return true;
	}

	private void drawPicture() {
		Log.d(TAG, "drawing picture: "+mGvg+" : "+mMatrix+" => "+mIdentityManager);
		mGvg.setLayer(0, mMatrix, mIdentityManager);
		mPictureDrawable = mGvg.createPictureDrawable(width, height);
		mControlPoints = mGvg.getControlPoints();
		this.setImageDrawable(mPictureDrawable);
		
		// notify the view that it needs to be redrawn
		invalidateViews();
	}
	
	private void invalidateViews() {
		this.invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) {
		this.setImageMatrix(mMatrix);
		super.onDraw(canvas);
		
		if(mControlPoints != null) {
			for(ControlPoint controlPoint : mControlPoints) {
				controlPoint.drawToCanvas(canvas);
			}
		}
	}
	

	// determines where to set the focus point of scaling when 1 or more fingers (pointers) are touching the view
	private float[] getFocus(MotionEvent evt) {
		final float[] foci = new float[2];
		switch(evt.getPointerCount()) {
		case 1:
			int pid1 = evt.findPointerIndex(evt.getPointerId(0));
			foci[0] = evt.getX(pid1);
			foci[1] = evt.getY(pid1);
			break;

		case 2:
			int pid2a = evt.findPointerIndex(evt.getPointerId(0));
			int pid2b = evt.findPointerIndex(evt.getPointerId(1));
			foci[0] = (evt.getX(pid2a) + evt.getX(pid2b)) / 2.f;
			foci[1] = (evt.getY(pid2a) + evt.getY(pid2b)) / 2.f;
			break;

		case 3:
			int pid3a = evt.findPointerIndex(evt.getPointerId(0));
			int pid3b = evt.findPointerIndex(evt.getPointerId(1));
			int pid3c = evt.findPointerIndex(evt.getPointerId(2));
			foci[0] = (evt.getX(pid3a) + evt.getX(pid3b) + evt.getX(pid3c)) / 3.f;
			foci[1] = (evt.getY(pid3a) + evt.getY(pid3b) + evt.getX(pid3c)) / 3.f;
			break;
		}
		return foci;
	}
	

	// calculates the difference in degrees of a rotation between two fingers
	private float getRotationDelta(MotionEvent evt) {
		double dx = (evt.getX(0) - evt.getX(1));
		double dy = (evt.getY(0) - evt.getY(1));
		double rad = Math.atan2(dy, dx);

		if(!rotateActive) {
			prevSlope = rad;
		}

		double rotationDelta = prevSlope - rad;
		prevSlope = rad;
		rotateActive = true;

		return (float) Math.toDegrees(rotationDelta);
	}

	// handles all touch events on the view
	@Override
	public boolean onTouchEvent(MotionEvent evt) {

		// pass the event on to the scale gesture detector
		mScaleGestureDetector.onTouchEvent(evt);

		final float[] xy = getFocus(evt);
		final float x = xy[0];
		final float y = xy[1];

		final int numPointers = evt.getPointerCount();

		final int action = evt.getAction();
		switch(action & MotionEvent.ACTION_MASK) {

		// for initializing a finger-down touch event
		case MotionEvent.ACTION_DOWN: {
//			Log.i(TAG, "touch action down: "+numPointers+"; "+x+", "+y);
			
			considerAsClick = true;
			
			touchDown[0] = x;
			touchDown[1] = y;

			prevNumPointers = 1;

			prevX = x;
			prevY = y;
			break;
		}

		// grabbing and moving the matrix
		case MotionEvent.ACTION_MOVE: {
//			Log.i(TAG, "move: "+numPointers+"; "+x+", "+y);
			
			boolean considerAsMove = true;
			
			if(considerAsClick) {
				float dx = x - touchDown[0];
				float dy = y - touchDown[1];
				if(Math.sqrt(dx*dx + dy*dy) > THRESHOLD_CLICK_POINT) {
					considerAsClick = false;
				}
				else {
					considerAsMove = false;
				}
			}

			if(prevNumPointers != numPointers) {
				prevX = x;
				prevY = y;
			}

			float dx = x - prevX;
			float dy = y - prevY;

			if(considerAsMove) {
				mMatrix.postTranslate(dx, dy);
			}

			prevX = x;
			prevY = y;

			if(numPointers == 2) {
				mMatrix.postRotate(-getRotationDelta(evt), x, y);
			}

			prevNumPointers = numPointers;

			break;
		}

		// release of all finger touches on view
		case MotionEvent.ACTION_UP: {
//			Log.i(TAG, "touch action up");
			
			if(considerAsClick) {
				resolveClick(xy);
			}
			
			prevNumPointers = 0;
			break;
		}

		// additional finger touch
		case MotionEvent.ACTION_POINTER_DOWN: {
//			Log.i(TAG, "touch action pointer down");
			considerAsClick = false;
			prevX = x;
			prevY = y;
			break;
		}

		// releasing an additional finger
		case MotionEvent.ACTION_POINTER_UP: {

//			Log.i(TAG, "touch action pointer up");

			int pindex = (evt.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			int pid = evt.findPointerIndex(evt.getPointerId(pindex));

			float sx = 0.f;
			float sy = 0.f;

//			Log.i(TAG, "pointer count: "+evt.getPointerCount());
//			Log.i(TAG, "pointer up id: "+pid);

			for(int i=0; i<numPointers; i++) {
				int pointer = evt.findPointerIndex(evt.getPointerId(i));
				if(pid != pointer) {
					sx += evt.getX(pointer);
					sy += evt.getY(pointer);
//					Log.d(TAG, "+1");
				}
			}

			prevX = sx / (numPointers-1);
			prevY = sy / (numPointers-1);

//			Log.w(TAG, "setting new xy: "+sx+", "+sy);

			rotateActive = false;
			prevNumPointers = numPointers - 1;

			break;
		}

		}

		invalidateViews();
		return true;
	}
	
	private void resolveClick(float[] xy) {
		double best = 0;
		ControlPoint bestPoint = null;
		
		for(ControlPoint testPoint : mControlPoints) {
			double test = testPoint.weighClickLikability(xy);
			if(test > best) {
				bestPoint = testPoint;
				best = test;
			}
		}
		
		if(bestPoint != null) {
			mIdentityManager.setValue(bestPoint.getId());
			mCallback.controlPointSelected(bestPoint);
		}
		
	}

	// simple class to forward scale events to the matrix
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

		@Override
		public void onScaleEnd(ScaleGestureDetector arg0) {
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			return true;
		}

		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			mMatrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
			return true;
		}
	}

	
	public interface Callback {
		public void controlPointSelected(ControlPoint controlPoint);
	}
}
