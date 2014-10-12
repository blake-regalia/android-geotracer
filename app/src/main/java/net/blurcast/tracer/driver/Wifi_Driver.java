package net.blurcast.tracer.driver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import net.blurcast.tracer.callback.Attempt;
import net.blurcast.tracer.callback.AttemptQueue;
import net.blurcast.tracer.callback.EventDetails;
import net.blurcast.tracer.callback.Subscriber;
import net.blurcast.tracer.callback.SubscriberSet;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by blake on 10/8/14.
 */
public class Wifi_Driver {

    // constants
    private static final int OBJECTIVE_NONE = -1;
    private static final String TAG = Wifi_Driver.class.getSimpleName();

    // primitives
    private static boolean bScanModeAvailable = false;
    private static boolean bScanModeEnabled = false;
    private static int nWifiStateObjective = OBJECTIVE_NONE;

    // data structures
    private static AttemptQueue mStateChangeAttempts = new AttemptQueue();
    private static SubscriberSet<List<ScanResult>> mSubscriberSet = new SubscriberSet<List<ScanResult>>();

    // resources
    private static Context mContext;
    private static WifiManager mWifiManager;
    private static Wifi_Driver mInstance;
    private static WifiManager.WifiLock mWifiLock;


    // single-instance interfacing
    public static Wifi_Driver getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new Wifi_Driver(context);
        }
        return mInstance;
    }


    // do this when initializing
    private Wifi_Driver(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        mWifiLock = mWifiManager.createWifiLock(WifiManager.WIFI_MODE_SCAN_ONLY, TAG);


        mContext.registerReceiver(mWifiStateBroadcastReceiver, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        mContext.registerReceiver(mWifiScanResultBroadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        // check scan mode availability as soon as this initializes
        this.checkScanModeAvailability();
    }


    // do this when closing resources
    public void close() {
        mContext.unregisterReceiver(mWifiStateBroadcastReceiver);
        mContext.unregisterReceiver(mWifiScanResultBroadcastReceiver);
    }


    // returns true if always scan mode is available
    private static void checkScanModeAvailability() {

        // Android 4.3 and above
        try {
            Method method = WifiManager.class.getMethod("isScanAlwaysAvailable");
            try {
                // scan is always available (no need to enable wifi)
                if(((Boolean) method.invoke(mWifiManager))) {
                    bScanModeEnabled = true;
                }
                // need to enable wifi in order to scan
                else {
                    bScanModeEnabled = false;
                }

                // scan mode is available
                bScanModeAvailable = true;
            }

            // error calling something that should have been fine
            catch(Exception x) {
                System.err.println(x.toString());
            }
        }
        // scan mode is not available
        catch(NoSuchMethodException x) {

            // wifi must be enabled in order to scan
            bScanModeAvailable = false;
        }
    }


    // will callback once wifi is ready for scanning only
    public void enableScanning(final Attempt attempt) {

        // scan mode is available and enabled (does not matter if wifi is on or off)
        if(bScanModeAvailable && bScanModeEnabled) {

            // acquire wifi lock
            mWifiLock.acquire();

            // notify ready
            attempt.ready();
        }

        // wifi must be enabled
        else {

            // enable wifi
            enable(new Attempt(attempt) {
                @Override
                public void ready() {

                    // acquire wifi lock
                    mWifiLock.acquire();

                    // notify ready
                    attempt.ready();
                }
            });
        }
    }


    public void relaxScanning() {

        // release wifi lock
        mWifiLock.release();
    }


    public boolean isEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void enable(Attempt attempt) {
        changeWifiState(true, attempt);
    }

    public void disable(Attempt attempt) {
        changeWifiState(false, attempt);
    }


    public void changeWifiState(boolean state, Attempt attempt) {

        // wifi state does not match what user wants
        if(mWifiManager.isWifiEnabled() != state) {

            // remember what the objective is
            nWifiStateObjective = state? WifiManager.WIFI_STATE_ENABLED: WifiManager.WIFI_STATE_DISABLED;

            // push this attempt to the group
            mStateChangeAttempts.add(attempt);

            // attempt to enable wifi
            mWifiManager.setWifiEnabled(state);
        }

        // wifi state already satisfies query
        else {
            attempt.ready();
        }
    }


    public void subscribeScanResults(Subscriber<List<ScanResult>> subscriber) {
        mSubscriberSet.add(subscriber);
    }


    // instantiate a broadcast receiver
    private static BroadcastReceiver mWifiStateBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            // no objective; ignore broadcast!
            if(nWifiStateObjective == OBJECTIVE_NONE) return;

            // get wifi state from intent
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);

            // objective was successful
            if (wifiState == nWifiStateObjective) {

                // callback everyone who attempted
                mStateChangeAttempts.ready();

                // reset objective
                nWifiStateObjective = OBJECTIVE_NONE;
            }

            // something else happened
            else {

                // determine which state
                switch (wifiState) {

                    // enabling or disabling...
                    case WifiManager.WIFI_STATE_ENABLING:
                    case WifiManager.WIFI_STATE_DISABLING:

                        // notify them if they are listening
                        mStateChangeAttempts.progress(wifiState);
                        break;

                    // something went wrong!
                    default:
                        mStateChangeAttempts.error("ERROR! Not expecting state: " + wifiState);
                        break;
                }
            }
        }
    };


    private static final int SCAN_MODE_IDLE = 0;
    private static final int SCAN_MODE_BUSY = 1;
    private static final int SCAN_MODE_ERROR = -1;

    private static EventDetails oScanEventDetails;
    private static int nScanMode = SCAN_MODE_IDLE;


    public boolean startScan() {

        // scan is already busy
        if(nScanMode != SCAN_MODE_IDLE) return false;

        // declare scan is busy now
        nScanMode = SCAN_MODE_BUSY;

        // start the clock
        oScanEventDetails = new EventDetails();

        // begin scanning
        return mWifiManager.startScan();
    }


    // instantiate a broadcast receiver
    private static BroadcastReceiver mWifiScanResultBroadcastReceiver = new BroadcastReceiver() {

        private List<ScanResult> scanResults;

        @Override
        public void onReceive(Context context, Intent intent) {

            // clock end of scan
            oScanEventDetails.endTimeSpan();

            // we did not initiate this scan; certain resources might be null; ignore this broadcast
            if(nScanMode != SCAN_MODE_BUSY) return;

            // collect results
            scanResults = mWifiManager.getScanResults();

            // declare ready for next scan
            nScanMode = SCAN_MODE_IDLE;

            // handle results
            mSubscriberSet.event(scanResults, oScanEventDetails);
        }
    };

}
