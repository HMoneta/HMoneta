package fan.summer.hmoneta.service.waf.dto.ssl;

import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/2/8
 */
@Data
public class LeiChiSslCerInsertReq {
    private Integer type;
    private CertInfo manual;

    @Data
    public static class CertInfo {
        private String crt;
        private String key;
    }
}
