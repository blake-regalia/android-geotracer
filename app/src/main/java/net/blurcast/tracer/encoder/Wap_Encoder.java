package net.blurcast.tracer.encoder;

import android.content.Context;
import android.net.wifi.ScanResult;

import net.blurcast.android.util.ByteBuilder;
import net.blurcast.android.util.Encoder;
import net.blurcast.tracer.callback.Attempt;
import net.blurcast.tracer.callback.EventDetails;
import net.blurcast.tracer.callback.EventFilter;
import net.blurcast.tracer.callback.Subscriber;
import net.blurcast.tracer.driver.Wifi_Driver;
import net.blurcast.tracer.logger._Logger;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Created by blake on 10/10/14.
 */
public class Wap_Encoder {

    private static final Charset CHARSET_ISO_8859_1 = Charset.forName("ISO-8859-1");

    private boolean bStopFlag = false;
    private Attempt mStopAttempt;

    private Wifi_Driver mWifi;
    private _Logger mLogger;
    private EventFilter<ScanResult> mEventFilter;

    // for handling the scan events
    private Subscriber<List<ScanResult>> mScanResultSubscriber;

    // for the curious user to see info
    private Subscriber<List<ScanResult>> mCuriousSubscriber;


    public Wap_Encoder(Context context, _Logger logger) {
        this.init(context, logger, null);
    }

    public Wap_Encoder(Context context, _Logger logger, EventFilter<ScanResult> eventFilter) {
        this.init(context, logger, eventFilter);
    }

    private void init(Context context, _Logger logger, EventFilter<ScanResult> eventFilter) {
        mWifi = Wifi_Driver.getInstance(context);
        mLogger = logger;
        mEventFilter = eventFilter;
        this.subscribe();
    }


    private void subscribe() {

        // friending
        final Wap_Encoder friend = this;

        // no need to filter data
        if(mEventFilter == null) {
            mScanResultSubscriber = new Subscriber<List<ScanResult>>() {

                @Override
                public void event(List<ScanResult> eventData, EventDetails eventDetails) {

                    // notify ourselves that the scan completed
                    friend.scanFinished();

                    // then commit events to log
                    friend.logEvents(eventData, eventDetails);
                }
            };
        }

        // data wants to be filtered
        else {
            mScanResultSubscriber = new Subscriber<List<ScanResult>>() {

                @Override
                public void event(List<ScanResult> eventData, EventDetails eventDetails) {

                    // notify ourselves that the scan completed
                    friend.scanFinished();

                    // first, filter out any scan results the user doesn't want
                    Iterator<ScanResult> iterator = eventData.iterator();
                    while(iterator.hasNext()) {
                        if(!mEventFilter.filter(iterator.next())) {
                            iterator.remove();
                        }
                    }

                    // now commit the events to log
                    friend.logEvents(eventData, eventDetails);
                }
            };
        }
    }


    protected void scanFinished() {
        if(!bStopFlag) {
            mWifi.startScan();
        }
    }

    protected void logEvents(List<ScanResult> eventData, EventDetails eventDetails) {

        // the curious user wants to see what's going on
        if(mCuriousSubscriber != null) {
            mCuriousSubscriber.event(eventData, eventDetails);
        }

        // decode details
        long[] timeSpan = eventDetails.getTimeSpan();

        // prepare byte builder, generate data segment header & entry
        ByteBuilder bytes = new ByteBuilder(1+1+3+2+eventData.size()*3);

        // event type
        bytes.append(_Logger.TYPE_WAP_EVENT);

        // number of wireless access points for this scan [0-255]: 1 byte
        bytes.append(
                Encoder.encode_byte(eventData.size())
        );

        // time span (1000Hz resolution) start offset: 3 bytes (279.6 minutes run-time)
        bytes.append_3(
                mLogger.encodeOffsetTime(timeSpan[0])
        );

        // compute scan time
        long scanTime = timeSpan[1] - timeSpan[0];

        // scan time larger than 2 bytes can represent
        if(scanTime > 0xFFFF) {
            scanTime = 0xFFFF;
        }

        // time span (1000Hz resolution) scan time: 2 bytes (65 seconds max)
        bytes.append_2(
                Encoder.encode_char(
                        scanTime
                )
        );

        // iterate through all scan results
        for(ScanResult wap: eventData) {

            // encode id of this access point: 2 bytes
            bytes.append_2(
                    commitWapAndEncodeId(wap)
            );

            // encode signal strength right now [-128, 127]: 1 byte
            bytes.append(
                    Encoder.encode_byte(wap.level)
            );
        }

        // stop recording
        if(bStopFlag) {

            // relax wifi scan
            mWifi.relaxScanning();

            // submit pending data to logger
            this.close();
        }
    }


