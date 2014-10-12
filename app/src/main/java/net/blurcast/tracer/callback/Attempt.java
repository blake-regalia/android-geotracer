package net.blurcast.tracer.callback;

/**
 * Created by blake on 10/8/14.
 */
public abstract class Attempt {

    private Attempt mWrap;

    public Attempt() {}

    public Attempt(Attempt wrap) {
        mWrap = wrap;
    }

    public abstract void ready();

    public void progress(int value) {
        if(mWrap != null) mWrap.progress(value);
    }

    public void update(String key, String value) {
        if(mWrap != null) mWrap.update(key, value);
    }

    public void error() {
        if(mWrap != null) mWrap.error();
    }
}
