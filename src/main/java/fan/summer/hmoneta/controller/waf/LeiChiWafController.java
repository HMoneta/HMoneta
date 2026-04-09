package fan.summer.hmoneta.controller.waf;

import fan.summer.hmoneta.controller.waf.dto.LeiChiCrtModifyDto;
import fan.summer.hmoneta.controller.waf.dto.LeiChiSettingDto;
import fan.summer.hmoneta.database.entity.waf.LeiChiSettingEntity;
import fan.summer.hmoneta.service.waf.LeiChiWafService;
import fan.summer.hmoneta.service.waf.dto.ssl.LeiChiSslInfoResp;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 雷池 WAF API 控制器
 * <p>
 * 提供雷池 WAF 系统的 RESTful API 接口，包括：
 * <ul>
 *   <li>设置信息查询</li>
 *   <li>API 令牌配置</li>
 *   <li>SSL 证书列表查询</li>
 * </ul>
 * </p>
 * <p>
 * 所有接口前缀为：<code>/hm/waf/lc</code>
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/25
 */
@RestController
@RequestMapping("/hm/waf/lc")
@AllArgsConstructor
public class LeiChiWafController {

    /**
     * 雷池 WAF 服务
     */
    private LeiChiWafService leiChiWafService;

    /**
     * 查询雷池 WAF 设置信息
     * <p>
     * 获取当前已配置的雷池 API 令牌信息。
     * </p>
     *
     * @return API 令牌字符串，如果未配置则返回 null
     * @apiEndpoint GET /hm/waf/lc/info
     */
    @GetMapping("/info")
    public ResponseEntity<LeiChiSettingDto> queryLeichiToken() {
        LeiChiSettingDto dto = leiChiWafService.queryLeiChiSetting();
        return ResponseEntity.ok(dto);
    }

    /**
     * 配置雷池 WAF API 令牌
     * <p>
     * 保存雷池 WAF 的 API 访问令牌到数据库。
     * 保存前会自动初始化 WebClient 连接。
     * </p>
     *
     * @param leiChiSettingDto 设置数据传输对象，包含 token 字段
     * @return 始终返回 true，表示操作成功
     * @apiEndpoint POST /hm/waf/lc/token
     */
    @PostMapping("/info")
    public ResponseEntity<Boolean> setLeiChiApiToken(@RequestBody LeiChiSettingDto leiChiSettingDto) {
        LeiChiSettingEntity leiChiSettingEntity = new LeiChiSettingEntity();
        leiChiSettingEntity.setBaseUrl(leiChiSettingDto.getBaseUrl());
        leiChiSettingEntity.setToken(leiChiSettingDto.getToken());
        leiChiWafService.insertLeiChiApiToken(leiChiSettingEntity);
        return ResponseEntity.ok(true);
    }

    /**
     * 查询雷池 WAF SSL 证书列表
     * <p>
     * 获取雷池 WAF 系统中所有 SSL 证书的详细信息，包括：
     * <ul>
     *   <li>证书 ID 和关联域名</li>
     *   <li>证书颁发者和类型</li>
     *   <li>证书状态（自签名、受信任、吊销、过期）</li>
     *   <li>证书有效期截止时间</li>
     * </ul>
     * </p>
     * <p>
     * 注意：调用此接口前需先通过 {@code POST /hm/waf/lc/token} 配置 API 令牌。
     * </p>
     *
     * @return SSL 证书信息响应对象
     * @apiEndpoint GET /hm/waf/lc/ssl/list
     */
    @GetMapping("/ssl/list")
    public ResponseEntity<LeiChiSslInfoResp> listLeiChiSslCer() {
        LeiChiSslInfoResp leiChiSslInfoResp = leiChiWafService.listSslCert();
        return ResponseEntity.ok(leiChiSslInfoResp);
    }

    @PostMapping("/ssl/modify")
    public ResponseEntity<String> modifySslCert(@RequestBody LeiChiCrtModifyDto req) {
        leiChiWafService.modifySslCert(req.getCrt(), req.getKey());
        return ResponseEntity.ok("success");
    }
}
