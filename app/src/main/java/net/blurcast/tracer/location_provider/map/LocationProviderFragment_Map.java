package net.blurcast.tracer.location_provider.map;

import java.io.File;

import net.blurcast.tracer.R;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderFragment;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class LocationProviderFragment_Map extends LocationProviderFragment {
	
	private final Intent browseForMapFileIntent;
	
	private String mapFilePath;
	private SharedPreferences mSettings;

	private TextView mSelectMapFile;
	private LinearLayout mSelectSampleTypeView;
	private TextView mSelectSampleType;
	private TextView mSelectSampleTypeValue;
	private LinearLayout mSelectSampleAmountView;
	private TextView mSelectSampleAmount;
	private TextView mSelectSampleAmountValue;
	
	public LocationProviderFragment_Map() {
		super();
		mLayoutResource = R.layout.fragment_location_provider_map;

        browseForMapFileIntent = new Intent();
        browseForMapFileIntent.setType("file/*");
        browseForMapFileIntent.setAction(Intent.ACTION_GET_CONTENT);
	}

	@Override
	public void checkReadyBeforeLayout() {
		callbackReadyStateChangeListener(false);
	}

	@Override
	public void onCreateFragmentView() {
		
		mSettings = DefineLocationProviders.MAP.getSettings(getActivity());
		
		
		mSelectMapFile = (TextView) mView.findViewById(R.id.textView_selectMapFile);
		mSelectMapFile.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
	            startActivityForResult(Intent.createChooser(browseForMapFileIntent, "Select GVG"), 1);
			}
		});

		mSelectSampleType = (TextView) mView.findViewById(R.id.textView_selectScanType);
		
		mSelectSampleTypeValue = (TextView) mView.findViewById(R.id.textView_selectScanTypeValue);
		
		mSelectSampleAmount = (TextView) mView.findViewById(R.id.textView_selectScanAmount);
		
		mSelectSampleAmountValue = (TextView) mView.findViewById(R.id.textView_selectScanAmountValue);
		int sampleAmountPrefValue = mSettings.getInt(SensorLocationProvider_Map.PREF_SAMPLE_COUNT, -1);
		if(sampleAmountPrefValue > 0) {
			mSelectSampleAmountValue.setText(sampleAmountPrefValue+"");
		}
		
		mSelectSampleAmountView = (LinearLayout) mView.findViewById(R.id.linearLayout_sampleAmount);
		mSelectSampleAmountView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final EditText input = new EditText(getActivity());
				input.setInputType(InputType.TYPE_NUMBER_VARIATION_NORMAL | InputType.TYPE_CLASS_NUMBER);
				
				new AlertDialog.Builder(getActivity())
					.setTitle("Select Sample Amount")
					.setMessage("How much to sample each station")
					.setView(input)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							String valueStr = input.getText().toString();
							if(valueStr.length() != 0) {
								mSelectSampleAmountValue.setText(valueStr);
								int valueInt = Integer.parseInt(valueStr);
								mSettings.edit().putInt(SensorLocationProvider_Map.PREF_SAMPLE_COUNT, valueInt).commit();
							}
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// close
						}
					})
					.show();
			}
		});
		
		mSelectSampleTypeView = (LinearLayout) mView.findViewById(R.id.linearLayout_sampleType);
		String sampleTypePrefValue = mSettings.getString(SensorLocationProvider_Map.PREF_SAMPLE_TYPE, null);
		if(sampleTypePrefValue != null) {
			if(sampleTypePrefValue.equals(SensorLocationProvider_Map.SAMPLE_TYPE_PREF.N_SAMPLES)) {
				mSelectSampleTypeValue.setText("Number of samples");
			}
			else if(sampleTypePrefValue.equals(SensorLocationProvider_Map.SAMPLE_TYPE_PREF.N_SECONDS)) {
				mSelectSampleTypeValue.setText("Number of seconds");				
			}
		}
		mSelectSampleTypeView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Context context = getActivity();
				final RadioGroup group = new RadioGroup(getActivity());
				
				final RadioButton typeSample = new RadioButton(context);
				typeSample.setText("Number of samples");
				typeSample.setId(1);
				group.addView(typeSample);
				
				final RadioButton typeSeconds = new RadioButton(context);
				typeSeconds.setText("Duration in seconds");
				typeSample.setId(2);
				group.addView(typeSeconds);
				
				new AlertDialog.Builder(getActivity())
					.setTitle("Select Sample Type")
					.setMessage("Method to use for sampling")
					.setView(group)
					.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							if(typeSample.isChecked()) {
								mSettings.edit().putString(SensorLocationProvider_Map.PREF_SAMPLE_TYPE, SensorLocationProvider_Map.SAMPLE_TYPE_PREF.N_SAMPLES).commit();
								mSelectSampleTypeValue.setText("Number of samples");
							}
							else if(typeSeconds.isChecked()) {
								mSettings.edit().putString(SensorLocationProvider_Map.PREF_SAMPLE_TYPE, SensorLocationProvider_Map.SAMPLE_TYPE_PREF.N_SECONDS).commit();
								mSelectSampleTypeValue.setText("Number of seconds");
							}
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							// close
						}
					})
					.show();
			}
			
		});
		
        mapFilePath = mSettings.getString(SensorLocationProvider_Map.PREF_URI_MAP_FILE, null);
        
        if(mapFilePath != null) {
        	setMapFilePath(mapFilePath);
        }
		
	}
	
	private void setMapFilePath(String path) {
		mapFilePath = path;

    	File file = new File(mapFilePath);
    	if(file.isFile()) {
    		mSettings.edit().putString(SensorLocationProvider_Map.PREF_URI_MAP_FILE, mapFilePath).commit();
    		mSelectMapFile.setText("Map file: "+file.getName());
    		mSelectMapFile.setTextColor(Color.WHITE);
    		callbackReadyStateChangeListener(true);
    	}
    	else {
    		mapFilePath = null;
    	}
	}


	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if(resultCode == Activity.RESULT_OK) {
    		if(requestCode == 1) {
    			
    			if(data == null)
    				return;
    			
    			Uri imgUri = data.getData();
    			
    			String fmgrPath = imgUri.getPath();
        		String imgPath = getPathOfUri(imgUri);
        		
        		if(imgPath != null) {
        			setMapFilePath(imgPath);
        		}
        		else {
        			setMapFilePath(fmgrPath);
        		}
    		}
    	}
    }
	
    /** borrowed from stack-overflow
     * http://stackoverflow.com/questions/2169649/open-an-image-in-androids-built-in-gallery-app-programmatically
     */
    public String getPathOfUri(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getActivity().managedQuery(uri, projection, null, null, null);
        if(cursor != null) {
        	int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        	cursor.moveToFirst();
        	return cursor.getString(column_index);
        }
        else return null;
    }
	
}
