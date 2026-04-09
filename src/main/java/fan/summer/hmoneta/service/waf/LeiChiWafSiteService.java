package fan.summer.hmoneta.service.waf;

import fan.summer.hmoneta.service.waf.dto.LeiChiWafApiResp;
import fan.summer.hmoneta.service.waf.dto.site.LeiChiSiteListResp;
import fan.summer.hmoneta.util.WebApiUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 雷池 WAF 站点服务
 * <p>
 * 提供雷池 WAF 系统站点相关的 API 调用功能，包括站点列表查询等。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/3
 */
@Service
public class LeiChiWafSiteService {

    private static final Logger log = LoggerFactory.getLogger(LeiChiWafSiteService.class);

    /**
     * 查询雷池 WAF 系统中所有站点信息
     * <p>
     * 调用雷池 WAF 系统的 /open/site 接口获取所有站点的列表，
     * 返回结果包含站点列表（data）和总数（total）。
     * </p>
     *
     * @param webApiUtil Web API 调用工具类，用于执行 HTTP 请求
     * @return 雷池 API 响应，包含站点列表信息（data）和总数（total）
     */
    protected LeiChiWafApiResp<LeiChiSiteListResp> listSite(WebApiUtil webApiUtil) {
        log.info("[雷池WAF] 查询站点列表 - 请求路径: /open/site");

        Mono<LeiChiWafApiResp<LeiChiSiteListResp>> api = webApiUtil.api(
                HttpMethod.GET,
                "/open/site",
                null,
                null,
                new ParameterizedTypeReference<LeiChiWafApiResp<LeiChiSiteListResp>>() {
                }
        );
        LeiChiWafApiResp<LeiChiSiteListResp> result = api.block();

        log.info("[雷池WAF] 查询站点列表完成 - 返回结果: {}", result);
        return result;
    }
}
