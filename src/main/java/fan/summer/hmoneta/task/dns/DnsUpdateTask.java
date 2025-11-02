package fan.summer.hmoneta.task.dns;

import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.service.dns.DnsService;
import fan.summer.hmoneta.util.IpUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
public class DnsUpdateTask {

    private static final Logger logger = LogManager.getLogger(DnsUpdateTask.class);
    private final DnsService dnsService;
    private final DnsResolveUrlRepository dnsResolveUrlRepository;

    private String latestIp;

    public DnsUpdateTask(DnsService dnsService, DnsResolveUrlRepository dnsResolveUrlRepository) {
        this.dnsService = dnsService;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;

        this.latestIp = null;
    }

    @Scheduled(fixedRate = 60000)
    public void updater() {
        String publicIp = IpUtil.getPublicIp();
        try {
            if (!publicIp.equals(latestIp)) {
                dnsResolveUrlRepository.findAll().forEach(dnsResolveUrl -> {
                    dnsService.updateDnsResolveUrl(dnsResolveUrl, publicIp);
                });
            }
            this.latestIp = publicIp;
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}
