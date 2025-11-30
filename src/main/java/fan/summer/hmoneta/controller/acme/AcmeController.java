package fan.summer.hmoneta.controller.acme;

import fan.summer.hmoneta.controller.acme.dto.AcmeUserReq;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.service.acme.AcmeService;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/23
 */
@RestController
@RequestMapping("/hm/acme")
public class AcmeController {
    private final AcmeService acmeService;

    public AcmeController(AcmeService acmeService) {
        this.acmeService = acmeService;
    }

    @PostMapping("/modify")
    public ResponseEntity<String> modifyAcmeUserInfo(@RequestBody AcmeUserReq req) {
        AcmeUserInfoEntity acmeUserInfo = new AcmeUserInfoEntity();
        BeanUtils.copyProperties(req, acmeUserInfo);
        acmeService.insertAcmeUserInfo(acmeUserInfo);
        return ResponseEntity.ok("success");
    }

    @GetMapping("/apply")
    public ResponseEntity<String> applyCertification(@RequestParam String domain) {
        String s = acmeService.applyCertification(domain);
        return ResponseEntity.ok(s);
    }
}
