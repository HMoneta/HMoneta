package fan.summer.hmoneta.service.dns.cloudflare;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CloudflareDnsProvider 单元测试
 * <p>
 * 使用本地 {@link HttpServer} 模拟 Cloudflare API v4，覆盖：
 * Zone 逐级探测、A 记录 upsert（含 proxied/CDN 开关）、TXT 记录不代理、
 * 重复记录清理、删除语义、API 错误处理与凭据解析。
 * </p>
 */
class CloudflareDnsProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private FakeCloudflareServer cf;
    private CloudflareDnsProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        cf = new FakeCloudflareServer();
        cf.start();
        provider = new CloudflareDnsProvider(
                new CloudflareApiClient("http://127.0.0.1:" + cf.port()));
    }

    @AfterEach
    void tearDown() {
        cf.stop();
    }

    private void authenticate(String proxied) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("apiToken", "test-token");
        if (proxied != null) {
            credentials.put("proxied", proxied);
        }
        provider.authenticate(credentials);
    }

    @Test
    void should_reject_authentication_without_api_token() {
        Map<String, String> empty = new HashMap<>();
        empty.put("proxied", "true");
        assertThrows(IllegalStateException.class, () -> provider.authenticate(empty));
        assertThrows(IllegalStateException.class, () -> provider.authenticate(null));
    }

    @Test
    void should_expose_provider_name_and_credential_keys() {
        assertEquals("Cloudflare", provider.providerName());
        assertTrue(provider.authenticateWay().contains("apiToken"));
        assertTrue(provider.authenticateWay().contains("proxied"));
    }

    @Test
    void should_return_existing_a_records_on_dns_check() {
        cf.addZone("example.com", "zone-1");
        cf.addRecord("zone-1", "A", "www.example.com", "1.2.3.4", false);
        authenticate(null);

        var records = provider.dnsCheck("example.com", "www");
        assertEquals(1, records.size());
        assertEquals("A", records.get(0).gettype());
        assertEquals("1.2.3.4", records.get(0).getvalue());
    }

    @Test
    void should_create_proxied_record_when_absent() {
        cf.addZone("example.com", "zone-1");
        authenticate("true");

        assertTrue(provider.modifyDns("example.com", "www", "A", "5.6.7.8"));

        var captured = cf.lastRequest("POST", "/zones/zone-1/dns_records");
        assertNotNull(captured);
        assertEquals("www.example.com", captured.body().path("name").asText());
        assertEquals("5.6.7.8", captured.body().path("content").asText());
        assertTrue(captured.body().path("proxied").asBoolean());
        assertEquals(1, captured.body().path("ttl").asLong());
    }

    @Test
    void should_create_unproxied_record_with_normal_ttl_by_default() {
        cf.addZone("example.com", "zone-1");
        authenticate(null);

        assertTrue(provider.modifyDns("example.com", "www", "A", "5.6.7.8"));

        var captured = cf.lastRequest("POST", "/zones/zone-1/dns_records");
        assertFalse(captured.body().path("proxied").asBoolean());
        assertEquals(300, captured.body().path("ttl").asLong());
    }

    @Test
    void should_update_record_when_ip_changed() {
        cf.addZone("example.com", "zone-1");
        cf.addRecord("zone-1", "A", "www.example.com", "1.2.3.4", false);
        authenticate("true");

        assertTrue(provider.modifyDns("example.com", "www", "A", "5.6.7.8"));

        var record = cf.records.get(0);
        assertEquals("5.6.7.8", record.path("content").asText());
        assertTrue(record.path("proxied").asBoolean());
    }

    @Test
    void should_apply_proxied_toggle_even_when_ip_unchanged() {
        cf.addZone("example.com", "zone-1");
        cf.addRecord("zone-1", "A", "www.example.com", "1.2.3.4", false);
        authenticate("true");

        // IP 相同但分组从 DNS only 切换为开启 CDN，必须产生一次 PUT
        assertTrue(provider.modifyDns("example.com", "www", "A", "1.2.3.4"));

        var captured = cf.lastRequest("PUT", "/zones/zone-1/dns_records/rec-0");
        assertNotNull(captured);
        assertTrue(captured.body().path("proxied").asBoolean());
        assertTrue(cf.records.get(0).path("proxied").asBoolean());
    }

    @Test
    void should_never_proxy_txt_records_for_acme_challenge() {
        cf.addZone("example.com", "zone-1");
        authenticate("true");

        assertTrue(provider.modifyDns("example.com", "_acme-challenge.www", "TXT", "digest-value"));

        var captured = cf.lastRequest("POST", "/zones/zone-1/dns_records");
        assertEquals("TXT", captured.body().path("type").asText());
        assertEquals("_acme-challenge.www.example.com", captured.body().path("name").asText());
        assertFalse(captured.body().path("proxied").asBoolean());
        assertEquals(120, captured.body().path("ttl").asLong());
    }

    @Test
    void should_deduplicate_multiple_records_with_same_name() {
        cf.addZone("example.com", "zone-1");
        cf.addRecord("zone-1", "A", "www.example.com", "1.2.3.4", false);
        cf.addRecord("zone-1", "A", "www.example.com", "9.9.9.9", false);
        authenticate(null);

        assertTrue(provider.modifyDns("example.com", "www", "A", "5.6.7.8"));

        assertEquals(1, cf.records.size());
        assertEquals("5.6.7.8", cf.records.get(0).path("content").asText());
        assertNotNull(cf.lastRequest("DELETE", "/zones/zone-1/dns_records/rec-1"));
    }

    @Test
    void should_delete_existing_records() {
        cf.addZone("example.com", "zone-1");
        cf.addRecord("zone-1", "A", "www.example.com", "1.2.3.4", true);
        authenticate(null);

        assertTrue(provider.deleteDns("example.com", "www", "A"));
        assertTrue(cf.records.isEmpty());
    }

    @Test
    void should_treat_delete_as_success_when_record_missing() {
        cf.addZone("example.com", "zone-1");
        authenticate(null);

        assertTrue(provider.deleteDns("example.com", "www", "A"));
    }

    @Test
    void should_walk_up_labels_to_locate_zone() {
        // www.example.co.uk：先查 www.example.co.uk 未命中，再查 example.co.uk 命中
        cf.addZone("example.co.uk", "zone-uk");
        authenticate(null);

        assertTrue(provider.modifyDns("example.co.uk", "www", "A", "5.6.7.8"));
        assertEquals("zone-uk", cf.records.get(0).path("zone_id").asText());

        List<String> zoneQueries = cf.requests().stream()
                .filter(r -> r.method().equals("GET") && r.path().equals("/zones"))
                .map(FakeCloudflareServer.Request::queryNameParam)
                .toList();
        assertEquals(List.of("www.example.co.uk", "example.co.uk"), zoneQueries);
    }

    @Test
    void should_support_root_domain_records() {
        cf.addZone("example.com", "zone-1");
        authenticate("true");

        assertTrue(provider.modifyDns("example.com", null, "A", "5.6.7.8"));

        var captured = cf.lastRequest("POST", "/zones/zone-1/dns_records");
        assertEquals("example.com", captured.body().path("name").asText());
    }

    @Test
    void should_return_false_when_zone_not_found() {
        authenticate(null);
        assertFalse(provider.modifyDns("nowhere.example", "www", "A", "5.6.7.8"));
    }

    @Test
    void should_return_false_when_api_reports_error() {
        cf.addZone("example.com", "zone-1");
        authenticate(null);
        cf.failMode = true;

        assertFalse(provider.modifyDns("example.com", "www", "A", "5.6.7.8"));
        assertFalse(provider.deleteDns("example.com", "www", "A"));
    }

    @Test
    void should_parse_proxied_flag_variants() {
        assertTrue(CloudflareDnsProvider.parseProxied("true"));
        assertTrue(CloudflareDnsProvider.parseProxied("TRUE"));
        assertTrue(CloudflareDnsProvider.parseProxied("1"));
        assertTrue(CloudflareDnsProvider.parseProxied("yes"));
        assertTrue(CloudflareDnsProvider.parseProxied(" on "));
        assertFalse(CloudflareDnsProvider.parseProxied("false"));
        assertFalse(CloudflareDnsProvider.parseProxied("0"));
        assertFalse(CloudflareDnsProvider.parseProxied(null));
        assertFalse(CloudflareDnsProvider.parseProxied("whatever"));
    }

    /**
     * 本地模拟 Cloudflare API v4 服务
     */
    static class FakeCloudflareServer {
        record Request(String method, String path, String query, JsonNode body) {
            String queryNameParam() {
                if (query == null) return null;
                for (String kv : query.split("&")) {
                    String[] pair = kv.split("=", 2);
                    if ("name".equals(pair[0]) && pair.length == 2) {
                        return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
                return null;
            }
        }

        private final HttpServer server;
        private final Map<String, String> zoneByName = new ConcurrentHashMap<>();
        private final List<ObjectNode> records = new CopyOnWriteArrayList<>();
        private final List<Request> requests = new CopyOnWriteArrayList<>();
        volatile boolean failMode = false;

        FakeCloudflareServer() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String method = exchange.getRequestMethod();
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getRawQuery();
                JsonNode body = null;
                if (!"GET".equals(method) && !"DELETE".equals(method)) {
                    body = MAPPER.readTree(exchange.getRequestBody().readAllBytes());
                }
                requests.add(new Request(method, path, query, body));

                ObjectNode response;
                if (failMode) {
                    response = error("9001", "simulated failure");
                } else {
                    response = route(method, path, query, body);
                }
                byte[] payload = MAPPER.writeValueAsBytes(response);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, payload.length);
                exchange.getResponseBody().write(payload);
                exchange.close();
            });
        }

        private ObjectNode route(String method, String path, String query, JsonNode body) {
            if ("GET".equals(method) && "/user/tokens/verify".equals(path)) {
                return success(MAPPER.createObjectNode().put("status", "active"));
            }
            if ("GET".equals(method) && "/zones".equals(path)) {
                com.fasterxml.jackson.databind.node.ArrayNode resultArr = MAPPER.createArrayNode();
                String name = queryNameOf(query);
                if (name != null && zoneByName.containsKey(name)) {
                    resultArr.add(MAPPER.createObjectNode().put("id", zoneByName.get(name)));
                }
                return success(resultArr);
            }
            if (path.startsWith("/zones/") && path.endsWith("/dns_records")) {
                String zoneId = path.split("/")[2];
                if ("GET".equals(method)) {
                    return listRecords(zoneId, query);
                }
                if ("POST".equals(method)) {
                    ObjectNode record = ((ObjectNode) body).deepCopy();
                    record.put("id", "rec-" + records.size() + "-" + UUID.randomUUID().toString().substring(0, 8));
                    record.put("zone_id", zoneId);
                    records.add(record);
                    return success(record);
                }
            }
            // /zones/{zoneId}/dns_records/{recordId}
            if (path.startsWith("/zones/") && path.contains("/dns_records/")) {
                String[] segments = path.split("/");
                String zoneId = segments[2];
                String recordId = segments[4];
                if ("PUT".equals(method)) {
                    for (ObjectNode record : records) {
                        if (record.path("id").asText().startsWith(recordId)) {
                            record.put("type", body.path("type").asText());
                            record.put("name", body.path("name").asText());
                            record.put("content", body.path("content").asText());
                            record.put("proxied", body.path("proxied").asBoolean());
                            record.put("ttl", body.path("ttl").asLong());
                            return success(record);
                        }
                    }
                    return error("7003", "record not found");
                }
                if ("DELETE".equals(method)) {
                    records.removeIf(record -> record.path("id").asText().startsWith(recordId));
                    return success(null);
                }
            }
            return error("7000", "no route: " + method + " " + path);
        }

        private ObjectNode listRecords(String zoneId, String query) {
            String name = queryNameOf(query);
            String type = queryValueOf(query, "type");
            var result = MAPPER.createArrayNode();
            for (ObjectNode record : records) {
                if (!zoneId.equals(record.path("zone_id").asText())) continue;
                if (name != null && !name.equals(record.path("name").asText())) continue;
                if (type != null && !type.equals(record.path("type").asText())) continue;
                result.add(record.deepCopy());
            }
            return success(result);
        }

        private static String queryNameOf(String query) {
            return queryValueOf(query, "name");
        }

        private static String queryValueOf(String query, String key) {
            if (query == null) return null;
            for (String kv : query.split("&")) {
                String[] pair = kv.split("=", 2);
                if (key.equals(pair[0]) && pair.length == 2) {
                    return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
            return null;
        }

        private static ObjectNode success(JsonNode result) {
            ObjectNode response = MAPPER.createObjectNode().put("success", true);
            response.set("result", result == null ? MAPPER.createObjectNode() : result);
            return response;
        }

        private static ObjectNode error(String code, String message) {
            ObjectNode response = MAPPER.createObjectNode();
            response.put("success", false);
            response.putArray("errors")
                    .add(MAPPER.createObjectNode().put("code", Integer.parseInt(code)).put("message", message));
            return response;
        }

        void addZone(String zoneName, String zoneId) {
            zoneByName.put(zoneName, zoneId);
        }

        void addRecord(String zoneId, String type, String name, String content, boolean proxied) {
            ObjectNode record = MAPPER.createObjectNode();
            record.put("id", "rec-" + records.size());
            record.put("zone_id", zoneId);
            record.put("type", type);
            record.put("name", name);
            record.put("content", content);
            record.put("proxied", proxied);
            record.put("ttl", 300);
            records.add(record);
        }

        Request lastRequest(String method, String path) {
            for (int i = requests.size() - 1; i >= 0; i--) {
                Request r = requests.get(i);
                if (r.method().equals(method) && r.path().startsWith(path)) {
                    return r;
                }
            }
            return null;
        }

        List<Request> requests() {
            return requests;
        }

        List<ObjectNode> records() {
            return records;
        }

        void start() {
            server.start();
        }

        void stop() {
            server.stop(0);
        }

        int port() {
            return server.getAddress().getPort();
        }
    }
}
