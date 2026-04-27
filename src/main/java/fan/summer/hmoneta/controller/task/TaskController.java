package fan.summer.hmoneta.controller.task;

import fan.summer.hmoneta.controller.task.dto.TaskInfoResp;
import fan.summer.hmoneta.service.task.TaskScheduleService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 定时任务管理控制器
 * 提供任务的查询、手动触发和动态配置功能
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/27
 */
@RestController
@RequestMapping("/hm/task")
public class TaskController {

    private final TaskScheduleService taskScheduleService;

    public TaskController(@Lazy TaskScheduleService taskScheduleService) {
        this.taskScheduleService = taskScheduleService;
    }

    /**
     * 获取所有定时任务列表
     */
    @GetMapping("/list")
    public ResponseEntity<List<TaskInfoResp>> getAllTasks() {
        return ResponseEntity.ok(taskScheduleService.getAllTasks());
    }

    /**
     * 手动触发指定任务
     */
    @PostMapping("/trigger/{taskName}")
    public ResponseEntity<String> triggerTask(@PathVariable String taskName) {
        try {
            taskScheduleService.triggerTask(taskName);
            return ResponseEntity.ok("任务 " + taskName + " 已触发");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("触发失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务的 cron 表达式
     */
    @PutMapping("/cron")
    public ResponseEntity<TaskInfoResp> updateCron(@RequestBody CronUpdateRequest request) {
        try {
            return ResponseEntity.ok(taskScheduleService.updateCronExpression(request.getTaskName(), request.getCronExpression()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 启用/禁用任务
     */
    @PutMapping("/enabled")
    public ResponseEntity<TaskInfoResp> setEnabled(@RequestBody EnabledUpdateRequest request) {
        try {
            return ResponseEntity.ok(taskScheduleService.setTaskEnabled(request.getTaskName(), request.isEnabled()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CronUpdateRequest {
        private String taskName;
        private String cronExpression;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnabledUpdateRequest {
        private String taskName;
        private boolean enabled;
    }
}
