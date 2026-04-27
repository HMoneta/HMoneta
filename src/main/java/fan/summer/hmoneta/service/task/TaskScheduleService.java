package fan.summer.hmoneta.service.task;

import fan.summer.hmoneta.controller.task.dto.TaskInfoResp;
import fan.summer.hmoneta.database.entity.task.TaskConfigEntity;
import fan.summer.hmoneta.database.repository.task.TaskConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务调度服务
 * 提供任务的注册、查询、动态修改cron和手动触发功能
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/27
 */
@Slf4j
@Service
@Lazy
public class TaskScheduleService {

    private final ApplicationContext applicationContext;
    private final TaskConfigRepository taskConfigRepository;
    private final TaskScheduler taskScheduler;

    /**
     * 任务注册表：taskName -> 任务信息
     */
    private final Map<String, TaskRegistration> taskRegistry = new ConcurrentHashMap<>();

    /**
     * 任务名称 -> ScheduledFuture
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    public TaskScheduleService(ApplicationContext applicationContext,
                               TaskConfigRepository taskConfigRepository,
                               TaskScheduler taskScheduler) {
        this.applicationContext = applicationContext;
        this.taskConfigRepository = taskConfigRepository;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 初始化任务注册表
     * 根据任务名称查找对应的 bean 和方法
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("[TaskSchedule] 开始初始化定时任务...");

        // 定义已知任务配置：taskName -> (描述, 默认cron, 方法名)
        Map<String, TaskDefinition> knownTasks = new HashMap<>();
        knownTasks.put("dnsUpdateTask", new TaskDefinition("dnsUpdateTask", "DDNS 更新任务", "0 0/10 * * * ?", "updater"));
        knownTasks.put("acmeUpdateTask", new TaskDefinition("acmeUpdateTask", "ACME 证书续期任务", "0 0 0 * * ?", "acmeUpdater"));

        // 扫描所有 Bean
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(org.springframework.stereotype.Component.class);

        for (Object bean : beans.values()) {
            Class<?> beanClass = bean.getClass();
            String beanName = beanClass.getSimpleName();
            String taskName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

            // 检查是否是已知任务
            TaskDefinition definition = knownTasks.get(taskName);
            if (definition == null) {
                continue;
            }

            // 查找指定名称的方法
            Method taskMethod = findTaskMethod(beanClass, definition.methodName);
            if (taskMethod == null) {
                log.warn("[TaskSchedule] 任务 {} 的方法 {} 未找到", taskName, definition.methodName);
                continue;
            }

            // 获取保存的配置
            Optional<TaskConfigEntity> configOpt = taskConfigRepository.findById(taskName);
            TaskConfigEntity config = configOpt.orElseGet(() -> createDefaultConfig(taskName));

            // 注册任务
            registerTask(taskName, definition, bean, taskMethod, config);
        }

        log.info("[TaskSchedule] 定时任务初始化完成，共注册 {} 个任务", taskRegistry.size());
    }

    private Method findTaskMethod(Class<?> beanClass, String methodName) {
        for (Method method : beanClass.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    private TaskConfigEntity createDefaultConfig(String taskName) {
        TaskConfigEntity config = new TaskConfigEntity();
        config.setTaskName(taskName);
        config.setEnabled(true);
        config.setCustomCron(false);
        return config;
    }

    private void registerTask(String taskName, TaskDefinition definition, Object bean,
                              Method method, TaskConfigEntity config) {
        Runnable taskRunnable = () -> {
            try {
                method.setAccessible(true);
                method.invoke(bean);
            } catch (Exception e) {
                log.error("[TaskSchedule] 任务 {} 执行失败: {}", taskName, e.getMessage());
            }
        };

        String cronExpression = config.isCustomCron() ? config.getCronExpression() : definition.defaultCron;
        CronTrigger trigger = new CronTrigger(cronExpression);

        ScheduledFuture<?> future = taskScheduler.schedule(taskRunnable, trigger);

        TaskRegistration registration = new TaskRegistration();
        registration.setTaskName(taskName);
        registration.setDescription(definition.description);
        registration.setDefaultCron(definition.defaultCron);
        registration.setCurrentCron(cronExpression);
        registration.setEnabled(config.isEnabled());
        registration.setScheduledFuture(future);
        registration.setBean(bean);
        registration.setMethod(method);

        taskRegistry.put(taskName, registration);
        scheduledTasks.put(taskName, future);

        log.info("[TaskSchedule] 注册任务: {}, cron: {}, enabled: {}", taskName, cronExpression, config.isEnabled());
    }

    /**
     * 获取所有任务信息
     */
    public List<TaskInfoResp> getAllTasks() {
        List<TaskInfoResp> result = new ArrayList<>();

        for (TaskRegistration reg : taskRegistry.values()) {
            TaskInfoResp resp = new TaskInfoResp();
            resp.setTaskName(reg.getTaskName());
            resp.setDescription(reg.getDescription());
            resp.setDefaultCron(reg.getDefaultCron());
            resp.setCurrentCron(reg.getCurrentCron());
            resp.setEnabled(reg.isEnabled());

            ScheduledFuture<?> future = reg.getScheduledFuture();
            if (future != null && !future.isDone()) {
                long nextExec = future.getDelay(TimeUnit.MILLISECONDS);
                if (nextExec > 0) {
                    resp.setNextExecutionTime(LocalDateTime.now().plusNanos(nextExec * 1_000_000));
                }
            }

            result.add(resp);
        }

        return result;
    }

