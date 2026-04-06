package fan.summer.hmoneta.service.waf.dto.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 雷池 WAF 站点列表响应
 * <p>
 * 用于封装雷池 WAF 系统返回的站点列表数据。
 * 响应结构包含站点列表（data）和总数（total）。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/3
 */
@Data
public class LeiChiSiteListResp {

    /**
     * 站点列表
     */
    private List<SiteInfo> data;

    /**
     * 站点总数
     */
    private Integer total;

    /**
     * 单个站点信息
     */
    @Data
    public static class SiteInfo {

        /**
         * 站点 ID
         */
        private Integer id;

        /**
         * 站点名称/标题
         */
        private String title;

        /**
         * 站点备注
         */
        private String comment;

        /**
         * 站点图标
         */
        private String icon;

        /**
         * 域名列表
         */
        @JsonProperty("server_names")
        private List<String> serverNames;

        /**
         * 端口列表
         */
        private List<String> ports;

        /**
         * 上游服务器地址列表
         */
        private List<String> upstreams;

        /**
         * 默认页面
         */
        private String index;

        /**
         * 是否启用
         */
        @JsonProperty("is_enabled")
        private Boolean isEnabled;

        /**
         * 站点分组 ID
         */
        @JsonProperty("group_id")
        private Integer groupId;

        /**
         * 运行模式
         * <p>
         * 0 = defense (防护模式)
         * 1 = offline (下线模式)
         * 2 = dry run (观察模式)
         * </p>
         */
        private Integer mode;

        /**
         * 证书 ID
         */
        @JsonProperty("cert_id")
        private Integer certId;

        /**
         * 证书类型
         * <p>
         * 0 = 无证书
         * 1 = 上传证书
         * 2 = 自签名证书
         * 3 = 管理证书
         * </p>
         */
        @JsonProperty("cert_type")
        private Integer certType;

        /**
         * 是否启用统计
         */
        @JsonProperty("stat_enabled")
        private Boolean statEnabled;

        /**
         * 是否启用安全态势
         */
        @JsonProperty("sp_enabled")
        private Boolean spEnabled;

        /**
         * 是否启用健康检查
         */
        @JsonProperty("health_check")
        private Boolean healthCheck;

        /**
         * 是否启用认证防御
         */
        @JsonProperty("auth_defense_id")
        private Integer authDefenseId;

        /**
         * 是否启用反爬
         */
        @JsonProperty("challenge_id")
        private Integer challengeId;

        /**
         * 是否启用动态安全
         */
        @JsonProperty("chaos_id")
        private Integer chaosId;

        /**
         * 等候室 ID
         */
        @JsonProperty("wr_id")
        private Integer wrId;

        /**
         * 是否启用语义引擎
         */
        private Boolean semantics;

        /**
         * 是否启用 Portal
         */
        private Boolean portal;

        /**
         * 站点类型
         */
        private Integer type;

        /**
         * 排序位置
         */
        private Integer position;

        /**
         * 负载均衡配置
         */
        @JsonProperty("load_balance")
        private LoadBalance loadBalance;

        /**
         * 创建时间
         */
        @JsonProperty("created_at")
        private String createdAt;

        /**
         * 更新时间
         */
        @JsonProperty("updated_at")
        private String updatedAt;
    }

    /**
     * 负载均衡配置
     */
    @Data
    public static class LoadBalance {
        @JsonProperty("balance_type")
        private Integer balanceType;
    }
}
