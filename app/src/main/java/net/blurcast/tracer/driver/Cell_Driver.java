package net.blurcast.tracer.driver;

import android.content.Context;
import android.telephony.CellInfo;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.List;

/**
 * Created by blake on 10/8/14.
 */
public class Cell_Driver {

    private static final String TAG = Cell_Driver.class.getSimpleName();

    private static Cell_Driver mInstance;
    private static TelephonyManager mTelephony;
    private static Context mContext;

    private static int iPhoneType;
    private static int iNetworkType;

    public static Cell_Driver getInstance(Context context) {
        if(mInstance == null) {
            mInstance = new Cell_Driver(context);
        }
        return mInstance;
    }

    public Cell_Driver(Context context) {
        mContext = context;

        mTelephony = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);

        iPhoneType = mTelephony.getPhoneType();
        iNetworkType = mTelephony.getNetworkType();

        //
        String phoneType = "other";
        switch(iPhoneType) {
            case TelephonyManager.PHONE_TYPE_CDMA:
                phoneType = "CDMA";
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                phoneType = "GSM";
                break;
            case TelephonyManager.PHONE_TYPE_NONE:
                phoneType = "NONE";
                break;
            case TelephonyManager.PHONE_TYPE_SIP:
                phoneType = "SIP";
                break;
        }

        String networkType = "other";
        switch(iNetworkType) {
            case TelephonyManager.NETWORK_TYPE_CDMA:
                networkType = "CDMA";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                networkType = "LTE";
                break;
            case TelephonyManager.NETWORK_TYPE_UMTS:
                networkType = "UMTS";
                break;
        }

        Log.d(TAG, "phone: " + phoneType + "; network: " + networkType);

//        List<CellInfo> cellInfoList = mTelephony.getAllCellInfo();
//        for(CellInfo cellInfo: cellInfoList) {
//            cellInfo.
//        }
    }


}
