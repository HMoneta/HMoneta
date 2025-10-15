package fan.summer.hmoneta.service.dns;

import fan.summer.hmoneta.common.enums.exception.dns.DnsExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import fan.summer.hmoneta.database.repository.DnsResolveGroupRepository;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.util.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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
    private final DnsResolveGroupRepository dnsResolveGroupRepository;

    @Autowired
    public DnsService(DnsProviderRepository dnsProviderRepository, DnsResolveGroupRepository dnsResolveGroupRepository) {
        this.dnsProviderRepository = dnsProviderRepository;
        this.dnsResolveGroupRepository = dnsResolveGroupRepository;
    }

    public List<DnsProviderEntity> queryAllDnsProvider() {
        return dnsProviderRepository.findAll();
    }

    public void insertDnsResolveGroup(DnsResolveGroupEntity entity) {
        if (ObjectUtil.isEmpty(entity)) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_EMPTY_ERROR);
        }
        // 验证entity必要信息
        if (ObjectUtil.isEmpty(entity.getAuthId())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_AUTH_EMPTY_ERROR);
        }
        if (ObjectUtil.isEmpty(entity.getAuthKey())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_AUTH_EMPTY_ERROR);
        }
        entity.setId(UUID.randomUUID().toString());
        dnsResolveGroupRepository.save(entity);
    }
}
