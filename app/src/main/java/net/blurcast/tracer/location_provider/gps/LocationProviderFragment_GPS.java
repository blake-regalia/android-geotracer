package net.blurcast.tracer.location_provider.gps;

import net.blurcast.tracer.R;
import net.blurcast.tracer.location_provider.LocationProviderFragment;
import android.widget.ListView;

public class LocationProviderFragment_GPS extends LocationProviderFragment {

	public LocationProviderFragment_GPS() {
		super();
		mLayoutResource = R.layout.fragment_location_provider_gps;
	}

	@Override
	public void checkReadyBeforeLayout() {
		callbackReadyStateChangeListener(true);
	}
	
	public void onCreateFragmentView() {
//		ListView settingsList = (ListView) mView.findViewById(R.id.listView_settings);
	}
}
