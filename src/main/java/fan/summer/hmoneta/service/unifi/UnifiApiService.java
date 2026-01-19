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
 * 类的详细说明
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

    // Unifi设置信息
    public UnifiSettingEntity queryUnifiSettingInfo() {
        return unifiSettingRepository.findAll().stream().findFirst().orElse(null);
    }

    @Transactional
    public void settingUnifiSettingInfo(UnifiSettingEntity unifiSettingEntity) {
        if (unifiSettingRepository.count() > 0) {
            unifiSettingRepository.deleteAll();
        }
        unifiSettingRepository.save(unifiSettingEntity);
    }


    // Site Manager Api

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

    public SiteManagerLocalResp<ClientsLocalInfo> listConnectedClients(String siteId) {
        initWebApiUtil(true);
        Mono<SiteManagerLocalResp<ClientsLocalInfo>> api = localWebApiUtil.api(HttpMethod.GET, "/proxy/network/integration/v1/sites/" + siteId + "/clients", null, null, new ParameterizedTypeReference<>() {
        });
        return api.block();
    }


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
