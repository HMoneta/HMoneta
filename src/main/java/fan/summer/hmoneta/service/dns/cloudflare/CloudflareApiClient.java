package fan.summer.hmoneta.service.dns.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cloudflare API v4 客户端
 * <p>
 * 封装 DDNS 所需的最小接口集合：Zone 查询与 dns_records 的增删改查。
 * 所有请求使用 Bearer API Token 认证，响应统一按 {@code {success, errors, result}} 结构解析。
 * </p>
 *
 * <p>baseUrl 可注入，便于测试时指向本地模拟服务。</p>
 *
 * @author phoebej
 * @version 1.00
 * @date 2026/9/8
 */
@Component
public class CloudflareApiClient {

    private static final Logger log = LoggerFactory.getLogger(CloudflareApiClient.class);

    /** Cloudflare API v4 默认地址 */
    public static final String DEFAULT_BASE_URL = "https://api.cloudflare.com/client/v4";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    /** ObjectMapper 配置完成后线程安全，静态共享即可 */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final HttpClient httpClient;

    public CloudflareApiClient() {
        this(DEFAULT_BASE_URL);
    }

    CloudflareApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Cloudflare DNS 记录
     *
     * @param id       记录 ID（32 位十六进制字符串）
     * @param type     记录类型，如 A / AAAA / TXT
     * @param name     完整记录名，如 www.example.com
     * @param content  记录值（A 记录为 IP，TXT 为文本）
     * @param proxied  是否开启 Cloudflare 代理（橙云 / CDN）
     */
    public record CfDnsRecord(String id, String type, String name, String content, boolean proxied) {
    }

