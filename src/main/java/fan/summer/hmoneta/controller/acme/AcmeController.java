package fan.summer.hmoneta.controller.acme;

import fan.summer.hmoneta.controller.acme.dto.AcmeUserReq;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.service.acme.AcmeService;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

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

    @GetMapping("/download-cert/{domain}")
    public ResponseEntity<byte[]> downloadCert(@PathVariable String domain) {
        try {
            byte[] certZipBytes = acmeService.packCertifications(domain);
            if (certZipBytes == null || certZipBytes.length == 0) {
                return ResponseEntity.notFound().build();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            String fileName = domain + "_certificate.zip";
            headers.setContentDispositionFormData(fileName, fileName);
            headers.setContentLength(certZipBytes.length);
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0L);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(certZipBytes);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
