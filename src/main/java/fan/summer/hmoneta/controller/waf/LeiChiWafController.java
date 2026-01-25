package fan.summer.hmoneta.controller.waf;

import fan.summer.hmoneta.controller.waf.dto.LeiChiTokenDto;
import fan.summer.hmoneta.database.entity.waf.LeiChiTokenEntity;
import fan.summer.hmoneta.service.waf.LeiChiWafService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/25
 */
@RestController
@RequestMapping("/hm/waf/lc")
@AllArgsConstructor
public class LeiChiWafController {
    private LeiChiWafService leiChiWafService;

    @PostMapping("/token")
    public ResponseEntity<Boolean> setLeiChiApiToken(@RequestBody LeiChiTokenDto leiChiTokenDto) {
        LeiChiTokenEntity leiChiTokenEntity = new LeiChiTokenEntity();
        leiChiTokenEntity.setToken(leiChiTokenDto.getToken());
        leiChiWafService.insertLeiChiApiToken(leiChiTokenEntity);
        return ResponseEntity.ok(true);
    }
}
