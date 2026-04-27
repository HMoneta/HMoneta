package fan.summer.hmoneta.controller.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务信息响应DTO
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/27
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskInfoResp {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 默认 cron 表达式
     */
    private String defaultCron;

    /**
     * 当前 cron 表达式
     */
    private String currentCron;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 下次执行时间
     */
    private LocalDateTime nextExecutionTime;
}
