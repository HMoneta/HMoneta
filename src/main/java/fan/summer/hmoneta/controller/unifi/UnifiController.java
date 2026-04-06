package fan.summer.hmoneta.controller.unifi;

import fan.summer.hmoneta.controller.unifi.dto.UnifiSettingDto;
import fan.summer.hmoneta.database.entity.unifi.UnifiSettingEntity;
import fan.summer.hmoneta.service.unifi.UnifiApiService;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteInfoLocal;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteManagerLocalResp;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.client.ClientsLocalInfo;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/16
 */
@RestController
@RequestMapping("/hm/unifi")
@AllArgsConstructor
public class UnifiController {
    private final UnifiApiService unifiApiService;

    @GetMapping("/status")
    public ResponseEntity<Boolean> checkUnifiAccessible() {
        return ResponseEntity.ok(unifiApiService.unifiIsAccessible());
    }

    @PostMapping("/setting")
    public ResponseEntity<Boolean> setUnifiSetting(@RequestBody UnifiSettingDto unifiSettingDto) {
        UnifiSettingEntity unifiSettingEntity = new UnifiSettingEntity();
        unifiSettingEntity.setBaseUri(unifiSettingDto.getBaseUri());
        unifiSettingEntity.setApiKey(unifiSettingDto.getApiKey());
        unifiSettingEntity.setLocalBaseUri(unifiSettingDto.getLocalBaseUri());
        unifiSettingEntity.setLocalApiKey(unifiSettingDto.getLocalApiKey());
        unifiApiService.settingUnifiSettingInfo(unifiSettingEntity);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/info")
    public ResponseEntity<UnifiSettingDto> getUnifiSetting() {
        UnifiSettingDto unifiSettingDto = unifiApiService.queryUnifiSettingInfo();
        return ResponseEntity.ok(unifiSettingDto);
    }

    // sites
    @GetMapping("/sites/info")
    public ResponseEntity<SiteManagerLocalResp<SiteInfoLocal>> getAllSitesInfo() {
        SiteManagerLocalResp<SiteInfoLocal> resp = unifiApiService.listLocalSites();
        return ResponseEntity.ok(resp);
    }

    // Clients
    @GetMapping("/sites/clients/info")
    public ResponseEntity<SiteManagerLocalResp<ClientsLocalInfo>> getAllConnectedClients(@RequestParam String siteId) {
        SiteManagerLocalResp<ClientsLocalInfo> resp = unifiApiService.listConnectedClients(siteId);
        return ResponseEntity.ok(resp);

    }
}
