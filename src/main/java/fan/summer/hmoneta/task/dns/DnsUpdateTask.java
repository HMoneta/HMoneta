package fan.summer.hmoneta.task.dns;

import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.service.dns.DnsService;
import fan.summer.hmoneta.util.IpUtil;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/1
 */
@Component
@Log4j2
public class DnsUpdateTask {

    private final DnsService dnsService;
    private final DnsResolveUrlRepository dnsResolveUrlRepository;

    private String latestIp;

    public DnsUpdateTask(DnsService dnsService, DnsResolveUrlRepository dnsResolveUrlRepository) {
        this.dnsService = dnsService;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;

        this.latestIp = null;
    }

    @Scheduled(fixedRate = 600000)
    public void updater() {
        log.info("===============开始DDNS定时任务===============");
        log.info("-开始查询公网IP");
        String publicIp = IpUtil.getPublicIp();
        log.info("-开始比对公网IP是否变化");
        try {
            if (!publicIp.equals(latestIp)) {
                log.info("-公网IP发生变更，开始更新DNS解析");
                dnsResolveUrlRepository.findAll().forEach(dnsResolveUrl -> {
                    log.info("开始为{}更新DNS解析", dnsResolveUrl.getUrl());
                    dnsService.updateDnsResolveUrl(dnsResolveUrl, publicIp);
                    log.info("DNS解析更新完成");
                });

            }else {
                log.info("-公网IP未发生变更，无需更新DNS解析");
            }
            this.latestIp = publicIp;
        } catch (Exception e) {
            log.error(e.getMessage());
        }finally {
            log.info("===============结束DDNS定时任务===============");
        }
    }
}
