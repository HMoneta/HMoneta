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
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DNS 解析服务核心类
 * <p>
 * 负责 DNS 解析组的全生命周期管理，包括：
 * <ul>
 *   <li>DNS 提供商查询 - 管理系统中注册的各类 DNS 服务提供商</li>
 *   <li>解析组管理 - 创建、修改、删除域名解析分组，每组可绑定不同的 DNS 提供商</li>
 *   <li>URL 解析管理 - 管理分组下的待解析域名列表</li>
 *   <li>动态更新 - 通过插件系统调用各 DNS 提供商 API 实现记录的增删改</li>
 *   <li>证书关联 - 关联 ACME 证书信息，提供域名与证书的一站式查询</li>
 * </ul>
 * </p>
 *
 * <p><b>插件机制：</b> DNS 提供商通过 {@link HmDnsProviderPlugin} 插件接口实现，
 * 由 {@link fan.summer.hmoneta.service.plugin.PluginService} 负责插件加载与调用。</p>
 *
 * <p><b>事务管理：</b> 所有涉及数据库写操作的方法均标注 {@linkTransactional}，
 * 确保数据一致性。</p>
 *
 * @see HmDnsProviderPlugin
 * @see fan.summer.hmoneta.service.plugin.PluginService
 * @see fan.summer.hmoneta.service.acme.AcmeService
 * @author phoebej
 * @version 1.00
 * @date 2025/10/7
 */
@Service
public class DnsService {
    /** 日志记录器 */
    private final static Logger log = LoggerFactory.getLogger(DnsService.class);

    /** DNS 提供商数据仓库 */
    private final DnsProviderRepository dnsProviderRepository;

    /** DNS 解析分组数据仓库 */
    private final DnsResolveGroupRepository dnsResolveGroupRepository;

    /** DNS 解析 URL 数据仓库 */
    private final DnsResolveUrlRepository dnsResolveUrlRepository;

    /** 插件服务，用于加载和管理 DNS 提供商插件 */
    private final PluginService pluginService;

    /** ACME 证书服务，用于关联域名与证书信息 */
    private final AcmeService acmeService;

    public DnsService(DnsProviderRepository dnsProviderRepository, DnsResolveGroupRepository dnsResolveGroupRepository, DnsResolveUrlRepository dnsResolveUrlRepository, PluginService pluginService, AcmeService acmeService) {
        this.dnsProviderRepository = dnsProviderRepository;
        this.dnsResolveGroupRepository = dnsResolveGroupRepository;
        this.dnsResolveUrlRepository = dnsResolveUrlRepository;
        this.pluginService = pluginService;
        this.acmeService = acmeService;
    }

    /**
     * 查询所有已注册的 DNS 提供商
     *
     * @return DNS 提供商实体列表
     */
    public List<DnsProviderEntity> queryAllDnsProvider() {
        return dnsProviderRepository.findAll();
    }

    /**
     * 新增 DNS 解析分组
     * <p>
     * 验证请求参数的完整性，自动生成无名称分组的默认名称，
     * 对 URL 列表进行格式校验（不允许携带协议头），
     * 校验通过后保存分组及关联的 URL 列表。
     * </p>
     *
     * @param req DNS 解析请求实体，包含分组名称、提供商ID、认证信息及 URL 列表
     * @throws HMException 当请求为空、认证信息为空或 URL 格式错误时抛出
     */
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

