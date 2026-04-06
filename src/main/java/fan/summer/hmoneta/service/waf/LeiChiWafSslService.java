package fan.summer.hmoneta.service.waf;

import fan.summer.hmoneta.service.waf.dto.LeiChiWafApiResp;
import fan.summer.hmoneta.service.waf.dto.ssl.LeiChiSslCerInsertReq;
import fan.summer.hmoneta.service.waf.dto.ssl.LeiChiSslInfoResp;
import fan.summer.hmoneta.util.WebApiUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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
     * <p>
     * 调用雷池 WAF 系统的 /open/cert 接口获取所有已添加的 SSL 证书列表，
     * 返回结果包含证书节点列表（nodes）和总数（total），用于前端展示和证书管理。
     * </p>
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

    /**
     * 向雷池 WAF 系统添加 SSL 证书
     * <p>
     * 通过手动方式（manual）向雷池 WAF 系统添加 SSL 证书。
     * 证书类型设置为 2（表示手动添加的非自动申请证书）。
     * 单独处理 WebClientResponseException 以便捕获雷池 API 返回的错误信息，
     * 包括 HTTP 状态码和响应体，便于问题排查和错误提示。
     * </p>
     *
     * @param webApiUtil Web API 调用工具类
     * @param crt        证书内容（PEM 格式）
     * @param key        私钥内容（PEM 格式）
     * @return API 响应结果
     */
    protected String modifySslCert(WebApiUtil webApiUtil, String crt, String key) {
        LeiChiSslCerInsertReq req = new LeiChiSslCerInsertReq();
        LeiChiSslCerInsertReq.CertInfo certInfo = new LeiChiSslCerInsertReq.CertInfo();
        certInfo.setCrt(crt);
        certInfo.setKey(key);
        req.setManual(certInfo);
        req.setType(2);
        try {
            Mono<String> api = webApiUtil.api(HttpMethod.POST, "/open/cert", null, req, new ParameterizedTypeReference<String>() {
            });
            return api.block();
        } catch (WebClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            int statusCode = e.getStatusCode().value();
            throw new RuntimeException(
                    "雷池 API 返回错误 [" + statusCode + "]: " + errorBody,
                    e
            );

        }

    }
}
