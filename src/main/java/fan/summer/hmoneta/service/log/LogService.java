package fan.summer.hmoneta.service.log;

import fan.summer.hmoneta.database.entity.log.AppLogEntity;
import fan.summer.hmoneta.database.repository.log.AppLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    private final AppLogRepository appLogRepository;

    public LogService(AppLogRepository appLogRepository) {
        this.appLogRepository = appLogRepository;
    }

    public LogQueryResponse queryLogs(Long startTime, Long endTime, String level, String service, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AppLogEntity> result = appLogRepository.findByFilters(startTime, endTime, level, service, pageable);

        List<LogEntry> logs = result.getContent().stream()
                .map(this::toLogEntry)
                .collect(Collectors.toList());

        return new LogQueryResponse(logs, result.getTotalElements(), page, result.getTotalPages());
    }

    public List<String> getDistinctServices() {
        return appLogRepository.findDistinctServices();
    }

    public void cleanupOldLogs(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        appLogRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    private LogEntry toLogEntry(AppLogEntity entity) {
        return new LogEntry(
                entity.getTimestamp(),
                entity.getLevel(),
                entity.getService(),
                entity.getThread(),
                entity.getMessage(),
                entity.getException()
        );
    }

    public record LogEntry(
            Long timestamp,
            String level,
            String service,
            String thread,
            String message,
            String exception
    ) {}

    public record LogQueryResponse(
            List<LogEntry> logs,
            long total,
            int page,
            int totalPages
    ) {}
}
