package fan.summer.hmoneta.service.waf;

import fan.summer.hmoneta.service.waf.dto.LeiChiWafApiResp;
import fan.summer.hmoneta.service.waf.dto.ssl.LeiChiSslInfoResp;
import fan.summer.hmoneta.util.WebApiUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 雷池 WAF SSL 证书服务
 * <p>
 * 提供雷池 WAF 系统 SSL 证书相关的 API 调用功能，包括证书列表查询等。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/2/6
 */
@Service
public class LeiChiWafSslService {

    /**
     * 查询雷池 WAF 系统中所有 SSL 证书信息
     *
     * @param webApiUtil Web API 调用工具类，用于执行 HTTP 请求
     * @return 雷池 API 响应，包含证书列表信息（nodes）和总数（total）
     */
    protected LeiChiWafApiResp<LeiChiSslInfoResp> listSslCert(WebApiUtil webApiUtil) {
        Mono<LeiChiWafApiResp<LeiChiSslInfoResp>> api = webApiUtil.api(
                HttpMethod.GET,
                "/open/cert",
                null,
                null,
                new ParameterizedTypeReference<LeiChiWafApiResp<LeiChiSslInfoResp>>() {
                }
        );
        LeiChiWafApiResp<LeiChiSslInfoResp> result = api.block();
        return result;
    }
}
