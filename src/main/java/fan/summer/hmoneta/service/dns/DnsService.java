package fan.summer.hmoneta.service.dns;

import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/10/7
 */
@Service
public class DnsService {
    private final DnsProviderRepository dnsProviderRepository;

    @Autowired
    public DnsService(DnsProviderRepository dnsProviderRepository) {
        this.dnsProviderRepository = dnsProviderRepository;
    }

    public List<DnsProviderEntity> queryAllDnsProvider() {
        return dnsProviderRepository.findAll();
    }
}
