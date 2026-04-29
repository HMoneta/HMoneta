package fan.summer.hmoneta.service.log;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import fan.summer.hmoneta.database.entity.log.AppLogEntity;
import fan.summer.hmoneta.database.repository.log.AppLogRepository;
import fan.summer.hmoneta.websocket.log.DbLogAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncLogPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(AsyncLogPersistenceService.class);
    private static final int BATCH_SIZE = 100;
    private static final int POLL_TIMEOUT_MS = 10000;

    private final AppLogRepository appLogRepository;
    private final SseLogService sseLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile boolean running = true;

    public AsyncLogPersistenceService(AppLogRepository appLogRepository, SseLogService sseLogService) {
        this.appLogRepository = appLogRepository;
        this.sseLogService = sseLogService;
    }

    @PostConstruct
    public void init() {
        Thread consumerThread = new Thread(() -> startConsumerAsync(), "LogPersistenceConsumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @PreDestroy
    public void shutdown() {
        running = false;
    }

    private void startConsumerAsync() {
        log.info("Async log persistence service started");
        BlockingQueue<ILoggingEvent> queue = DbLogAppender.LOG_QUEUE;
        List<ILoggingEvent> batch = new ArrayList<>(BATCH_SIZE);

        while (running) {
            try {
                ILoggingEvent event = queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                if (event != null) {
                    batch.add(event);

                    while (batch.size() < BATCH_SIZE) {
                        ILoggingEvent next = queue.poll(10, TimeUnit.MILLISECONDS);
                        if (next == null) {
                            break;
                        }
                        batch.add(next);
                    }

                    persistBatch(batch);
                    broadcastBatch(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error processing log batch", e);
            }
        }

        if (!batch.isEmpty()) {
            try {
                persistBatch(batch);
                broadcastBatch(batch);
            } catch (Exception e) {
                log.error("Error processing final batch", e);
            }
        }
        log.info("Async log persistence service stopped");
    }

    private void persistBatch(List<ILoggingEvent> events) {
        List<AppLogEntity> entities = new ArrayList<>(events.size());
        for (ILoggingEvent event : events) {
            entities.add(toEntity(event));
        }
        appLogRepository.saveAll(entities);
    }

    private void broadcastBatch(List<ILoggingEvent> events) {
        for (ILoggingEvent event : events) {
            try {
                String serviceName = extractServiceName(event.getLoggerName());
                Map<String, Object> logMessage = new HashMap<>();
                logMessage.put("timestamp", event.getTimeStamp());
                logMessage.put("level", event.getLevel().toString());
                logMessage.put("service", serviceName);
                logMessage.put("thread", event.getThreadName());
                logMessage.put("message", event.getFormattedMessage());

                if (event.getThrowableProxy() != null) {
                    logMessage.put("exception", event.getThrowableProxy().getMessage());
                }

                String json = objectMapper.writeValueAsString(logMessage);
                sseLogService.broadcast(serviceName, json);
            } catch (Exception e) {
                log.error("Error broadcasting log", e);
            }
        }
    }

    private AppLogEntity toEntity(ILoggingEvent event) {
        AppLogEntity entity = new AppLogEntity();
        entity.setTimestamp(event.getTimeStamp());
        entity.setLevel(event.getLevel().toString());
        entity.setService(extractServiceName(event.getLoggerName()));
        entity.setThread(event.getThreadName());
        entity.setMessage(event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            entity.setException(event.getThrowableProxy().getMessage());
        }
        return entity;
    }

    private String extractServiceName(String loggerName) {
        if (loggerName == null) {
            return "UNKNOWN";
        }
        String[] parts = loggerName.split("\\.");
        String className = parts[parts.length - 1];
        if (className.endsWith("Impl")) {
            className = className.substring(0, className.length() - 4);
        }
        return className;
    }
}
