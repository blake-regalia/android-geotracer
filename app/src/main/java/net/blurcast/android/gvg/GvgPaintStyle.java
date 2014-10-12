package net.blurcast.android.gvg;

import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;


public class GvgPaintStyle {
	private static final String TAG = "PaintStyle";

	public final boolean hasFill;
	public final boolean hasStroke;

	private Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	
	public GvgPaintStyle(String style) {

		// initialize paints before assigning values
		fillPaint.setStyle(Paint.Style.FILL);
		strokePaint.setStrokeWidth(0);
		strokePaint.setStyle(Paint.Style.STROKE);
		
		boolean setHasFill = false;
		boolean setHasStroke = false;
		
		String[] attrs = style.split(";");
		for(int i=0; i<attrs.length; i++) {
			String[] rule = attrs[i].split(":");
			if(rule.length != 2) continue;
			
			String key = rule[0];
			String value = rule[1];

			if(key.equals("fill")) {
				int color = Color.parseColor(value);
				fillPaint.setColor(color);
				setHasFill = true;
			}
			else if(key.equals("stroke")) {
				int color = Color.parseColor(value);
				strokePaint.setColor(color);
				setHasStroke = true;
			}
			else if(key.equals("stroke-width")) {
				strokePaint.setStrokeWidth(Integer.parseInt(value));
			}
			else {
				unknownStyleAttributeWarning(key);
			}
			
		}

		hasFill = setHasFill;
		hasStroke = setHasStroke;
	}

	public Paint getFillPaint() {
		return fillPaint;
	}
	
	public Paint getStrokePaint() {
		return strokePaint;
	}
	
	
	private void unknownStyleAttributeWarning(String key) {
		Log.w("Error", "unknown style attribute: "+key+"");
	}
}