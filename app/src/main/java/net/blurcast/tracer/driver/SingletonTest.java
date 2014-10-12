package net.blurcast.tracer.driver;

/**
 * Created by blake on 10/9/14.
 */
public enum SingletonTest {
    INSTANCE;

    SingletonTest() {
        // init
        System.out.println("hello");
    }

    public static SingletonTest getInstance() {
        return INSTANCE;
    }


}