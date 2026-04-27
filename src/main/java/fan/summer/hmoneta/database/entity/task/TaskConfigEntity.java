package fan.summer.hmoneta.database.entity.task;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 定时任务配置实体
 * 存储任务的自定义 cron 表达式和启用状态
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/27
 */
@Entity
@Table(name = "task_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskConfigEntity {

    /**
     * 任务名称（主键）
     */
    @Id
    private String taskName;

    /**
     * 自定义 cron 表达式
     * 为空时使用默认配置
     */
    private String cronExpression;

    /**
     * 是否启用
     */
    private boolean enabled = true;

    /**
     * 是否使用自定义 cron
     * false = 使用 @Scheduled 默认配置
     * true = 使用自定义 cronExpression
     */
    private boolean customCron = false;
}
