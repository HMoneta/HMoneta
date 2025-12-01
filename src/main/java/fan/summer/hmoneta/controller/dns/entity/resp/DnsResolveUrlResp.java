package fan.summer.hmoneta.controller.dns.entity.resp;

import fan.summer.hmoneta.controller.acme.dto.resp.AcmeCerInfoResp;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/12/1
 */
@Data
public class DnsResolveUrlResp {
    private String id;

    private String groupId;

    private String url;

    private Integer resolveStatus;

    private String ipAddress;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private AcmeCerInfoResp acmeCerInfo;
}
