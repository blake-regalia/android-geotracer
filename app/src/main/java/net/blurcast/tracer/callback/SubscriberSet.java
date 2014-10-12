package net.blurcast.tracer.callback;

import java.util.ArrayList;

/**
 * Created by blake on 10/9/14.
 */
public class SubscriberSet<EventType> {

    private ArrayList<Subscriber> mSubscriberList = new ArrayList<Subscriber>();

    public void add(Subscriber<EventType> subscriber) {
        mSubscriberList.add(subscriber);
    }

    public void event(EventType eventData, EventDetails eventDetails) {
        for(Subscriber<EventType> subscriber: mSubscriberList) {
            subscriber.event(eventData, eventDetails);
        }
    }

    public void error(String error) {
        for(Subscriber subscriber: mSubscriberList) {
            subscriber.error(error);
        }
    }

}