    /**
     * 校验 API Token 是否有效
     *
     * @param apiToken Cloudflare API Token
     * @return token 有效返回 true；网络异常时返回 false 并记录日志
     */
    public boolean verifyToken(String apiToken) {
        try {
            JsonNode body = exchange(buildRequest("GET", "/user/tokens/verify", apiToken, null));
            return body != null && body.path("success").asBoolean(false);
        } catch (Exception e) {
            log.warn("[CF] API Token 校验请求失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 查询 Zone ID，支持逐级向上回退：
     * 依次尝试 {@code candidate}、去掉首标签后的域名……直到仅剩两段标签。
     * 这样 {@code www.example.co.uk}、{@code a.b.example.com} 均能定位到真实托管的 Zone。
     *
     * @param apiToken API Token
     * @param host     起始域名（完整记录名或其一部分）
     * @return 命中的 Zone ID；未找到返回 Optional.empty()
     */
    public Optional<String> findZoneId(String apiToken, String host) {
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        String candidate = host.trim().toLowerCase();
        while (true) {
            Optional<String> zoneId = queryZoneId(apiToken, candidate);
            if (zoneId.isPresent()) {
                return zoneId;
            }
            int dot = candidate.indexOf('.');
            // 仅剩两段标签（如 example.com）时停止回退
            if (candidate.indexOf('.', dot + 1) < 0) {
                return Optional.empty();
            }
            candidate = candidate.substring(dot + 1);
        }
    }

    private Optional<String> queryZoneId(String apiToken, String name) {
        try {
            String query = urlencode("name", name);
            JsonNode body = exchange(buildRequest("GET", "/zones?" + query + "&status=active&per_page=1", apiToken, null));
            if (body == null || !body.path("success").asBoolean(false)) {
                return Optional.empty();
            }
            JsonNode result = body.path("result");
            if (result.isArray() && !result.isEmpty()) {
                return Optional.ofNullable(result.get(0).path("id").asText(null));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("[CF] 查询 Zone 失败, name: {}, 原因: {}", name, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 查询 Zone 下的 DNS 记录
     *
     * @param apiToken API Token
     * @param zoneId   Zone ID
     * @param name     记录名过滤，可为 null
     * @param type     记录类型过滤，可为 null
     * @return 记录列表；请求失败返回空列表
     */
    public List<CfDnsRecord> listRecords(String apiToken, String zoneId, String name, String type) {
        StringBuilder path = new StringBuilder("/zones/").append(zoneId).append("/dns_records?per_page=100");
        if (name != null && !name.isBlank()) {
            path.append("&").append(urlencode("name", name.trim().toLowerCase()));
        }
        if (type != null && !type.isBlank()) {
            path.append("&").append(urlencode("type", type.trim().toUpperCase()));
        }
        try {
            JsonNode body = exchange(buildRequest("GET", path.toString(), apiToken, null));
            if (body == null || !body.path("success").asBoolean(false)) {
                return List.of();
            }
            List<CfDnsRecord> records = new ArrayList<>();
            for (JsonNode node : body.path("result")) {
                records.add(new CfDnsRecord(
                        node.path("id").asText(null),
                        node.path("type").asText(null),
                        node.path("name").asText(null),
                        node.path("content").asText(null),
                        node.path("proxied").asBoolean(false)));
            }
            return records;
        } catch (Exception e) {
            log.error("[CF] 查询 DNS 记录失败, zone: {}, 原因: {}", zoneId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 创建 DNS 记录
     *
     * @return 创建成功返回 true
     */
    public boolean createRecord(String apiToken, String zoneId, String name, String type, String content, boolean proxied, long ttl) {
        ObjectNode payload = recordPayload(name, type, content, proxied, ttl);
        try {
            JsonNode body = exchange(buildRequest("POST", "/zones/" + zoneId + "/dns_records", apiToken, payload));
            return body != null && body.path("success").asBoolean(false);
        } catch (Exception e) {
            log.error("[CF] 创建 DNS 记录失败, name: {}, 原因: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 更新 DNS 记录（全量替换）
     *
     * @return 更新成功返回 true
     */
    public boolean updateRecord(String apiToken, String zoneId, String recordId, String name, String type, String content, boolean proxied, long ttl) {
        ObjectNode payload = recordPayload(name, type, content, proxied, ttl);
        try {
            JsonNode body = exchange(buildRequest("PUT", "/zones/" + zoneId + "/dns_records/" + recordId, apiToken, payload));
            return body != null && body.path("success").asBoolean(false);
        } catch (Exception e) {
            log.error("[CF] 更新 DNS 记录失败, name: {}, 原因: {}", name, e.getMessage());
            return false;
        }
    }

    /**
     * 删除 DNS 记录
     *
     * @return 删除成功返回 true
     */
    public boolean deleteRecord(String apiToken, String zoneId, String recordId) {
        try {
            JsonNode body = exchange(buildRequest("DELETE", "/zones/" + zoneId + "/dns_records/" + recordId, apiToken, null));
            return body != null && body.path("success").asBoolean(false);
        } catch (Exception e) {
            log.error("[CF] 删除 DNS 记录失败, recordId: {}, 原因: {}", recordId, e.getMessage());
            return false;
        }
    }

    private ObjectNode recordPayload(String name, String type, String content, boolean proxied, long ttl) {
        ObjectNode payload = MAPPER.createObjectNode();
        payload.put("type", type);
        payload.put("name", name);
        payload.put("content", content);
        payload.put("proxied", proxied);
        payload.put("ttl", ttl);
        return payload;
    }

    private HttpRequest buildRequest(String method, String path, String apiToken, ObjectNode payload) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", "Bearer " + apiToken)
                .header("Content-Type", "application/json");
        if (payload == null) {
            return builder.method(method, HttpRequest.BodyPublishers.noBody()).build();
        }
        return builder.method(method, HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8)).build();
    }

    /**
     * 执行请求并解析响应体；HTTP 非 2xx 或 JSON 解析失败返回 null
     */
    private JsonNode exchange(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            log.error("[CF] HTTP {} {} {}", response.statusCode(), request.method(), request.uri().getPath());
            log.error("[CF] 响应体: {}", response.body());
            return null;
        }
        JsonNode body = MAPPER.readTree(response.body() == null ? "" : response.body());
        if (body.has("success") && !body.path("success").asBoolean(false)) {
            log.error("[CF] API 返回失败: {}", summarizeErrors(body.path("errors")));
        }
        return body;
    }

    private String summarizeErrors(JsonNode errors) {
        if (!errors.isArray() || errors.isEmpty()) {
            return "(无错误详情)";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode error : (ArrayNode) errors) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append(error.path("code").asInt(-1)).append(": ").append(error.path("message").asText(""));
        }
        return sb.toString();
    }

    private static String urlencode(String key, String value) {
        return key + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
