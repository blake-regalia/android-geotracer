package net.blurcast.tracer.callback;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by blake on 10/8/14.
 */
public class AttemptQueue {

    private Queue<Attempt> mQueue = new LinkedList<Attempt>();

    public void add(Attempt attempt) {
        mQueue.add(attempt);
    }

    public void progress(int value) {
        for(Attempt a: mQueue) {
            a.progress(value);
        }
    }

    public void update(String key, String value) {
        for(Attempt a: mQueue) {
            a.update(key, value);
        }
    }

    public void ready() {
        while(!mQueue.isEmpty()) {
            mQueue.remove().ready();
        }
    }

    public void error(String error) {
        while(!mQueue.isEmpty()) {
            mQueue.remove().error();
        }
    }


}
