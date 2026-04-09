package fan.summer.hmoneta.service.waf;

import fan.summer.hmoneta.controller.waf.dto.LeiChiSettingDto;
import fan.summer.hmoneta.database.entity.waf.LeiChiSettingEntity;
import fan.summer.hmoneta.database.repository.waf.LeiChiRepository;
import fan.summer.hmoneta.service.waf.dto.LeiChiWafApiResp;
import fan.summer.hmoneta.service.waf.dto.site.LeiChiSiteListResp;
import fan.summer.hmoneta.service.waf.dto.ssl.LeiChiSslInfoResp;
import fan.summer.hmoneta.util.WebApiUtil;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 雷池 WAF 服务
 * <p>
 * 提供雷池 WAF 系统的核心业务逻辑，包括：
 * <ul>
 *   <li>API 设置管理（令牌、基础URL配置）</li>
 *   <li>WebClient 初始化和连接管理</li>
 *   <li>SSL 证书查询功能</li>
 *   <li>站点查询功能</li>
 * </ul>
 * </p>
 * <p>
 * 该服务使用懒加载模式初始化 WebClient，仅在首次调用 API 时创建连接。
 * 支持单例模式，确保整个应用中只有一个 WebClient 实例。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/25
 */
@Service
@Log4j2
public class LeiChiWafService {

    /**
     * WebClient 是否已初始化标志
     * -- GETTER --
     *  检查 WebClient 是否已初始化
     *  <p>
     *  用于在调用 API 前检查连接状态，避免因未初始化导致的异常。
     *  </p>
     *
     * @return true 表示已初始化可以调用 API，false 表示未配置或初始化失败

     */
    @Getter
    private boolean isWebApiUtilInit = false;

    /**
     * 雷池 API WebClient 实例
     */
    private WebApiUtil webApiUtil;

    /**
     * 雷池设置数据存储库
     */
    private final LeiChiRepository leiChiRepository;

    /**
     * 雷池 SSL 证书服务
     */
    private final LeiChiWafSslService leiChiWafSslService;

    /**
     * 雷池站点服务
     */
    private final LeiChiWafSiteService leiChiWafSiteService;

    /**
     * 构造方法
     *
     * @param leiChiRepository    雷池设置存储库
     * @param leiChiWafSslService 雷池 SSL 证书服务
     * @param leiChiWafSiteService 雷池站点服务
     */
    public LeiChiWafService(LeiChiRepository leiChiRepository, LeiChiWafSslService leiChiWafSslService,
                            LeiChiWafSiteService leiChiWafSiteService) {
        this.leiChiRepository = leiChiRepository;
        this.leiChiWafSslService = leiChiWafSslService;
        this.leiChiWafSiteService = leiChiWafSiteService;
    }

    /**
     * 查询雷池 WAF API 令牌
     * <p>
     * 从数据库中获取已保存的雷池 API 令牌信息。
     * </p>
     *
     * @return 雷池 API 令牌字符串，如果未配置则返回 null
     */
    public LeiChiSettingDto queryLeiChiSetting() {
        Optional<LeiChiSettingEntity> first = leiChiRepository.findAll().stream().findFirst();
        if (first.isPresent()) {
            LeiChiSettingEntity leiChiSettingEntity = first.get();
            LeiChiSettingDto dto = new LeiChiSettingDto();
            dto.setToken(leiChiSettingEntity.getToken());
            dto.setBaseUrl(leiChiSettingEntity.getBaseUrl());
            return dto;
        } else {
            return null;
        }
    }

    /**
     * 保存雷池 WAF API 设置
     * <p>
     * 将雷池 API 设置保存到数据库，系统自动维护唯一性：
     * <ul>
     *   <li>如果已存在设置，会先删除旧设置再保存新设置</li>
     *   <li>如果不存在设置，直接保存新设置</li>
     * </ul>
     * </p>
     *
     * @param leiChiSettingEntity 雷池设置实体，包含 baseUrl 和 token
     */
    public void insertLeiChiApiToken(LeiChiSettingEntity leiChiSettingEntity) {
        List<LeiChiSettingEntity> all = leiChiRepository.findAll();
        if (!all.isEmpty()) {
            leiChiRepository.deleteAll();
        }
        leiChiRepository.save(leiChiSettingEntity);
    }

