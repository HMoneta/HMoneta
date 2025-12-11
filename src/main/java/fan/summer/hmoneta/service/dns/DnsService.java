package fan.summer.hmoneta.service.dns;

import fan.summer.hmoneta.common.enums.exception.dns.DnsExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.acme.dto.resp.AcmeCerInfoResp;
import fan.summer.hmoneta.controller.dns.entity.req.DnsResolveReq;
import fan.summer.hmoneta.controller.dns.entity.req.GroupModifyReq;
import fan.summer.hmoneta.controller.dns.entity.resp.DnsResolveResp;
import fan.summer.hmoneta.controller.dns.entity.resp.DnsResolveUrlResp;
import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveGroupRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.plugin.api.dns.dto.DNSRecordInfo;
import fan.summer.hmoneta.service.acme.AcmeService;
import fan.summer.hmoneta.service.plugin.PluginService;
import fan.summer.hmoneta.util.IpUtil;
import fan.summer.hmoneta.util.ObjectUtil;
import fan.summer.hmoneta.util.WebUtil;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final static Logger log = LoggerFactory.getLogger(DnsService.class);
    private final DnsProviderRepository dnsProviderRepository;
    private final DnsResolveGroupRepository dnsResolveGroupRepository;
    private final DnsResolveUrlRepository dnsResolveUrlRepository;
    private final PluginService pluginService;
    private final AcmeService acmeService;

    public DnsService(DnsProviderRepository dnsProviderRepository, DnsResolveGroupRepository dnsResolveGroupRepository, DnsResolveUrlRepository dnsResolveUrlRepository, PluginService pluginService, AcmeService acmeService) {
        this.dnsProviderRepository = dnsProviderRepository;
        this.dnsResolveGroupRepository = dnsResolveGroupRepository;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;
        this.pluginService = pluginService;
        this.acmeService = acmeService;
    }

    public List<DnsProviderEntity> queryAllDnsProvider() {
        return dnsProviderRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public void insertDnsResolveGroup(DnsResolveReq req) {
        log.info("===============开始插入定时任务===============");
        if (ObjectUtil.isEmpty(req)) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_EMPTY_ERROR);
        }
        // 验证Req必要信息
        if (ObjectUtil.isEmpty(req.getAuthenticateWayMap())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_AUTH_EMPTY_ERROR);
        }
        if (ObjectUtil.isEmpty(req.getGroupName())) {
            req.setGroupName(String.valueOf(req.getProviderId()) + System.currentTimeMillis());
        }
        DnsResolveGroupEntity entity = new DnsResolveGroupEntity();

        // 创建DNS解析组数据库实体
        entity.setId(UUID.randomUUID().toString());
        entity.setGroupName(req.getGroupName());
        entity.setProviderId(req.getProviderId());
        entity.setCredentials(req.getAuthenticateWayMap());
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

    public void modifyDnsResolveGroup(GroupModifyReq req) {
        if (ObjectUtil.isEmpty(req.getId()) || ObjectUtil.isEmpty(req.getAuthenticateWayMap()) || ObjectUtil.isEmpty(req.getGroupName()) || ObjectUtil.isEmpty(req.getIsDelete())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_MODIFY_INFO_EMPTY);
        }
        String id = req.getId();
        if (req.getIsDelete()) {
            dnsResolveGroupRepository.findById(req.getId()).ifPresentOrElse(dnsResolveGroupEntity -> {
                // 先删除分组下所有解析
                List<DnsResolveUrlEntity> allByGroupId = dnsResolveUrlRepository.findAllByGroupId(dnsResolveGroupEntity.getId());
                if (ObjectUtil.isNotEmpty(allByGroupId)) {
                    dnsResolveUrlRepository.deleteAll(allByGroupId);
                }
                dnsResolveGroupRepository.deleteById(dnsResolveGroupEntity.getId());
            }, () -> {
                throw new HMException(DnsExceptionEnum.DNS_GROUP_NOT_FOUND_EMPTY);
            });
        } else {
            dnsResolveGroupRepository.findById(id).ifPresent(dnsResolveGroupEntity -> {
                dnsResolveGroupEntity.setGroupName(req.getGroupName());
                dnsResolveGroupEntity.setCredentials(req.getAuthenticateWayMap());
                dnsResolveGroupRepository.save(dnsResolveGroupEntity);
            });
        }

    }

    public void deleteDnsResolveGroup(GroupModifyReq req) {
        if (ObjectUtil.isEmpty(req) || ObjectUtil.isEmpty(req.getId())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_MODIFY_INFO_EMPTY);
        }

    }


    public List<DnsResolveResp> queryAllDnsResolve() {
        List<DnsResolveResp> resolveResps = new ArrayList<>();
        List<DnsResolveGroupEntity> allGroup = dnsResolveGroupRepository.findAll();
        if (ObjectUtil.isNotEmpty(allGroup)) {
            allGroup.forEach(group -> {
                List<DnsResolveUrlEntity> allUrl = dnsResolveUrlRepository.findAllByGroupId(group.getId());
                List<DnsResolveUrlResp> urlResps = new ArrayList<>();
                if (ObjectUtil.isNotEmpty(allUrl)) {
                    // 查询证书信息
                    allUrl.forEach(url -> {
                        DnsResolveUrlResp urlResp = new DnsResolveUrlResp();
                        BeanUtils.copyProperties(url, urlResp);
                        AcmeCertificationEntity acmeCertificationEntity = acmeService.queryCertificationInfo(urlResp.getUrl());
                        if (ObjectUtil.isNotEmpty(acmeCertificationEntity)) {
                            AcmeCerInfoResp acmeResp = new AcmeCerInfoResp(true, acmeCertificationEntity.getCertApplyTime(), acmeCertificationEntity.getNotBefore(), acmeCertificationEntity.getNotAfter());
                            urlResp.setAcmeCerInfo(acmeResp);
                        } else {
                            AcmeCerInfoResp acmeResp = new AcmeCerInfoResp(false, null, null, null);
                            urlResp.setAcmeCerInfo(acmeResp);
                        }
                        urlResps.add(urlResp);
                    });
                }
                DnsResolveResp resp = new DnsResolveResp();
                resp.setGroupId(group.getId());
                resp.setUrls(urlResps);
                resp.setGroupName(group.getGroupName());
                resp.setAuthenticateWayMap(group.getCredentials());
                resolveResps.add(resp);
            });
        }

        return resolveResps;
    }

    public void deleteDnsResolveUrl(String urlId) {
        dnsResolveUrlRepository.deleteById(urlId);
        
    }

    public void modifyDnsResolveUrl(DnsResolveUrlEntity entity) {
        if (ObjectUtil.isEmpty(entity.getId())) {
            // 新增url
            DnsResolveUrlEntity oneByUrl = dnsResolveUrlRepository.findOneByUrl(entity.getUrl());
            if (ObjectUtil.isEmpty(oneByUrl)) {
                entity.setId(UUID.randomUUID().toString());
                entity.setCreateTime(LocalDateTime.now());
                entity.setResolveStatus(0);
                dnsResolveUrlRepository.save(entity);
            } else {
                throw new HMException(DnsExceptionEnum.DNS_URL_EXISTS_ERROR);
            }
        } else {
            dnsResolveUrlRepository.findById(entity.getId()).ifPresentOrElse(dataBaseEntity -> {
                if (!entity.getUrl().equals(dataBaseEntity.getUrl())) {
                    dataBaseEntity.setUrl(entity.getUrl());
                    dataBaseEntity.setUpdateTime(LocalDateTime.now());
                    dnsResolveUrlRepository.save(dataBaseEntity);
                }
            }, () -> {
                throw new HMException(DnsExceptionEnum.DNS_URL_NOT_EXISTS_ERROR);
            });
        }
        updateDnsResolveUrl(entity, IpUtil.getPublicIp());
    }

    public void updateDnsResolveUrl(DnsResolveUrlEntity entity, String ip) {
        log.info(">>>>>>>>>>>>>开始为:{}更新DNS，更新地址为:{}", entity.getUrl(), ip);
        String groupId = entity.getGroupId();
        dnsResolveGroupRepository.findById(groupId).ifPresentOrElse(group -> {
            String providerId = group.getProviderId();
            dnsProviderRepository.findById(providerId).ifPresentOrElse(provider -> {
                boolean dnsResult = false;
                log.info(">>>>>>>>>>>>>开始使用:{}DNS供应商进行变更", provider.getProviderName());
                HmDnsProviderPlugin dnsProviderPlugin = pluginService.getDnsProvider(provider.getProviderName());
                dnsProviderPlugin.authenticate(group.getCredentials());
                Map<String, String> urlMap = WebUtil.extractParts(entity.getUrl());
                log.info(">>>>>>>>>>>>>检查是否存在DNS解析");
                List<DNSRecordInfo> dnsRecordInfos = dnsProviderPlugin.dnsCheck(urlMap.get("host"), urlMap.get("sub"));
                if (ObjectUtil.isNotEmpty(dnsRecordInfos)) {
                    log.info(">>>>>>>>>>>>>存在DNS解析，开始检测DNS解析是否一致");
                    for (DNSRecordInfo dnsRecordInfo : dnsRecordInfos) {
                        if (!dnsRecordInfo.getvalue().equals(ip)) {
                            log.info(">>>>>>>>>>>>>解析不一致需更新");
                            dnsResult = dnsProviderPlugin.modifyDns(urlMap.get("host"), urlMap.get("sub"), "A", ip);
                        } else {
                            log.info(">>>>>>>>>>>>>解析一致无需更新");
                            dnsResult = true;
                        }
                    }
                } else {
                    log.info(">>>>>>>>>>>>>不存在DNS解析，直接创建DNS解析");
                    dnsResult = dnsProviderPlugin.modifyDns(urlMap.get("host"), urlMap.get("sub"), "A", ip);
                }
                if (dnsResult) {
                    log.info(">>>>>>>>>>>>>DNS解析成功");
                    entity.setResolveStatus(1);
                    entity.setUpdateTime(LocalDateTime.now());
                    entity.setIpAddress(ip);
                    dnsResolveUrlRepository.save(entity);
                } else {
                    log.error(">>>>>>>>>>>>>DNS解析失败");
                    entity.setResolveStatus(0);
                    entity.setUpdateTime(LocalDateTime.now());
                    entity.setIpAddress("");
                    dnsResolveUrlRepository.save(entity);
                }
            }, () -> {
                throw new HMException(DnsExceptionEnum.DNS_PROVIDER_NOT_FOUND_EMPTY);
            });
        }, () -> {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_NOT_FOUND_EMPTY);
        });
    }
}
