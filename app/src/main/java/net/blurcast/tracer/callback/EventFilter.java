package net.blurcast.tracer.callback;

/**
 * Created by blake on 10/10/14.
 */
public abstract class EventFilter<EventType> {

    public abstract boolean filter(EventType event);
}
