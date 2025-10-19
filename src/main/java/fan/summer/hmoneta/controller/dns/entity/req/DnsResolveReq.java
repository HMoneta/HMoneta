package fan.summer.hmoneta.controller.dns.entity.req;

import lombok.Data;

import java.util.List;

/**
 * 前端解析组请求类
 */
@Data
public class DnsResolveReq {
    private String providerId;

    private String authId;

    private String authKey;

    private List<String> urls;
}
