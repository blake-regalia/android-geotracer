package net.blurcast.tracer.logger;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import net.blurcast.android.util.ByteBuilder;
import net.blurcast.android.util.Encoder;

/**
 * Created by blake on 10/11/14.
 */
public abstract class _Logger {

    public static final byte TYPE_OPEN       = 0x00;
    public static final byte TYPE_CLOSE      = 0x01;

    public static final byte TYPE_WAP_EVENT  = 0x10;
    public static final byte TYPE_WAP_INFO   = 0x11;
    public static final byte TYPE_WAP_SSID   = 0x12;

    private long lTimeStarted;
    private int iApkVersion;

    protected Context mContext;


    public abstract void submit(byte[] data);
    public abstract void close();


    public _Logger(Context context) {
        mContext = context;
        try {
            iApkVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch(PackageManager.NameNotFoundException e) {
            iApkVersion = -1;
        }
    }

    public void writeHeader() {
        long now = System.currentTimeMillis();
        lTimeStarted = SystemClock.elapsedRealtime();

        ByteBuilder bytes = new ByteBuilder(1+2+8);

        // this is an opening block
        bytes.append(TYPE_OPEN);

        // version info: 2 bytes
        bytes.append_2(
                Encoder.encode_char(iApkVersion)
        );

        // start time (real-world time): 8 bytes
        bytes.append_8(
                Encoder.encode_long(now)
        );

        // submit header
        this.submit(bytes.getBytes());
    }


    public byte[] encodeOffsetTime(long elapsed) {
        return Encoder.encode_long_to_3_bytes(
                elapsed - lTimeStarted
        );
    }

}
