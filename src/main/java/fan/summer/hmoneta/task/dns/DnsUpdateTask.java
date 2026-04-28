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
        log.info("===============开始DDNS定时任务===============");
        log.info("-开始查询公网IP");
        String publicIp = IpUtil.getPublicIp();
        log.info("-当前公网IP: {}", publicIp);

        try {
            List<DnsResolveUrlEntity> allUrls = dnsResolveUrlRepository.findAll();
            if (allUrls.isEmpty()) {
                log.info("-未找到任何DNS解析记录");
                return;
            }

            log.info("-共 {} 条DNS解析记录，开始检查IP变化", allUrls.size());

            for (DnsResolveUrlEntity dnsResolveUrl : allUrls) {
                String storedIp = dnsResolveUrl.getIpAddress();
                log.info("-检查域名: {}, 当前解析IP: {}, 公网IP: {}",
                        dnsResolveUrl.getUrl(), storedIp, publicIp);

                if (publicIp.equals(storedIp)) {
                    log.info("-域名 {} IP未变化，跳过更新", dnsResolveUrl.getUrl());
                    continue;
                }

                log.info("-域名 {} IP发生变化，开始更新DNS解析", dnsResolveUrl.getUrl());
                dnsService.updateDnsResolveUrl(dnsResolveUrl, publicIp);
                log.info("-域名 {} DNS更新完成", dnsResolveUrl.getUrl());
            }

        } catch (Exception e) {
            log.error("DDNS任务执行异常: {}", e.getMessage(), e);
        } finally {
            log.info("===============结束DDNS定时任务===============");
        }
    }
}