    /**
     * 查询雷池 WAF SSL 证书列表
     * <p>
     * 调用雷池 WAF API 获取所有 SSL 证书信息，包括：
     * <ul>
     *   <li>证书 ID</li>
     *   <li>关联域名</li>
     *   <li>证书颁发者</li>
     *   <li>证书有效期</li>
     *   <li>证书状态（自签名、受信任、吊销、过期）</li>
     * </ul>
     * </p>
     * <p>
     * 注意：调用此方法前需确保已通过 {@link #insertLeiChiApiToken(LeiChiSettingEntity)} 配置 API 设置。
     * </p>
     *
     * @return SSL 证书信息响应对象，包含证书列表和总数
     */
    public LeiChiSslInfoResp listSslCert() {
        LeiChiWafApiResp<LeiChiSslInfoResp> result = leiChiWafSslService.listSslCert(webApiUtil);
        LeiChiSslInfoResp data = result.getData();
        return data;
    }

    public void modifySslCert(String crt, String key) {
        String s = leiChiWafSslService.modifySslCert(webApiUtil, crt, key);
    }

    /**
     * 查询雷池 WAF 站点列表
     * <p>
     * 调用雷池 WAF API 获取所有站点信息，包括：
     * <ul>
     *   <li>站点 ID</li>
     *   <li>站点名称/标题</li>
     *   <li>域名列表</li>
     *   <li>端口列表</li>
     *   <li>上游服务器地址</li>
     *   <li>证书信息</li>
     *   <li>运行模式</li>
     *   <li>启用状态</li>
     * </ul>
     * </p>
     *
     * @return 站点列表响应对象，包含站点列表和总数
     */
    public LeiChiSiteListResp listSite() {
        return leiChiWafSiteService.listSite(webApiUtil).getData();
    }





    /**
     * 初始化雷池 API WebClient
     * <p>
     * 从数据库读取雷池设置（baseUrl 和 token），构建 WebClient 实例。
     * 该方法采用懒加载模式，仅在首次调用 API 前执行初始化。
     * </p>
     * <p>
     * WebClient 配置：
     * <ul>
     *   <li>忽略 SSL 证书验证（适用于本地雷池服务）</li>
     *   <li>设置请求头：Accept: application/json</li>
     *   <li>设置认证头：X-SLCE-API-TOKEN</li>
     * </ul>
     * </p>
     * <p>
     * 如果初始化失败（数据库无设置或连接异常），会记录错误日志并将标志位设为 false。
     * </p>
     */
    public void initWebApiUtil() {
        if (this.isWebApiUtilInit) {
            return;
        }
        try {
            LeiChiSettingEntity leiChiSettingEntity = leiChiRepository.findAll().stream().findFirst().orElse(null);
            if (leiChiSettingEntity == null || leiChiSettingEntity.getBaseUrl() == null || leiChiSettingEntity.getBaseUrl().isBlank()
                    || leiChiSettingEntity.getToken() == null || leiChiSettingEntity.getToken().isBlank()) {
                this.isWebApiUtilInit = false;
                log.error("雷池WAF初始化失败: baseUrl 或 token 未配置");
                return;
            }
            this.webApiUtil = WebApiUtil.buildIgnoreSsl(leiChiSettingEntity.getBaseUrl(),
                    Map.of("Accept", "application/json", "X-SLCE-API-TOKEN", leiChiSettingEntity.getToken()));
            this.isWebApiUtilInit = true;
        } catch (Exception e) {
            this.isWebApiUtilInit = false;
            log.error("雷池WAF初始化异常:{}", e.getMessage());
        }
    }

}