    private void close() {

        // wapInfoId: 2
        // wapInfo: 1+6+2+2+1

        // prepare byte builder
        ByteBuilder bytes = new ByteBuilder(1+2
                + mWapsLength*(2+1+6+2+2+1)
        );

        // first write type of this block: 1 byte
        bytes.append(_Logger.TYPE_WAP_INFO);

        // next, how many wapInfo will go here
        bytes.append_2(
                Encoder.encode_char(mWapsLength)
        );

        // encode each and every wapInfo
        for(WapInfo wapInfo: mWaps.keySet()) {

            // start with id of this wapInfo: 2 bytes
            bytes.append_2(
                    Encoder.encode_char(mWaps.get(wapInfo))
            );

            // append the whole wap info
            wapInfo.encodeInto(bytes);
        }

        // submit wap info
        mLogger.submit(bytes.getBytes());


        // high-level byte builder
        ByteBuffer byteBuffer = ByteBuffer.allocate(1+2
                +(2+2+32)*mWapSsidLength);

        // start of new block: 1 byte
        byteBuffer.put(_Logger.TYPE_WAP_SSID);

        // how many ssids will go here: 2 bytes
        byteBuffer.putChar((char) mWapSsidLength);

        // encode each and every ssid
        for(String ssid: mWapSssids.keySet()) {

            // start with id of this wapSsidId: 2 bytes
            byteBuffer.putChar(
                    mWapSssids.get(ssid)
            );

            // decode the string to bytes
            byte[] ssidBytes = ssid.getBytes(CHARSET_ISO_8859_1);

            // encode length of ssid string: 1 byte
            byteBuffer.put(
                    (byte) ssidBytes.length
            );

            // encode sequence of chars
            byteBuffer.put(
                    ssidBytes
            );
        }

        // submit ssids
        mLogger.submit(byteBuffer.array());


        // all done!
        mStopAttempt.ready();
    }


    // data structures for storing wireless access points
    private HashMap<WapInfo, Integer> mWaps = new HashMap<WapInfo, Integer>();
    private int mWapsLength = 0;

    private byte[] commitWapAndEncodeId(ScanResult wap) {

        // create uid for wap
        WapInfo wapInfo = new WapInfo(wap);
        int wapId = mWapsLength;

        // hashmap doesn't have this wap yet
        if(!mWaps.containsKey(wapInfo)) {

            // give it an id
            mWaps.put(wapInfo, mWapsLength++);
        }

        // hashmap already has this wap
        else {

            // fetch the id
            wapId = mWaps.get(wapInfo);
        }

        return Encoder.encode_char(wapId);
    }


    // hash of ssids
    private HashMap<String, Character> mWapSssids = new HashMap<String, Character>();
    private int mWapSsidLength = 0;

    private class WapInfo {

        private static final String OPEN = "Open";
        private static final String WEP = "WEP";
        private static final String WPA_PSK = "WPA-PSK";
        private static final String WPA2_PSK = "WPA2-PSK";
        private static final String WPA_EAP = "WPA-EAP";
        private static final String IEEE8021X = "IEEE8021X";


        private char[] mBssid = new char[3];
        private char mSecurity = 0;
        private char mSsidId;
        private char mFrequency;

        private char[] mUid = new char[5];


        public WapInfo(ScanResult wap) {

            // first, encode bssid
            int k = 0;

            // bssid strings are 17 characters long
            for(int i=0; i<17; i+=3) {

                // increment k
                k += 1;

                // encode into bssid character array
                mBssid[k] = mUid[k] = (char) ((Character.digit(wap.BSSID.charAt(i), 16) << 4)
                        + Character.digit(wap.BSSID.charAt(i+1), 16));
            }

            // security type(s)
            if(wap.capabilities.contains(OPEN)) {
                mSecurity |= 0x01;
            }
            else if(wap.capabilities.contains((WEP))) {
                mSecurity |= 0x02;
            }
            else {
                if(wap.capabilities.contains(WPA_PSK)) {
                    mSecurity |= 0x04;
                }
                if(wap.capabilities.contains(WPA2_PSK)) {
                    mSecurity |= 0x08;
                }
                if(wap.capabilities.contains(WPA_EAP)) {
                    mSecurity |= 0x10;
                }
                if(wap.capabilities.contains(IEEE8021X)) {
                    mSecurity |= 0x20;
                }
            }

            // capabilities identity
            mUid[3] = mSecurity;

            // finally add truncated hash of ssid string
            mUid[4] = (char) wap.SSID.hashCode();

            // frequency
            mFrequency = (char) wap.frequency;

            // ssid id
            if(mWapSssids.containsKey(wap.SSID)) {
                mSsidId = mWapSssids.get(wap.SSID);
            }
            else {
                mSsidId = (char) mWapSsidLength++;
                mWapSssids.put(wap.SSID, mSsidId);
            }
        }


        public void encodeInto(ByteBuilder bytes) {

            // encode BSSID: 6 bytes
            bytes.append_6(
                    Encoder.encode_bytes(mBssid)
            );

            // encode SSID name: 2 byte identifier
            bytes.append_2(
                    Encoder.encode_char(mSsidId)
            );

            // encode frequency: 2 bytes
            bytes.append_2(
                    Encoder.encode_char(mFrequency)
            );

            // encode security: 1 byte
            bytes.append(
                    Encoder.encode_byte((byte) mSecurity)
            );
        }


        public boolean uidMatches(char[] uid) {
            for(int i=0; i<uid.length; i++) {
                if(uid[i] != mUid[i]) return false;
            }
            return true;
        }


        @Override
        public int hashCode() {
            return mUid[0]*31*31*31*31 + mUid[1]*31*31*31 + mUid[2]*31*31 + mUid[3]*31 + mUid[4];
        }

        @Override
        public boolean equals(Object obj) {
            if(obj == null) return false;
            WapInfo other = (WapInfo) obj;
            return other.uidMatches(mUid);
        }

    }

    public void start() {
        this.start(null);
    }


    public void start(Subscriber<List<ScanResult>> subscriber) {
        mCuriousSubscriber = subscriber;
        bStopFlag = false;
        mStopAttempt = null;
        mWifi.enableScanning(new Attempt() {
            @Override
            public void ready() {
                mWifi.subscribeScanResults(mScanResultSubscriber);
                mWifi.startScan();
            }
        });
    }

    public void stop(Attempt attempt) {
        bStopFlag = true;
        mStopAttempt = attempt;
    }

}
