package fan.summer.hmoneta.controller.unifi;

import fan.summer.hmoneta.controller.unifi.dto.UnifiSettingDto;
import fan.summer.hmoneta.database.entity.unifi.UnifiSettingEntity;
import fan.summer.hmoneta.service.unifi.UnifiApiService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    @PostMapping("/setting")
    public ResponseEntity<Boolean> setUnifiSetting(@RequestBody UnifiSettingDto unifiSettingDto) {
        UnifiSettingEntity unifiSettingEntity = new UnifiSettingEntity();
        unifiSettingEntity.setBaseUri(unifiSettingDto.getBaseUri());
        unifiSettingEntity.setApiKey(unifiSettingDto.getApiKey());
        unifiApiService.settingUnifiSettingInfo(unifiSettingEntity);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> getUnifiSetting() {
        UnifiSettingEntity unifiSettingEntity = unifiApiService.queryUnifiSettingInfo();
        return ResponseEntity.ok(Map.of("baseUri", unifiSettingEntity.getBaseUri()));
    }
}
