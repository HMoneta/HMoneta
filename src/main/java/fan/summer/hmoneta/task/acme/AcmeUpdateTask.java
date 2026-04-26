package fan.summer.hmoneta.task.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeCertificationRepository;
import fan.summer.hmoneta.service.acme.AcmeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * ACME证书自动续期定时任务
 * 检查所有证书并在过期前自动触发续期流程
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/26
 */
@Slf4j
@Component
@Profile("!dev")
public class AcmeUpdateTask {

    private final AcmeCertificationRepository acmeCertificationRepository;
    private final AcmeService acmeService;

    @Value("${acme.renewal.threshold-days:30}")
    private int renewalThresholdDays;

    public AcmeUpdateTask(AcmeCertificationRepository acmeCertificationRepository, AcmeService acmeService) {
        this.acmeCertificationRepository = acmeCertificationRepository;
        this.acmeService = acmeService;
    }

    @Scheduled(fixedRate = 86400000)
    protected void acmeUpdater() {
        String taskId = UUID.randomUUID().toString();
        Date today = new Date();
        List<AcmeCertificationEntity> allAcmeInfos = acmeCertificationRepository.findAll();
        log.info("[ACME-Updater:{}] 开始检查证书更新任务, 共 {} 条证书记录", taskId, allAcmeInfos.size());

        if (allAcmeInfos.isEmpty()) {
            log.debug("[ACME-Updater:{}] 未找到任何 ACME 证书记录", taskId);
            return;
        }

        for (AcmeCertificationEntity acmeInfo : allAcmeInfos) {
            String domain = acmeInfo.getDomain();
            Date notAfter = acmeInfo.getNotAfter();

            if (notAfter == null) {
                log.warn("[ACME-Updater:{}] 域名 {} 证书过期时间为空，跳过检查", taskId, domain);
                continue;
            }

            long daysUntilExpiry = ChronoUnit.DAYS.between(
                    today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                    notAfter.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            );

            log.debug("[ACME-Updater:{}] 检查域名: {}, 过期日期: {}, 剩余天数: {}",
                    taskId, domain, notAfter, daysUntilExpiry);

            if (daysUntilExpiry <= renewalThresholdDays) {
                log.info("[ACME-Updater:{}] 证书即将过期, 域名: {}, 剩余天数: {}, 开始重新申请证书",
                        taskId, domain, daysUntilExpiry);
                acmeService.renewCertification(domain, taskId);
            } else {
                log.info("[ACME-Updater:{}] 证书状态正常, 域名: {}, 剩余天数: {}",
                        taskId, domain, daysUntilExpiry);
            }
        }

        log.info("[ACME-Updater:{}] 证书更新检查任务完成", taskId);
    }
}
