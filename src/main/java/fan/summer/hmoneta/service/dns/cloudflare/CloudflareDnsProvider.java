package fan.summer.hmoneta.service.dns.cloudflare;

import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.plugin.api.dns.dto.DNSRecordInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cloudflare DDNS 提供商（内置）
 * <p>
 * 通过 Cloudflare API v4 实现 {@link HmDnsProviderPlugin}，支持：
 * <ul>
 *   <li>API Token 认证（建议使用仅授予 Zone.DNS Edit 权限的 Token）</li>
 *   <li>A 记录自动 DDNS（由 DnsUpdateTask 定时触发，本类无状态轮询）</li>
 *   <li>CDN 开关 —— 凭据中的 {@code proxied} 键为 {@code true} 时，
 *       记录将以 Cloudflare 代理（橙云）方式创建/更新，隐藏源站 IP 并启用 CDN；
 *       TXT 等其他类型记录不适用代理，始终以 DNS only 方式写入</li>
 * </ul>
 * </p>
 *
 * <p><b>凭据键：</b>{@code apiToken}（必填）、{@code proxied}（可选，默认 false）。
 * 凭据在每次操作前由 DnsService 通过 {@link #authenticate(Map)} 注入，
 * 并以原子方式快照到 {@link ProviderConfig}，避免多分组间读到半初始化状态。</p>
 *
 * <p><b>Zone 解析：</b>按完整记录名逐级向上探测 Zone（如 a.b.example.com → b.example.com → example.com），
 * 并按域名缓存 10 分钟，避免每次更新都查询 Zone 列表。</p>
 *
 * @author phoebej
 * @version 1.00
 * @date 2026/9/8
 */
@Component
public class CloudflareDnsProvider implements HmDnsProviderPlugin {

    private static final Logger log = LoggerFactory.getLogger(CloudflareDnsProvider.class);

    public static final String PROVIDER_NAME = "Cloudflare";
    public static final String CREDENTIAL_API_TOKEN = "apiToken";
    public static final String CREDENTIAL_PROXIED = "proxied";

    /** 未开启代理时 A/AAAA 记录的 TTL（秒）；开启代理时使用 1（Auto） */
    private static final long TTL_UNPROXIED = 300;
    /** TXT 等 DNS only 记录的 TTL（秒） */
    private static final long TTL_DNS_ONLY = 120;
    /** 适用 Cloudflare 代理（CDN）的记录类型 */
    private static final Set<String> PROXIABLE_TYPES = Set.of("A", "AAAA");
    /** Zone 缓存有效期（毫秒） */
    private static final long ZONE_CACHE_TTL_MS = 10 * 60 * 1000L;

    /** 认证后的提供商配置快照；每次 authenticate 原子替换 */
    private volatile ProviderConfig config = ProviderConfig.EMPTY;

    private final CloudflareApiClient apiClient;

    /** zoneId 缓存：域名 → (zoneId, 过期时间戳) */
    private final Map<String, CachedZone> zoneCache = new ConcurrentHashMap<>();

    private record ProviderConfig(String apiToken, boolean proxied) {
        static final ProviderConfig EMPTY = new ProviderConfig(null, false);
    }

    private record CachedZone(String zoneId, long expiresAtMs) {
    }

    public CloudflareDnsProvider(CloudflareApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public void authenticate(Map<String, String> credentials) {
        if (credentials == null) {
            throw new IllegalStateException("[CF] Cloudflare 凭据为空，请填写 apiToken");
        }
        String apiToken = credentials.get(CREDENTIAL_API_TOKEN);
        if (apiToken == null || apiToken.isBlank()) {
            throw new IllegalStateException("[CF] 缺少 Cloudflare API Token（凭据键 apiToken）");
        }
        boolean proxied = parseProxied(credentials.get(CREDENTIAL_PROXIED));
        this.config = new ProviderConfig(apiToken.trim(), proxied);
        if (!apiClient.verifyToken(apiToken.trim())) {
            log.warn("[CF] API Token 校验未通过，请确认 Token 有效且具有 DNS 编辑权限，将继续尝试执行 DNS 操作");
        }
    }

    @Override
    public Set<String> authenticateWay() {
        return new LinkedHashSet<>(List.of(CREDENTIAL_API_TOKEN, CREDENTIAL_PROXIED));
    }

    @Override
    public String providerName() {
        return PROVIDER_NAME;
    }

    @Override
    public List<DNSRecordInfo> dnsCheck(String domain, String subDomain) {
        ProviderConfig cfg = config;
        String fqdn = toFqdn(domain, subDomain);
        return resolveZoneId(cfg, fqdn)
                .map(zoneId -> apiClient.listRecords(cfg.apiToken(), zoneId, fqdn, null).stream()
                        .map(record -> new DNSRecordInfo(record.type(), 0, record.content()))
                        .toList())
                .orElseGet(List::of);
    }

    @Override
    public boolean modifyDns(String domain, String subDomain, String type, String value) {
        ProviderConfig cfg = config;
        String fqdn = toFqdn(domain, subDomain);
        String recordType = type == null ? "A" : type.toUpperCase();
        // Cloudflare 仅 A/AAAA/CNAME 可代理；DDNS 场景下 TXT（ACME DNS-01）必须 DNS only
        boolean proxied = cfg.proxied() && PROXIABLE_TYPES.contains(recordType);
        long ttl = proxied ? 1 : (PROXIABLE_TYPES.contains(recordType) ? TTL_UNPROXIED : TTL_DNS_ONLY);

        return resolveZoneId(cfg, fqdn).map(zoneId -> {
            List<CloudflareApiClient.CfDnsRecord> existing =
                    apiClient.listRecords(cfg.apiToken(), zoneId, fqdn, recordType);
            if (existing.isEmpty()) {
                log.info("[CF] 创建记录 {} {} -> {} (proxied={})", recordType, fqdn, value, proxied);
                return apiClient.createRecord(cfg.apiToken(), zoneId, fqdn, recordType, value, proxied, ttl);
            }
            // 同名记录只保留第一条，其余删除，避免多次 DDNS 累积出重复记录
            boolean result = true;
            for (int i = 0; i < existing.size(); i++) {
                CloudflareApiClient.CfDnsRecord record = existing.get(i);
                if (i == 0) {
                    boolean changed = !record.content().equals(value) || record.proxied() != proxied;
                    log.info("[CF] {}记录 {} {} -> {} (proxied={})",
                            changed ? "更新" : "刷新", recordType, fqdn, value, proxied);
                    result = apiClient.updateRecord(cfg.apiToken(), zoneId, record.id(), fqdn, recordType, value, proxied, ttl);
                } else {
                    log.info("[CF] 删除重复记录 {} ({})", fqdn, record.id());
                    result &= apiClient.deleteRecord(cfg.apiToken(), zoneId, record.id());
                }
            }
            return result;
        }).orElse(false);
    }

    @Override
    public boolean deleteDns(String domain, String subDomain, String type) {
        ProviderConfig cfg = config;
        String fqdn = toFqdn(domain, subDomain);
        String recordType = type == null ? "A" : type.toUpperCase();
        return resolveZoneId(cfg, fqdn).map(zoneId -> {
            List<CloudflareApiClient.CfDnsRecord> existing =
                    apiClient.listRecords(cfg.apiToken(), zoneId, fqdn, recordType);
            if (existing.isEmpty()) {
                log.info("[CF] 记录 {} ({}) 不存在，视为删除成功", fqdn, recordType);
                return true;
            }
            boolean result = true;
            for (CloudflareApiClient.CfDnsRecord record : existing) {
                result &= apiClient.deleteRecord(cfg.apiToken(), zoneId, record.id());
            }
            return result;
        }).orElse(false);
    }

    /**
     * 解析记录所属 Zone：优先读缓存，未命中则按完整域名逐级向上探测
     */
    private Optional<String> resolveZoneId(ProviderConfig cfg, String fqdn) {
        if (cfg.apiToken() == null) {
            log.error("[CF] 尚未认证（缺少 apiToken），无法操作 {}", fqdn);
            return Optional.empty();
        }
        long now = System.currentTimeMillis();
        CachedZone cached = zoneCache.get(fqdn);
        if (cached != null && cached.expiresAtMs() > now) {
            return Optional.of(cached.zoneId());
        }
        return apiClient.findZoneId(cfg.apiToken(), fqdn).map(zoneId -> {
            zoneCache.put(fqdn, new CachedZone(zoneId, now + ZONE_CACHE_TTL_MS));
            log.info("[CF] {} 所属 Zone: {}", fqdn, zoneId);
            return zoneId;
        });
    }

    private static String toFqdn(String domain, String subDomain) {
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("[CF] 主域名为空");
        }
        if (subDomain == null || subDomain.isBlank() || "@".equals(subDomain)) {
            return domain.trim().toLowerCase();
        }
        return (subDomain.trim() + "." + domain).trim().toLowerCase();
    }

    static boolean parseProxied(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toLowerCase()) {
            case "true", "1", "yes", "on" -> true;
            default -> false;
        };
    }
}
