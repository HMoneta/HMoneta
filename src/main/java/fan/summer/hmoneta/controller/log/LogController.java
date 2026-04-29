package fan.summer.hmoneta.controller.log;

import fan.summer.hmoneta.service.log.LogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/hm/logs")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<LogService.LogQueryResponse> queryLogs(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String service,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        LogService.LogQueryResponse response = logService.queryLogs(startTime, endTime, level, service, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/services")
    public ResponseEntity<List<String>> getDistinctServices() {
        return ResponseEntity.ok(logService.getDistinctServices());
    }
}