    /**
     * 手动触发指定任务
     */
    public void triggerTask(String taskName) {
        TaskRegistration reg = taskRegistry.get(taskName);
        if (reg == null) {
            throw new IllegalArgumentException("任务不存在: " + taskName);
        }

        log.info("[TaskSchedule] 手动触发任务: {}", taskName);
        try {
            reg.getMethod().setAccessible(true);
            reg.getMethod().invoke(reg.getBean());
        } catch (Exception e) {
            log.error("[TaskSchedule] 任务 {} 执行失败: {}", taskName, e.getMessage());
            throw new RuntimeException("任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 更新任务的 cron 表达式
     */
    public TaskInfoResp updateCronExpression(String taskName, String cronExpression) {
        TaskRegistration reg = taskRegistry.get(taskName);
        if (reg == null) {
            throw new IllegalArgumentException("任务不存在: " + taskName);
        }

        // 取消旧任务
        ScheduledFuture<?> oldFuture = scheduledTasks.get(taskName);
        if (oldFuture != null && !oldFuture.isDone()) {
            oldFuture.cancel(false);
        }

        // 创建新任务
        Runnable taskRunnable = () -> {
            try {
                reg.getMethod().setAccessible(true);
                reg.getMethod().invoke(reg.getBean());
            } catch (Exception e) {
                log.error("[TaskSchedule] 任务 {} 执行失败: {}", taskName, e.getMessage());
            }
        };

        CronTrigger trigger = new CronTrigger(cronExpression);
        ScheduledFuture<?> newFuture = taskScheduler.schedule(taskRunnable, trigger);

        // 更新注册表
        reg.setCurrentCron(cronExpression);
        reg.setScheduledFuture(newFuture);
        scheduledTasks.put(taskName, newFuture);

        // 保存配置到数据库
        TaskConfigEntity config = taskConfigRepository.findById(taskName)
                .orElseGet(() -> createDefaultConfig(taskName));
        config.setCronExpression(cronExpression);
        config.setCustomCron(true);
        taskConfigRepository.save(config);

        log.info("[TaskSchedule] 更新任务 {} cron 表达式为: {}", taskName, cronExpression);

        // 返回更新后的信息
        TaskInfoResp resp = new TaskInfoResp();
        resp.setTaskName(reg.getTaskName());
        resp.setDescription(reg.getDescription());
        resp.setDefaultCron(reg.getDefaultCron());
        resp.setCurrentCron(cronExpression);
        resp.setEnabled(reg.isEnabled());

        long nextExec = newFuture.getDelay(TimeUnit.MILLISECONDS);
        if (nextExec > 0) {
            resp.setNextExecutionTime(LocalDateTime.now().plusNanos(nextExec * 1_000_000));
        }

        return resp;
    }

    /**
     * 启用/禁用任务
     */
    public TaskInfoResp setTaskEnabled(String taskName, boolean enabled) {
        TaskRegistration reg = taskRegistry.get(taskName);
        if (reg == null) {
            throw new IllegalArgumentException("任务不存在: " + taskName);
        }

        if (enabled) {
            // 重新调度任务
            ScheduledFuture<?> oldFuture = scheduledTasks.get(taskName);
            if (oldFuture != null && !oldFuture.isDone()) {
                oldFuture.cancel(false);
            }

            Runnable taskRunnable = () -> {
                try {
                    reg.getMethod().setAccessible(true);
                    reg.getMethod().invoke(reg.getBean());
                } catch (Exception e) {
                    log.error("[TaskSchedule] 任务 {} 执行失败: {}", taskName, e.getMessage());
                }
            };

            CronTrigger trigger = new CronTrigger(reg.getCurrentCron());
            ScheduledFuture<?> newFuture = taskScheduler.schedule(taskRunnable, trigger);
            reg.setScheduledFuture(newFuture);
            scheduledTasks.put(taskName, newFuture);
        } else {
            // 取消任务
            ScheduledFuture<?> future = scheduledTasks.get(taskName);
            if (future != null && !future.isDone()) {
                future.cancel(false);
            }
            reg.setScheduledFuture(null);
        }

        reg.setEnabled(enabled);

        // 保存配置
        TaskConfigEntity config = taskConfigRepository.findById(taskName)
                .orElseGet(() -> createDefaultConfig(taskName));
        config.setEnabled(enabled);
        taskConfigRepository.save(config);

        log.info("[TaskSchedule] 设置任务 {} 启用状态为: {}", taskName, enabled);

        TaskInfoResp resp = new TaskInfoResp();
        resp.setTaskName(reg.getTaskName());
        resp.setDescription(reg.getDescription());
        resp.setDefaultCron(reg.getDefaultCron());
        resp.setCurrentCron(reg.getCurrentCron());
        resp.setEnabled(enabled);

        if (enabled && reg.getScheduledFuture() != null) {
            long nextExec = reg.getScheduledFuture().getDelay(TimeUnit.MILLISECONDS);
            if (nextExec > 0) {
                resp.setNextExecutionTime(LocalDateTime.now().plusNanos(nextExec * 1_000_000));
            }
        }

        return resp;
    }

    /**
     * 任务定义
     */
    private static class TaskDefinition {
        String taskName;
        String description;
        String defaultCron;
        String methodName;

        TaskDefinition(String taskName, String description, String defaultCron, String methodName) {
            this.taskName = taskName;
            this.description = description;
            this.defaultCron = defaultCron;
            this.methodName = methodName;
        }
    }

    /**
     * 任务注册信息
     */
    private static class TaskRegistration {
        private String taskName;
        private String description;
        private String defaultCron;
        private String currentCron;
        private boolean enabled;
        private ScheduledFuture<?> scheduledFuture;
        private Object bean;
        private Method method;

        // Getters and setters
        public String getTaskName() { return taskName; }
        public void setTaskName(String taskName) { this.taskName = taskName; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getDefaultCron() { return defaultCron; }
        public void setDefaultCron(String defaultCron) { this.defaultCron = defaultCron; }
        public String getCurrentCron() { return currentCron; }
        public void setCurrentCron(String currentCron) { this.currentCron = currentCron; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public ScheduledFuture<?> getScheduledFuture() { return scheduledFuture; }
        public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) { this.scheduledFuture = scheduledFuture; }
        public Object getBean() { return bean; }
        public void setBean(Object bean) { this.bean = bean; }
        public Method getMethod() { return method; }
        public void setMethod(Method method) { this.method = method; }
    }
}
