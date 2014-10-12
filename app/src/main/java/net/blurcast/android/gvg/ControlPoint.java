package net.blurcast.android.gvg;

import java.util.HashMap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

public class ControlPoint extends GvgDrawable {

	private static final float THRESHOLD_CLICK_RADIUS = 38.f;
	private static final float RADIUS_OUTER = 16.f;
	private static final float RADIUS_INNER = 12.f;
	
	private float[] point;	
	private float[] drawPoint;
	private float[] transformedPoints;
	private int mId;

	private HashMap<String, String> attrs;

	private Matrix mMatrix;
	private IdentityManager mIdentityManager;
	private Paint mPaintInner = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mPaintOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mPaintInnerSelected = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	public ControlPoint(String setPoint, HashMap<String, String> setAttrs) {
		point = parseCoordinate(setPoint);
		attrs = setAttrs;
		
		mBounds = new RectF(point[0], point[1], point[0], point[1]);

		mPaintInner.setColor(Color.WHITE);
		mPaintOuter.setColor(Color.BLACK);
		mPaintInnerSelected.setColor(0xff5555ff);
	}
	
	public void setMatrixAndIdentityManager(Matrix matrix, IdentityManager identityManager) {
		mMatrix = matrix;
		mIdentityManager = identityManager;
		mId = mIdentityManager.getNewId();
	}
	
	@Override
	public void prepare(RectF bounds, int width, int height) {
		float xFactor = width / bounds.width();
		float yFactor = height / bounds.height();
		
		int count = 0;
		float x = (point[0] - bounds.left) * xFactor;
		float y = (point[1] - bounds.top) * yFactor;
		
		drawPoint = new float[]{x, y};
	}
	
	public void drawToCanvas(Canvas canvas) {
		transformedPoints = new float[2];
		mMatrix.mapPoints(transformedPoints, drawPoint);

		canvas.drawCircle(transformedPoints[0], transformedPoints[1], RADIUS_OUTER, mPaintOuter);

		Paint usePaintInner = mPaintInner;
		if(mIdentityManager.getValue() == mId) {
			usePaintInner = mPaintInnerSelected;
		}

		canvas.drawCircle(transformedPoints[0], transformedPoints[1], RADIUS_INNER, usePaintInner);

//		Log.d(TAG, "drawing CONTROL POINT!! "+point[0]+","+point[1]+" => "+transformedPoints[0]+","+transformedPoints[1]);
	}
	
	public int getId() {
		return mId;
	}
	
	public String getName() {
		return attrs.get("name");
	}

	public float getX() {
		return point[0];
	}
	
	public float getY() {
		return point[1];
	}
	
	public double weighClickLikability(float[] screenXY) {
		float dx = transformedPoints[0] - screenXY[0];
		float dy = transformedPoints[1] - screenXY[1];
		double dd = Math.sqrt(dx*dx + dy*dy);
		if(dd > THRESHOLD_CLICK_RADIUS) {
			return 0.0;
		}
		return 1.0 / dd;
	}

}
