package fan.summer.hmoneta.service.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LogCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogCleanupScheduler.class);
    private static final int RETENTION_DAYS = 30;

    private final LogService logService;

    public LogCleanupScheduler(LogService logService) {
        this.logService = logService;
    }

    @Scheduled(cron = "0 0 3 ? * SUN")
    public void cleanupOldLogs() {
        log.info("Starting log cleanup task, removing logs older than {} days", RETENTION_DAYS);
        try {
            logService.cleanupOldLogs(RETENTION_DAYS);
            log.info("Log cleanup completed successfully");
        } catch (Exception e) {
            log.error("Log cleanup failed", e);
        }
    }
}
