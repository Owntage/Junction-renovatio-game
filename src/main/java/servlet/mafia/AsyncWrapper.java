package servlet.mafia;

import javax.servlet.AsyncContext;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Owntage on 11/5/2016.
 */
public class AsyncWrapper {
    public final AsyncContext context;
    public volatile boolean isFinished;
    public final Lock lock = new ReentrantLock();
    public AsyncWrapper(AsyncContext context) {
        this.context = context;
    }
}
