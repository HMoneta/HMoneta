package fan.summer.hmoneta.controller.dns.entity.req;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 前端解析组请求类
 */
@Data
public class DnsResolveReq {
    private String providerId;

    private String groupName;

    private Map<String, String> authenticateWayMap;

    private List<String> urls;
}
