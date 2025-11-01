package fan.summer.hmoneta.controller.dns.entity.resp;

import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/10/19
 */
@Data
public class DnsResolveResp {
    private String groupId;
    private String groupName;
    private Map<String,String> authenticateWayMap;
    private List<DnsResolveUrlEntity> urls;
}
