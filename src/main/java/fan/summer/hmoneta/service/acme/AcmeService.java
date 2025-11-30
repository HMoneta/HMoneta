package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.common.enums.exception.acme.AcmeExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import fan.summer.hmoneta.database.repository.acme.AcmeUserInfoRepository;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveGroupRepository;
import fan.summer.hmoneta.database.repository.dns.DnsResolveUrlRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.service.plugin.PluginService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Acme证书申请服务
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/28
 */
@Service
@Log4j2
public class AcmeService {


    private final PluginService pluginService;


    private final AcmeUserInfoRepository acmeUserInfoRepository;

    private final AcmeServiceComponent acmeServiceComponent;

    private final DnsProviderRepository dnsProviderRepository;

    private final DnsResolveGroupRepository dnsResolveGroupRepository;

    private final DnsResolveUrlRepository dnsResolveUrlRepository;

    /**
     * 构造函数，初始化AcmeService所需的各种依赖
     *
     * @param pluginService               插件服务
     * @param acmeChallengeInfoRepository ACME用户信息仓库
     * @param acmeServiceComponent        ACME服务组件
     * @param dnsProviderRepository       DNS提供商仓库
     * @param dnsResolveGroupRepository   DNS解析分组仓库
     * @param dnsResolveUrlRepository     DNS解析URL仓库
     */
    public AcmeService(PluginService pluginService, AcmeUserInfoRepository acmeChallengeInfoRepository, AcmeServiceComponent acmeServiceComponent, DnsProviderRepository dnsProviderRepository,
                       DnsResolveGroupRepository dnsResolveGroupRepository,
                       DnsResolveUrlRepository dnsResolveUrlRepository) {
        this.pluginService = pluginService;
        this.acmeUserInfoRepository = acmeChallengeInfoRepository;
        this.acmeServiceComponent = acmeServiceComponent;
        this.dnsProviderRepository = dnsProviderRepository;
        this.dnsResolveGroupRepository = dnsResolveGroupRepository;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;
    }

    /**
     * 保存或更新ACME用户信息
     * 如果实体没有ID，则创建新实体；如果存在ID，则更新现有实体
     *
     * @param acmeUserInfoEntity ACME用户信息实体
     * @throws HMException 如果更新时找不到对应的实体，则抛出异常
     */
    public void insertAcmeUserInfo(AcmeUserInfoEntity acmeUserInfoEntity) {
        if (ObjectUtils.isEmpty(acmeUserInfoEntity.getId())) {
            // 新增
            acmeUserInfoEntity.setId(UUID.randomUUID().toString());
            acmeUserInfoRepository.save(acmeUserInfoEntity);
        } else {
            // 更新
            acmeUserInfoRepository.findById(acmeUserInfoEntity.getId()).ifPresentOrElse(item -> {
                item.setUserEmail(acmeUserInfoEntity.getUserEmail());
                acmeUserInfoRepository.save(item);
            }, () -> {
                throw new HMException(AcmeExceptionEnum.ACME_ACCOUNT_UPDATE_ERROR);
            });
        }
    }

    /**
     * 为指定域名申请SSL证书
     * 通过DNS挑战方式验证域名所有权并获取证书
     *
     * @param domain 需要申请证书的域名
     */
    public String applyCertification(String domain) {
        AcmeTaskContext acmeTaskContext = getAcmeTaskContext(domain);
        acmeServiceComponent.useDnsChallengeGetCertification(acmeTaskContext);
        return acmeTaskContext.getTaskId();
    }

    /**
     * 根据域名获取ACME任务上下文
     * 构建包含域名、用户信息和DNS提供商的任务上下文
     *
     * @param domain 需要申请证书的域名
     * @return ACME任务上下文对象
     */
    private AcmeTaskContext getAcmeTaskContext(String domain) {
        final AcmeTaskContext acmeTaskContext = new AcmeTaskContext();
        acmeTaskContext.setTaskId(UUID.randomUUID().toString());
        acmeTaskContext.setDomain(domain);
        AcmeUserInfoEntity orCreateAcmeUser = acmeServiceComponent.findOrCreateAcmeUser(domain);
        acmeTaskContext.setAcmeUserInfoEntity(orCreateAcmeUser);
        HmDnsProviderPlugin dnsProvider = getDnsProvider(domain);
        acmeTaskContext.setHmDnsProviderPlugin(dnsProvider);
        return acmeTaskContext;
    }

    /**
     * 根据域名获取对应的DNS提供商插件
     * 通过域名查找DNS解析URL，再获取分组和提供商信息，最终获取插件实例
     *
     * @param domain 需要申请证书的域名
     * @return DNS提供商插件实例
     */
    private HmDnsProviderPlugin getDnsProvider(String domain) {
        DnsResolveUrlEntity oneByUrl = dnsResolveUrlRepository.findOneByUrl(domain);
        DnsResolveGroupEntity dnsResolveGroupEntity = dnsResolveGroupRepository.findById(oneByUrl.getGroupId()).get();
        DnsProviderEntity dnsProviderEntity = dnsProviderRepository.findById(dnsResolveGroupEntity.getProviderId()).get();
        HmDnsProviderPlugin dnsProvider = pluginService.getDnsProvider(dnsProviderEntity.getProviderName());
        dnsProvider.authenticate(dnsResolveGroupEntity.getCredentials());
        return dnsProvider;
    }

}
