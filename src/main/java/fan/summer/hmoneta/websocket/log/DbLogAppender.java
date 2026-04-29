package fan.summer.hmoneta.websocket.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DbLogAppender extends AppenderBase<ILoggingEvent> {

    public static final BlockingQueue<ILoggingEvent> LOG_QUEUE = new LinkedBlockingQueue<>(10000);

    @Override
    protected void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }
        try {
            if (!LOG_QUEUE.offer(event)) {
                addWarn("Log queue is full, dropping log message");
            }
        } catch (Exception e) {
            addError("Failed to enqueue log message", e);
        }
    }
}
