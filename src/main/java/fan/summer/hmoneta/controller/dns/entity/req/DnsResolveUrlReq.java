package fan.summer.hmoneta.controller.dns.entity.req;

import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/9
 */
@Data
public class DnsResolveUrlReq {
    private String id;
    private String groupId;
    private String url;
}
