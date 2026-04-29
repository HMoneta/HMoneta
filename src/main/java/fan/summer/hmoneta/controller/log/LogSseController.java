package fan.summer.hmoneta.controller.log;

import fan.summer.hmoneta.service.log.SseLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/hm/logs")
public class LogSseController {

    private final SseLogService sseLogService;

    public LogSseController(SseLogService sseLogService) {
        this.sseLogService = sseLogService;
    }

    @GetMapping("/stream")
    public SseEmitter streamLogs(@RequestParam(required = false) String service) {
        return sseLogService.subscribe(service);
    }
}
