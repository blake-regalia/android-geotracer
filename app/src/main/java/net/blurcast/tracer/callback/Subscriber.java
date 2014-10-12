package net.blurcast.tracer.callback;

/**
 * Created by blake on 10/9/14.
 */
public abstract class Subscriber<EventType> {

    public abstract void event(EventType eventData, EventDetails eventDetails);

    public void error(String error) {

    }

}
