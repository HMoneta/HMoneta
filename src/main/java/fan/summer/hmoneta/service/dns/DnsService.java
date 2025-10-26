package fan.summer.hmoneta.service.dns;

import fan.summer.hmoneta.common.enums.exception.dns.DnsExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.dns.entity.req.DnsResolveReq;
import fan.summer.hmoneta.controller.dns.entity.resp.DnsResolveResp;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveGroupRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.util.ObjectUtil;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final DnsResolveUrlRepository dnsResolveUrlRepository;

    @Autowired
    public DnsService(DnsProviderRepository dnsProviderRepository,
                      DnsResolveGroupRepository dnsResolveGroupRepository,
                      DnsResolveUrlRepository dnsResolveUrlRepository) {
        this.dnsProviderRepository = dnsProviderRepository;
        this.dnsResolveGroupRepository = dnsResolveGroupRepository;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;
    }

    public List<DnsProviderEntity> queryAllDnsProvider() {
        return dnsProviderRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertDnsResolveGroup(DnsResolveReq req) {
        if (ObjectUtil.isEmpty(req)) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_EMPTY_ERROR);
        }
        // 验证Req必要信息
        if (ObjectUtil.isEmpty(req.getAuthId())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_AUTH_EMPTY_ERROR);
        }
        if (ObjectUtil.isEmpty(req.getAuthKey())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_AUTH_EMPTY_ERROR);
        }
        if (ObjectUtil.isEmpty(req.getGroupName())) {
            req.setGroupName(String.valueOf(req.getProviderId()) + System.currentTimeMillis());
        }
        DnsResolveGroupEntity entity = new DnsResolveGroupEntity();

        // 创建DNS解析组数据库实体
        entity.setId(UUID.randomUUID().toString());
        entity.setGroupName(req.getGroupName());
        entity.setAuthId(req.getAuthId());
        entity.setAuthKey(req.getAuthKey());
        entity.setProviderId(req.getProviderId());
        // 创建该分组下需解析的urls
        List<DnsResolveUrlEntity> urls = new ArrayList<>();
        if (ObjectUtil.isNotEmpty(req.getUrls())) {
            req.getUrls().forEach(url -> {
                if (url.startsWith("http:") || url.startsWith("https:")) {
                    throw new HMException(DnsExceptionEnum.DNS_GROUP_URL_WITH_PROTOCOL_ERROR);
                }
                String[] schemes = {"http", "https"};
                UrlValidator urlValidator = new UrlValidator(schemes);
                boolean valid = urlValidator.isValid("https://" + url);
                if (valid) {
                    DnsResolveUrlEntity urlEntity = new DnsResolveUrlEntity();
                    urlEntity.setUrl(url);
                    urlEntity.setGroupId(entity.getId());
                    urlEntity.setId(UUID.randomUUID().toString());
                    urlEntity.setResolveStatus(0);
                    urlEntity.setCreateTime(LocalDateTime.now());
                    urls.add(urlEntity);
                }
            });
        }
        dnsResolveGroupRepository.save(entity);
        if (ObjectUtil.isNotEmpty(urls)) {
            dnsResolveUrlRepository.saveAll(urls);
        }
    }


    public List<DnsResolveResp> queryAllDnsResolve() {
        List<DnsResolveResp> resolveResps = new ArrayList<>();
        List<DnsResolveGroupEntity> allGroup = dnsResolveGroupRepository.findAll();
        if (ObjectUtil.isNotEmpty(allGroup)) {
            allGroup.forEach(group -> {
                List<DnsResolveUrlEntity> allUrl = dnsResolveUrlRepository.findAllByGroupId(group.getId());
                DnsResolveResp resp = new DnsResolveResp();
                resp.setGroupId(group.getId());
                resp.setUrls(allUrl);
                resp.setGroupName(group.getGroupName());
                resolveResps.add(resp);
            });
        }

        return resolveResps;
    }

    public void deleteDnsResolveUrl(String urlId) {
        dnsResolveUrlRepository.deleteById(urlId);
    }
}
