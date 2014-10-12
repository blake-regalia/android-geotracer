package net.blurcast.tracer.location_provider;

import net.blurcast.tracer.activity.SelectLocationProviderActivity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class LocationProviderFragment extends Fragment  {
	
	public static final String ARG_READY_CALLBACK = "readyCallback";

	protected View mView;
	protected int mLayoutResource;
	protected ReadyStateChangeListener mReadyStateChangeCallback;
	
	public LocationProviderFragment() {
		super();
	}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	
    	mReadyStateChangeCallback = SelectLocationProviderActivity.gInstance; 
    	
    	checkReadyBeforeLayout();
    	
    	// inflate the layout for this fragment
    	mView = inflater.inflate(mLayoutResource, container, false);
    	
    	onCreateFragmentView();
    	
    	return mView;
    }

	public abstract void checkReadyBeforeLayout();
	public abstract void onCreateFragmentView();
	
	public void callbackReadyStateChangeListener(boolean ready) {
		mReadyStateChangeCallback.readyStateChanged(ready);
	}
    
}
