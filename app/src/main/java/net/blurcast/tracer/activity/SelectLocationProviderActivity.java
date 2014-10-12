package net.blurcast.tracer.activity;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;

import net.blurcast.tracer.R;
import net.blurcast.tracer.location_provider.DefineLocationProviders;
import net.blurcast.tracer.location_provider.LocationProviderDefinition;
import net.blurcast.tracer.location_provider.LocationProviderFragment;
import net.blurcast.tracer.location_provider.ReadyStateChangeListener;
import net.blurcast.tracer.service.MainService;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;

public class SelectLocationProviderActivity extends Activity implements ReadyStateChangeListener {
	
	public static SelectLocationProviderActivity gInstance = null;

	private FragmentManager mFragmentManager;
	private Fragment mFragment;
	private FragmentTransaction mFragmentTransaction;
	private RadioGroup mLocationProviders;

	private LocationProviderDefinition mLocationProviderDefinition; 

	private Context mContext;

	private Switch mServiceSwitch;
	private LocationProviderFragment mFragmentClass;
	private SelectLocationProviderActivity mSelf;

	private SharedPreferences mSettings;

	private boolean flagSetServiceSwitchEnabled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_location_provider);
		mContext = this;

		// set reference to myself
		mSelf = this;
		
		gInstance = this;

		// get reference to the fragment manager
		mFragmentManager = getFragmentManager();

		// reference the preferences of this activity
		mSettings = getSharedPreferences(ServiceControlActivity.PREFERENCE_URI, Context.MODE_PRIVATE);

		// obtain the uri of the established location provider if it exists 
		String locationProviderUri = mSettings.getString(ServiceControlActivity.URI_LOCATION_PROVIDER, null);

		// if the location provider is set...
		if(locationProviderUri != null) {

			// point the location provider object to the given uri if it exists
			LocationProviderDefinition definition = DefineLocationProviders.getLocationProviderDefinition(locationProviderUri);
			if(definition != null) {
				String name = definition.getName();
			}
		}


		// get reference to layout inflater
		final LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		// get reference to the radio group view for adding radio buttons to
		mLocationProviders = (RadioGroup) this.findViewById(R.id.radioGroup_locationProviders);

		// prepare variable to hold each radio button while adding to group
		RadioButton radioButton;

		// add a radio button for each location provider
		Iterator<Entry<String, String>> uriIterator = DefineLocationProviders.getLocationProviderUrisAndNames();

		// keep track of the index for the radio id 
		int i = 0;

		// prepare a variable to hold the entry pair of the uri to name for location provider
		Entry<String, String> uriNameEntry;

		// iterate through uris
		while(uriIterator.hasNext()) {
			uriNameEntry = uriIterator.next();
			radioButton = (RadioButton) inflater.inflate(R.layout.radio_button, mLocationProviders, false);
			radioButton.setText(uriNameEntry.getValue());
			radioButton.setId(i);

			// if this location provider is the one saved from the preferences
			if(uriNameEntry.getKey().equals(locationProviderUri)) {
				// set the radio button as checked
				radioButton.setChecked(true);

				// update the location provider selection
				selectLocationProvider(i);
			}

			// bind the onclick listener
			radioButton.setOnClickListener(OnClickListener_locationProvider);

			// add this radio button to the radio group view 
			mLocationProviders.addView(radioButton);

			// increment the id counter
			i += 1;
		}


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_select_location_provider, menu);

		// set reference to the switch in the action bar
		mServiceSwitch = (Switch) menu.getItem(0).getActionView().findViewById(R.id.switch_serviceControl);

		// bind click listener to the switch in the action bar
		mServiceSwitch.setOnClickListener(new OnClickListener() {

			public void onClick(View view)  {
				boolean enabledService = ((Switch) view).isChecked();

				// if service is toggled on
				if(enabledService) {
					sendCommandToService(MainService.COMMAND.ENABLE_SERVICE, 0);
				}
				else {
					sendCommandToService(MainService.COMMAND.DISABLE_SERVICE, 0);
				}
			}
		});

		// if the flag was set to enable the service switch
		if(flagSetServiceSwitchEnabled) {
			mServiceSwitch.setEnabled(true);
		}

		return true;
	}

	/**
	 * 
	 * @param command
	 */
	public void sendCommandToService(String command, int arg) {
		// create a new intent for the service
		Intent serviceIntent = new Intent(this, MainService.class);

		// put the command in the intent
		serviceIntent.setAction(command);

		// set the extra int argument
		serviceIntent.putExtra(MainService.INTENT_NAME_MESSAGE.INT, arg);

		// send the intent to the service
		this.startService(serviceIntent);
	}

	/**
	 * notifies the activity to update the preferences of the location provider and to enable the service control switch
	 * @param ready
	 */
	public void readyStateChanged(boolean ready) {

		// if the options menu hasn't been created yet
		if(mServiceSwitch == null) {

			// set the flag to enable the service switch as soon as it is created
			flagSetServiceSwitchEnabled = true;
		}
		else {
			// enable the control switch
			mServiceSwitch.setEnabled(ready);
		}

		// if the fragment activity verified the settings are ready and okay
		if(ready && mLocationProviderDefinition != null) {

			// then update the settings for the preferred location provider
			mSettings.edit().putString(ServiceControlActivity.URI_LOCATION_PROVIDER, mLocationProviderDefinition.getUri()).apply();
		}
	}



	private OnClickListener OnClickListener_locationProvider = new OnClickListener() {		
		public void onClick(View view) {
			selectLocationProvider(view.getId());
		}
	};

	private void selectLocationProvider(int id) {

		// set the ready state to false (not ready)
		readyStateChanged(false);

		// get reference to a new fragment transaction
		FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

		// prepare a fragment
		mFragmentClass = null;

		// get the definition of the location provider at the given index
		mLocationProviderDefinition = DefineLocationProviders.getLocationProviderDefinition(id);
		
		if(mLocationProviderDefinition == null) {
			Log.e("location provider", "id: "+id+" does not exist.");
			return;
		}

		// create the fragment view for the specified location provider
		Class<?> fragmentClass = mLocationProviderDefinition.getPreferencesFragment();
		try {
			mFragmentClass = (LocationProviderFragment) fragmentClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

		// replace the fragment with a new example fragment
		fragmentTransaction.replace(R.id.fragment_locationProviders, mFragmentClass);

		// commit the replacement
		fragmentTransaction.commit();

	}

}
