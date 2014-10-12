package net.blurcast.tracer.callback;

import android.os.SystemClock;

/**
 * Created by blake on 10/10/14.
 */
public class EventDetails {

    private long mTimeSpanStart;
    private long mTimeSpanEnd;

    public EventDetails() {
        mTimeSpanStart = SystemClock.elapsedRealtime();
    }

    public void endTimeSpan() {
        mTimeSpanEnd = SystemClock.elapsedRealtime();
    }

    public long[] getTimeSpan() {
        return new long[]{
                mTimeSpanStart,
                mTimeSpanEnd
        };
    }

}
