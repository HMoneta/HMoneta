package fan.summer.hmoneta.service.waf.dto.ssl;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 雷池 WAF SSL 证书信息响应
 * <p>
 * 用于封装雷池 WAF 系统返回的 SSL 证书列表数据。
 * 响应结构包含证书列表（nodes）和总数（total）。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/2/7
 */
@Data
public class LeiChiSslInfoResp {

    /**
     * SSL 证书列表
     */
    private List<SslInfo> nodes;

    /**
     * 证书总数
     */
    private Integer total;

    /**
     * 单个 SSL 证书信息
     * <p>
     * 包含证书的基本信息和状态，如域名、颁发者、有效期等。
     * </p>
     */
    @Data
    public static class SslInfo {

        /**
         * 证书 ID
         */
        private Integer id;

        /**
         * 证书关联的域名列表
         */
        private List<String> domains;

        /**
         * 证书颁发者
         */
        private String issuer;

        /**
         * 是否自签名证书
         */
        @JsonProperty("self_signature")
        private Boolean selfSignature;

        /**
         * 证书是否受信任
         */
        private Boolean trusted;

        /**
         * 证书是否已吊销
         */
        private Boolean revoked;

        /**
         * 证书是否已过期
         */
        private Boolean expired;

        /**
         * 证书类型
         * <p>
         * 例如：2 表示 ACME 申请的证书
         * </p>
         */
        private Integer type;

        /**
         * ACME 协议相关消息
         */
        @JsonProperty("acme_message")
        private String acmeMessage;

        /**
         * 证书有效期截止时间
         * <p>
         * 格式：ISO 8601 时间格式，如 "2026-04-27T13:12:23Z"
         * </p>
         */
        @JsonProperty("valid_before")
        private String validBefore;

        /**
         * 证书关联的站点列表
         */
        @JsonProperty("related_sites")
        private List<String> relatedSites;
    }

}
