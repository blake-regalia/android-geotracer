package net.blurcast.android.gvg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class GvgParser {

	private static final String ERR_SYNTAX = "GVG Syntax Error";
	private static final String ns = null;
	private XmlPullParser mParser;

	private HashMap<String, String> getAttributesAsHashMap(XmlPullParser parser) {
		HashMap<String, String> attrs = new HashMap<String, String>();
		for(int i=0; i<parser.getAttributeCount(); i++) {
			attrs.put(parser.getAttributeName(i), parser.getAttributeValue(i));
		}
		return attrs;
	}

	public GvgParser(InputStream in) throws XmlPullParserException, IOException {
		try {
			mParser = Xml.newPullParser();
			mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
			mParser.setInput(in, null);
			mParser.nextTag();
		} finally {
			in.close();
		}
	}

	public GVG getGvg() {
		try {
			return readGvg(mParser);
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private GVG readGvg(XmlPullParser parser) throws XmlPullParserException, IOException {
		ArrayList<GvgLayer> layers = new ArrayList<GvgLayer>();

		parser.require(XmlPullParser.START_TAG, ns, "gvg");
		while(parser.next() != XmlPullParser.END_TAG) {
			if(parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = parser.getName();

			// Starts by looking for a layer tag
			if(name.equals("layer")) {
				layers.add(readLayer(parser));
			}
			else {
				skip(parser);
			}
		}
		return new GVG(layers);
	}

	private String eventTypeToString(int evt) {
		switch(evt) {
		case XmlPullParser.END_DOCUMENT:
			return "END_DOCUMENT";
		case XmlPullParser.END_TAG:
			return "END_TAG";
		case XmlPullParser.ENTITY_REF:
			return "ENTITY_REF";
		case XmlPullParser.START_DOCUMENT:
			return "START_DOCUMENT";
		case XmlPullParser.START_TAG:
			return "START_TAG";
		case XmlPullParser.TEXT:
			return "TEXT";
		default:
			return evt+"";
		}
	}


	// Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
	// off
	// to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
	private GvgLayer readLayer(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "layer");

		ArrayList<GvgDrawable> drawables = new ArrayList<GvgDrawable>(); 
		ArrayList<ControlPoint> controlPoints = new ArrayList<ControlPoint>(); 
		HashMap<String, String> attrs = getAttributesAsHashMap(parser);
		String name = attrs.get("name");
		if(name == null) {
			missingAttributeError("layer", "name");
		}

		while(parser.next() != XmlPullParser.END_DOCUMENT) {
			int eventType = parser.getEventType();
			if(eventType != XmlPullParser.START_TAG) {
				if(eventType == XmlPullParser.END_TAG && parser.getName().equals("layer")) {
					break;
				}
				continue;
			}
			String tagName = parser.getName();

			if(tagName.equals("polygon")) {
				drawables.add(readPolygon(parser));
			}
			else if(tagName.equals("polyline")) {
				drawables.add(readPolyline(parser));
			}
			else if(tagName.equals("control")) {
				controlPoints.add(readControl(parser));
			}
			else {
				skip(parser);
			}
		}

		return new GvgLayer(name, drawables, controlPoints);
	}


	// Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
	// off
	// to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
	private GvgPolygon readPolygon(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "polygon");

		HashMap<String, String> attrs = getAttributesAsHashMap(parser);
		String points = attrs.get("points");
		if(points == null) {
			missingAttributeError("polygon", "points");
		}

		return new GvgPolygon(points, new GvgPaintStyle(attrs.get("style")));
	}
	
	//
	//
	private GvgPolyline readPolyline(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "polyline");
		
		HashMap<String, String> attrs = getAttributesAsHashMap(parser);
		String points = attrs.get("points");
		if(points == null) {
			missingAttributeError("polyline", "points");
		}
		
		return new GvgPolyline(points, new GvgPaintStyle(attrs.get("style")));
	}

	// Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
	// off
	// to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
	private ControlPoint readControl(XmlPullParser parser) throws XmlPullParserException, IOException {
		parser.require(XmlPullParser.START_TAG, ns, "control");

		HashMap<String, String> attrs = getAttributesAsHashMap(parser);
		String point = attrs.get("point");
		if(point == null) {
			missingAttributeError("control", "point");
		}

		return new ControlPoint(point, attrs);
	}


	// Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
	// if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
	// finds the matching END_TAG (as indicated by the value of "depth" being 0).
	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
		if (parser.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (parser.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}


	private void missingAttributeError(String tag, String attributeName) {
		Log.e(ERR_SYNTAX, "<"+tag+"/> tag requires "+attributeName+" attribute");
	}
}
