package fan.summer.hmoneta.service.unifi;

import fan.summer.hmoneta.database.entity.unifi.UnifiSettingEntity;
import fan.summer.hmoneta.database.repository.unifi.UnifiSettingRepository;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.SiteManagerHostResp;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteInfoLocal;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteManagerLocalResp;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.client.ClientsLocalInfo;
import fan.summer.hmoneta.util.WebApiUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * UniFi Network API 服务类
 * <p>
 * 提供 UniFi 网络设备和客户端信息的管理功能，支持以下操作：
 * <ul>
 *   <li>UniFi 设置信息管理：查询和保存 API 连接配置</li>
 *   <li>连接状态检查：验证 UniFi API 是否可访问</li>
 *   <li>Site Manager API：查询 UniFi 主机列表</li>
 *   <li>Network API：查询本地站点列表和连接客户端</li>
 * </ul>
 *
 * <p>该服务支持两种 API 授权方式：
 * <ul>
 *   <li>远程授权：通过 baseUri 和 apiKey 调用远程 UniFi Host API</li>
 *   <li>本地授权：通过 localBaseUri 和 localApiKey 调用本地 UniFi 设备 API（支持 SSL 证书跳过验证）</li>
 * </ul>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/16
 */
@Service
public class UnifiApiService {
    private final UnifiSettingRepository unifiSettingRepository;
    private WebApiUtil webApiUtil;
    private WebApiUtil localWebApiUtil;

    public UnifiApiService(UnifiSettingRepository unifiSettingRepository) {
        this.unifiSettingRepository = unifiSettingRepository;
    }

    /**
     * 检查 UniFi API 是否可访问
     * <p>
     * 通过查询配置信息并尝试调用本地站点列表接口来验证 UniFi API 的连通性。
     * </p>
     *
     * @return true 表示 UniFi API 可访问，false 表示不可访问或未配置
     */
    public boolean unifiIsAccessible() {
        UnifiSettingEntity unifiSettingEntity = queryUnifiSettingInfo();
        if (unifiSettingEntity == null) {
            return false;
        } else {
            try {
                SiteManagerLocalResp<SiteInfoLocal> siteInfoLocalSiteManagerLocalResp = listLocalSites();
                return siteInfoLocalSiteManagerLocalResp != null;
            } catch (Exception e) {
                return false;
            }
        }
    }

    /**
     * 查询 UniFi 设置信息
     * <p>
     * 从数据库中获取已保存的 UniFi API 连接配置。
     * </p>
     *
     * @return UnifiSettingEntity 配置实体，若未配置则返回 null
     */
    public UnifiSettingEntity queryUnifiSettingInfo() {
        return unifiSettingRepository.findAll().stream().findFirst().orElse(null);
    }

    /**
     * 保存/更新 UniFi 设置信息
     * <p>
     * 如果已存在配置则先删除旧配置，然后保存新的配置信息。
     * 该操作会在事务中执行。
     * </p>
     *
     * @param unifiSettingEntity UniFi 配置实体，包含 baseUri、apiKey、localBaseUri、localApiKey 等信息
     */
    @Transactional
    public void settingUnifiSettingInfo(UnifiSettingEntity unifiSettingEntity) {
        if (unifiSettingRepository.count() > 0) {
            unifiSettingRepository.deleteAll();
        }
        unifiSettingRepository.save(unifiSettingEntity);
    }


    /**
     * 获取 UniFi 主机列表（Site Manager API）
     * <p>
     * 通过远程 API 调用获取 UniFi Host 列表。
     * 使用 baseUri 和 apiKey 进行远程授权认证。
     * </p>
     *
     * @return SiteManagerHostResp 主机列表响应，若配置无效则返回 null
     */
    public SiteManagerHostResp siteListHost() {
        initWebApiUtil(false);
        if (webApiUtil == null) {
            return null;
        }
        Mono<SiteManagerHostResp> api = webApiUtil.api(HttpMethod.GET, "/v1/hosts", null, null, SiteManagerHostResp.class);
        return api.block();
    }

    // Network Api
    // Sites

    /**
     * 获取本地站点列表（Network API）
     * <p>
     * 通过本地 API 调用获取 UniFi Network 站点列表。
     * 使用 localBaseUri 和 localApiKey 进行本地授权认证，
     * 并跳过 SSL 证书验证（适用于本地自签名证书场景）。
     * </p>
     *
     * @return SiteManagerLocalResp 站点列表响应，若配置无效则返回 null
     */
    public SiteManagerLocalResp<SiteInfoLocal> listLocalSites() {
        initWebApiUtil(true);
        Mono<SiteManagerLocalResp<SiteInfoLocal>> api = localWebApiUtil.api(
                HttpMethod.GET,
                "proxy/network/integration/v1/sites",
                null,
                null,
                new ParameterizedTypeReference<SiteManagerLocalResp<SiteInfoLocal>>() {
                });
        return api.block();
    }

    // Clients

    /**
     * 获取指定站点的连接客户端列表（Network API）
     * <p>
     * 通过本地 API 调用获取指定站点下已连接的客户端设备列表。
     * </p>
     *
     * @param siteId 站点 ID，指定要查询的 UniFi 网络站点
     * @return SiteManagerLocalResp 客户端列表响应，若配置无效则返回 null
     */
    public SiteManagerLocalResp<ClientsLocalInfo> listConnectedClients(String siteId) {
        initWebApiUtil(true);
        Mono<SiteManagerLocalResp<ClientsLocalInfo>> api = localWebApiUtil.api(HttpMethod.GET, "/proxy/network/integration/v1/sites/" + siteId + "/clients", null, null, new ParameterizedTypeReference<>() {
        });
        return api.block();
    }


    /**
     * 初始化 WebApiUtil 实例
     * <p>
     * 根据配置信息构建远程或本地的 WebApiUtil 实例。
     * 本地模式会跳过 SSL 证书验证，适用于自签名证书场景。
     * </p>
     *
     * @param isLocal true 表示本地模式（跳过 SSL 验证），false 表示远程模式
     */
    private void initWebApiUtil(boolean isLocal) {
        UnifiSettingEntity unifiSettingEntity = unifiSettingRepository.findAll().stream().findFirst().orElse(null);
        if (isLocal) {
            if (unifiSettingEntity != null) {
                this.localWebApiUtil = WebApiUtil.buildIgnoreSsl(unifiSettingEntity.getLocalBaseUri(), Map.of("Accept", "application/json", "X-API-Key", unifiSettingEntity.getLocalApiKey()));
            } else {
                this.localWebApiUtil = null;
            }
        } else {
            if (unifiSettingEntity != null) {
                this.webApiUtil = WebApiUtil.build(unifiSettingEntity.getBaseUri(), Map.of("Accept", "application/json", "X-API-Key", unifiSettingEntity.getApiKey()));
            } else {
                this.webApiUtil = null;
            }
        }
    }
}
