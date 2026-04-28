package fan.summer.hmoneta.task.dns;

import fan.summer.hmoneta.common.annotation.ScheduledTask;
import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.service.dns.DnsService;
import fan.summer.hmoneta.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DDNS 更新定时任务
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/1
 */
@Component
@ScheduledTask(name = "dnsUpdateTask", description = "DDNS 更新任务", defaultCron = "0 0/10 * * * ?", methodName = "updater")
public class DnsUpdateTask {

    private static final Logger log = LoggerFactory.getLogger(DnsUpdateTask.class);

    private final DnsService dnsService;
    private final DnsResolveUrlRepository dnsResolveUrlRepository;


    public DnsUpdateTask(DnsService dnsService, DnsResolveUrlRepository dnsResolveUrlRepository) {
        this.dnsService = dnsService;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;
    }

    public void updater() {
        log.info("==============================================");
        log.info("DDNS定时任务开始");

        // 获取公网IP
        log.info("[1/4] 正在获取公网IP地址...");
        String publicIp = IpUtil.getPublicIp();

        if (publicIp == null || publicIp.isEmpty()) {
            log.error("[!] 获取公网IP失败，跳过本次DDNS任务");
            return;
        }
        log.info("[OK] 当前公网IP: {}", publicIp);

        // 查询DNS记录
        log.info("[2/4] 正在查询DNS解析记录...");
        List<DnsResolveUrlEntity> allUrls = dnsResolveUrlRepository.findAll();

        if (allUrls.isEmpty()) {
            log.info("[OK] 未找到任何DNS解析记录，任务结束");
            return;
        }
        log.info("[OK] 共找到 {} 条DNS解析记录", allUrls.size());

        // 检查并更新IP
        log.info("[3/4] 开始检查IP变化...");
        int updatedCount = 0;
        int skippedCount = 0;

        for (DnsResolveUrlEntity dnsResolveUrl : allUrls) {
            String storedIp = dnsResolveUrl.getIpAddress();
            String domain = dnsResolveUrl.getUrl();

            log.info("  └── 检查域名: {}", domain);
            log.info("      ├─ 数据库存储IP: {}", storedIp != null ? storedIp : "(空)");
            log.info("      ├─ 当前公网IP: {}", publicIp);
            log.info("      └─ 对比结果: ", storedIp);

            boolean ipChanged = (storedIp == null) || (!publicIp.equals(storedIp));

            if (!ipChanged) {
                log.info("      └── [跳过] IP未变化，无需更新");
                skippedCount++;
                continue;
            }

            log.info("      └── [更新] IP已变化，开始更新DNS解析...");
            try {
                dnsService.updateDnsResolveUrl(dnsResolveUrl, publicIp);
                log.info("      └── [成功] 域名 {} DNS更新完成", domain);
                updatedCount++;
            } catch (Exception e) {
                log.error("      └── [失败] 域名 {} 更新异常: {}", domain, e.getMessage());
            }
        }

        // 任务总结
        log.info("[4/4] DDNS任务执行完成");
        log.info("  ├── 总记录数: {}", allUrls.size());
        log.info("  ├── 更新数量: {}", updatedCount);
        log.info("  └── 跳过数量: {}", skippedCount);
        log.info("==============================================");
    }
}
