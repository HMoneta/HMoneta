package fan.summer.hmoneta.task.dns;

import fan.summer.hmoneta.common.annotation.ScheduledTask;
import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.service.dns.DnsService;
import fan.summer.hmoneta.util.IpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
        log.info("-开始比对公网IP是否变化");
        try {
            log.info("-公网IP发生变更，开始更新DNS解析");
            dnsResolveUrlRepository.findAll().forEach(dnsResolveUrl -> {
                log.info("开始为{}更新DNS解析", dnsResolveUrl.getUrl());
                dnsService.updateDnsResolveUrl(dnsResolveUrl, publicIp);
                log.info("DNS解析更新完成");
            });


        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            log.info("===============结束DDNS定时任务===============");
        }
    }
}