    /**
     * 修改 DNS 解析分组
     * <p>
     * 根据请求中的 isDelete 标志判断操作类型：
     * <ul>
     *   <li>isDelete=true：删除整个分组及其关联的所有 URL 记录</li>
     *   <li>isDelete=false：仅更新分组的名称和认证信息</li>
     * </ul>
     * </p>
     *
     * @param req 分组修改请求实体，包含分组ID、名称、认证信息及删除标志
     * @throws HMException 当必填参数为空时抛出
     * @see #deleteDnsResolveGroup(GroupModifyReq)
     */
    @Transactional(rollbackFor = Exception.class)
    public void modifyDnsResolveGroup(GroupModifyReq req) {
        if (ObjectUtil.isEmpty(req.getId()) || ObjectUtil.isEmpty(req.getAuthenticateWayMap()) || ObjectUtil.isEmpty(req.getGroupName()) || ObjectUtil.isEmpty(req.getIsDelete())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_MODIFY_INFO_EMPTY);
        }
        String id = req.getId();
        if (req.getIsDelete()) {
            deleteDnsResolveGroup(req);
        } else {
            dnsResolveGroupRepository.findById(id).ifPresent(dnsResolveGroupEntity -> {
                dnsResolveGroupEntity.setGroupName(req.getGroupName());
                dnsResolveGroupEntity.setCredentials(req.getAuthenticateWayMap());
                dnsResolveGroupRepository.save(dnsResolveGroupEntity);
            });
        }

    }

    /**
     * 删除 DNS 解析分组
     * <p>
     * 级联删除分组下的所有 URL 记录，并调用 DNS 提供商插件删除对应的解析记录。
     * 此方法通过 AOP 代理调用以确保事务生效。
     * </p>
     *
     * @param req 分组修改请求实体，包含要删除的分组ID
     * @throws HMException 当分组不存在时抛出
     * @see #deleteDnsResolveUrl(String)
     */
    @Transactional
    protected void deleteDnsResolveGroup(GroupModifyReq req) {
        if (ObjectUtil.isEmpty(req) || ObjectUtil.isEmpty(req.getId())) {
            throw new HMException(DnsExceptionEnum.DNS_GROUP_MODIFY_INFO_EMPTY);
        } else {
            // 查询分组是否有已经解析的网址
            String groupId = req.getId();
            dnsResolveGroupRepository.findById(groupId).ifPresentOrElse(dnsResolveGroupEntity -> {
                        List<DnsResolveUrlEntity> allByGroupId = dnsResolveUrlRepository.findAllByGroupId(groupId);
                        if (ObjectUtil.isNotEmpty(allByGroupId)) {
                            for (DnsResolveUrlEntity dnsResolveUrlEntity : allByGroupId) {
                                ((DnsService) AopContext.currentProxy()).deleteDnsResolveUrl(dnsResolveUrlEntity.getId());
                            }
                            dnsResolveUrlRepository.deleteAll(allByGroupId);
                        }
                        dnsResolveGroupRepository.deleteById(dnsResolveGroupEntity.getId());
                    },
                    () -> {
                        throw new HMException(DnsExceptionEnum.DNS_GROUP_NOT_FOUND_EMPTY);
                    }
            );
        }

    }


    /**
     * 查询所有 DNS 解析记录及其关联的证书信息
     * <p>
     * 遍历所有分组及其下的 URL，为每个 URL 关联对应的 ACME 证书信息，
     * 包括证书申请时间、有效期起止时间。
     * </p>
     *
     * @return DNS 解析响应列表，包含分组信息、URL 列表及各自关联的证书信息
     * @see fan.summer.hmoneta.service.acme.AcmeService#queryCertificationInfo(String)
     */
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

    /**
     * 删除指定的 DNS 解析 URL 记录
     * <p>
     * 根据 URL ID 查询记录信息，获取关联的分组和提供商，
     * 调用 DNS 提供商插件删除对应的 DNS A 记录，
     * 删除成功后再从数据库中移除记录。
     * </p>
     *
     * @param urlId 要删除的 DNS 解析 URL ID
     * @throws HMException 当 URL 不存在或 DNS 记录删除失败时抛出
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteDnsResolveUrl(String urlId) {
        dnsResolveUrlRepository.findById(urlId).ifPresentOrElse(dnsResolveUrlEntity -> {
            String groupId = dnsResolveUrlEntity.getGroupId();
            DnsResolveGroupEntity dnsResolveGroupEntity = dnsResolveGroupRepository.findById(groupId).get();
            String providerId = dnsResolveGroupEntity.getProviderId();
            DnsProviderEntity dnsProviderEntity = dnsProviderRepository.findById(providerId).get();
            HmDnsProviderPlugin dnsProvider = pluginService.getDnsProvider(dnsProviderEntity.getProviderName());
            dnsProvider.authenticate(dnsResolveGroupEntity.getCredentials());
            Map<String, String> urlMap = WebUtil.extractParts(dnsResolveUrlEntity.getUrl());
            boolean b = dnsProvider.deleteDns(urlMap.get("host"), urlMap.get("sub"), "A");
            if (b) {
                dnsResolveUrlRepository.deleteById(urlId);
            } else {
                throw new HMException(DnsExceptionEnum.DNS_RECORD_DELETE_ERROR);
            }
        }, () -> {
            throw new HMException(DnsExceptionEnum.DNS_URL_NOT_EXISTS_ERROR);
        });


    }

    /**
     * 修改 DNS 解析 URL 记录
     * <p>
     * 根据实体 ID 判断操作类型：
     * <ul>
     *   <li>ID 为空：新增 URL 记录，需保证 URL 全局唯一</li>
     *   <li>ID 非空：更新现有 URL 的域名地址</li>
     * </ul>
     * 操作完成后自动触发 DNS 解析更新。
     * </p>
     *
     * @param entity DNS 解析 URL 实体，包含 URL 地址和分组ID
     * @throws HMException 当 URL 已存在或不存在时抛出
     * @see #updateDnsResolveUrl(DnsResolveUrlEntity, String)
     */
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

    /**
     * 更新 DNS 解析记录
     * <p>
     * 执行流程:
     * 1. 选择 DNS 供应商 - 根据分组ID获取对应的DNS提供商
     * 2. 初始化插件 - 加载并认证 DNS 提供商插件
     * 3. 解析域名 - 分离主域名和子域名 (如 www.example.com -> host:example.com, sub:www)
     * 4. 检查现有记录 - 查询 DNS 提供商当前生效的记录
     * 5. 对比/创建记录 - 如果记录存在且IP不同则更新，否则直接创建
     * 6. 保存结果 - 更新数据库中的解析状态和IP
     * </p>
     *
     * @param entity DNS解析记录实体, 包含域名、分组ID等信息
     * @param ip     目标IP地址
     */
    public void updateDnsResolveUrl(DnsResolveUrlEntity entity, String ip) {
        log.info("[DNS] ════════════════════════════════════════════");
        log.info("[DNS] 开始更新 DNS 解析");
        log.info("[DNS]   ├── 域名: {}", entity.getUrl());
        log.info("[DNS]   └── 目标IP: {}", ip);

        // Step 1: 根据分组ID获取分组信息
        String groupId = entity.getGroupId();
        log.info("[DNS]   ├── 分组ID: {}", groupId);

        dnsResolveGroupRepository.findById(groupId).ifPresentOrElse(group -> {
            // Step 2: 根据提供商ID获取提供商信息
            String providerId = group.getProviderId();
            log.info("[DNS]   ├── 提供商ID: {}", providerId);

            dnsProviderRepository.findById(providerId).ifPresentOrElse(provider -> {
                String providerName = provider.getProviderName();
                log.info("[DNS] ══ 步骤1: 选择DNS供应商");
                log.info("[DNS]   └── 供应商: {} → 开始DNS操作", providerName);

                // Step 3: 初始化 DNS 供应商插件
                log.info("[DNS] ══ 步骤2: 初始化DNS供应商插件");
                HmDnsProviderPlugin dnsProviderPlugin = pluginService.getDnsProvider(providerName);
                dnsProviderPlugin.authenticate(group.getCredentials());
                log.info("[DNS]   └── [OK] 插件初始化成功");

                // Step 4: 解析域名结构 - 分离主域名和子域名
                log.info("[DNS] ══ 步骤3: 解析域名结构");
                Map<String, String> urlMap = WebUtil.extractParts(entity.getUrl());
                String host = urlMap.get("host");   // 主域名, 如 example.com
                String sub = urlMap.get("sub");     // 子域名, 如 www 或 null(根域名)
                log.info("[DNS]   ├── 主域名(host): {}", host);
                log.info("[DNS]   └── 子域名(sub): {}", sub != null ? sub : "(根域名)");

                // Step 5: 检查 DNS 提供商上是否存在现有记录
                log.info("[DNS] ══ 步骤4: 检查现有DNS记录");
                List<DNSRecordInfo> dnsRecordInfos = dnsProviderPlugin.dnsCheck(host, sub);

                boolean dnsResult = false;

                if (ObjectUtil.isNotEmpty(dnsRecordInfos)) {
                    // 存在现有记录, 需要对比IP决定是否更新
                    log.info("[DNS]   └── [发现] 存在 {} 条DNS记录", dnsRecordInfos.size());
                    log.info("[DNS] ══ 步骤5: 对比现有记录与目标IP");

                    for (int i = 0; i < dnsRecordInfos.size(); i++) {
                        DNSRecordInfo record = dnsRecordInfos.get(i);
                        String currentValue = record.getvalue();
                        boolean needsUpdate = !ip.equals(currentValue);

                        log.info("[DNS]   ├── 记录#{}/{}:", i + 1, dnsRecordInfos.size());
                        log.info("[DNS]   │   ├── 当前IP: {}", currentValue);
                        log.info("[DNS]   │   ├── 目标IP: {}", ip);
                        log.info("[DNS]   │   └── 状态: {}", needsUpdate ? "需更新" : "无需更新");

                        if (needsUpdate) {
                            // IP不一致, 调用DNS提供商更新记录
                            log.info("[DNS]   │   └── 执行DNS更新...");
                            dnsResult = dnsProviderPlugin.modifyDns(host, sub, "A", ip);
                            log.info("[DNS]   │   └── 更新结果: {}", dnsResult ? "成功" : "失败");
                        } else {
                            // IP一致, 跳过更新
                            log.info("[DNS]   │   └── [跳过] IP一致，跳过更新");
                            dnsResult = true;
                        }
                    }
                } else {
                    // 无现有记录, 直接创建新记录
                    log.info("[DNS]   └── [未发现] 未找到现有DNS记录");
                    log.info("[DNS] ══ 步骤5: 创建新DNS记录");
                    log.info("[DNS]   └── 执行DNS创建操作...");
                    dnsResult = dnsProviderPlugin.modifyDns(host, sub, "A", ip);
                    log.info("[DNS]   └── 创建结果: {}", dnsResult ? "成功" : "失败");
                }

                // Step 6: 根据DNS操作结果更新数据库记录
                log.info("[DNS] ══ 步骤6: 保存更新结果");
                if (dnsResult) {
                    // 成功: 设置状态为1, 更新IP和时间
                    log.info("[DNS]   └── [成功] DNS解析更新成功");
                    entity.setResolveStatus(1);
                    entity.setUpdateTime(LocalDateTime.now());
                    entity.setIpAddress(ip);
                } else {
                    // 失败: 设置状态为0, 清空IP
                    log.info("[DNS]   └── [失败] DNS解析更新失败");
                    entity.setResolveStatus(0);
                    entity.setUpdateTime(LocalDateTime.now());
                    entity.setIpAddress("");
                }
                dnsResolveUrlRepository.save(entity);
                log.info("[DNS]   └── [完成] 记录已保存到数据库");

                log.info("[DNS] ════════════════════════════════════════════");
            }, () -> {
                log.error("[DNS] [!] DNS供应商不存在, providerId: {}", providerId);
                throw new HMException(DnsExceptionEnum.DNS_PROVIDER_NOT_FOUND_EMPTY);
            });
        }, () -> {
            log.error("[DNS] [!] DNS分组不存在, groupId: {}", groupId);
            throw new HMException(DnsExceptionEnum.DNS_GROUP_NOT_FOUND_EMPTY);
        });
    }
}
