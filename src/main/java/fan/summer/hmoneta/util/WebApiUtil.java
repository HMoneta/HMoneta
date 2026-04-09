package fan.summer.hmoneta.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.util.Map;

/**
 * WebAPI工具类 - 使用Builder模式实现可配置的WebClient单例
 *
 * <p>提供简洁的HTTP API调用接口，支持GET/POST/PUT/DELETE等常用HTTP方法。
 * 内置SSL证书验证跳过功能（用于本地开发环境），支持自定义请求头和查询参数。
 *
 * <p>使用示例：
 * <pre>
 * // 基础用法
 * WebApiUtil util = WebApiUtil.build("https://api.example.com");
 * String result = util.api(HttpMethod.GET, "/users", null, null, String.class).block();
 *
 * // 带请求头和查询参数
 * Map&lt;String, String&gt; headers = Map.of("Authorization", "Bearer token");
 * Map&lt;String, String&gt; params = Map.of("page", "1", "size", "10");
 * util.api(HttpMethod.GET, "/users", params, null, UserList.class).block();
 * </pre>
 *
 * @author phoebej
 * @version 1.01
 * @Date 2026/1/15
 */
public class WebApiUtil {

    private static final Logger log = LoggerFactory.getLogger(WebApiUtil.class);

    private final WebClient webClient;

    private WebApiUtil(String baseUrl, Map<String, String> headers) {
        this(baseUrl, headers, false);
    }

    private WebApiUtil(String baseUrl, Map<String, String> headers, boolean ignoreSsl) {
        WebClient.Builder clientBuilder = WebClient.builder().baseUrl(baseUrl);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(clientBuilder::defaultHeader);
        }

        if (ignoreSsl) {
            HttpClient httpClient = HttpClient.create()
                    .secure(sslContextSpec -> sslContextSpec.sslContext(buildInsecureSslContext()));
            clientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient));
        }

        this.webClient = clientBuilder.build();

        // 记录初始化信息
        if (log.isInfoEnabled()) {
            log.info("[WebApiUtil] 初始化 WebClient - baseUrl: {}, ignoreSsl: {}, headers数量: {}",
                    baseUrl, ignoreSsl, headers != null ? headers.size() : 0);
            if (ignoreSsl) {
                log.warn("[WebApiUtil] SSL证书验证已禁用，仅用于本地开发环境！");
            }
        }
    }

    private static SslContext buildInsecureSslContext() {
        try {
            return SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException("Failed to build insecure SSL context", e);
        }
    }

    /**
     * 构建 WebApiUtil 实例（带默认请求头）
     *
     * @param baseUrl API基础URL，如 "https://api.example.com"
     * @param headers 默认请求头，可为 null
     * @return WebApiUtil 实例
     */
    public static WebApiUtil build(String baseUrl, Map<String, String> headers) {
        return new WebApiUtil(baseUrl, headers);
    }

    /**
     * 构建 WebApiUtil 实例（无默认请求头）
     *
     * @param baseUrl API基础URL，如 "https://api.example.com"
     * @return WebApiUtil 实例
     */
    public static WebApiUtil build(String baseUrl) {
        return new WebApiUtil(baseUrl, null);
    }

    /**
     * 构建忽略SSL证书验证的 WebApiUtil 实例
     *
     * <p>警告：此方法仅适用于本地开发环境，禁用SSL验证会带来安全风险！
     *
     * @param baseUrl API基础URL
     * @param headers 默认请求头，可为 null
     * @return WebApiUtil 实例
     */
    public static WebApiUtil buildIgnoreSsl(String baseUrl, Map<String, String> headers) {
        return new WebApiUtil(baseUrl, headers, true);
    }

    /**
     * 执行 HTTP API 请求（返回类型为 Class）
     *
     * @param httpMethod   HTTP方法，如 HttpMethod.GET、HttpMethod.POST
     * @param uri          请求路径，如 "/users"、"api/data"
     * @param queryParams  查询参数，可为 null
     * @param body         请求体对象，可为 null（GET请求通常为null）
     * @param respObj      响应体的 Java 类型（Class）
     * @param <T>          响应体类型
     * @return 包含响应体的 Mono 对象
     */
    public <T> Mono<T> api(HttpMethod httpMethod, String uri,
                           Map<String, String> queryParams,
                           Object body,
                           Class<T> respObj) {
        // 构建 URI 和查询参数
        String fullUri = buildFullUri(uri, queryParams);

        if (log.isDebugEnabled()) {
            log.debug("[WebApiUtil] HTTP请求 - method: {}, uri: {}, queryParams: {}, body: {}",
                    httpMethod, fullUri, queryParams, body != null ? body : "无");
        }

        try {
            WebClient.RequestBodyUriSpec requestSpec = webClient.method(httpMethod);

            WebClient.RequestBodySpec bodySpec = requestSpec.uri(uriBuilder -> {
                if (uri != null && !uri.isEmpty()) {
                    uriBuilder.path(uri);
                }
                if (queryParams != null) {
                    queryParams.forEach(uriBuilder::queryParam);
                }
                return uriBuilder.build();
            });

            // 设置请求体
            WebClient.RequestHeadersSpec<?> headersSpec;
            if (body != null) {
                headersSpec = bodySpec.bodyValue(body);
            } else {
                headersSpec = bodySpec;
            }

            Mono<T> resultMono = headersSpec.retrieve().bodyToMono(respObj);

            // 添加响应日志和错误日志
            return resultMono
                    .doOnSuccess(response -> {
                        if (log.isDebugEnabled()) {
                            log.debug("[WebApiUtil] HTTP响应成功 - method: {}, uri: {}, response: {}",
                                    httpMethod, fullUri, response);
                        }
                    })
                    .doOnError(error -> {
                        log.error("[WebApiUtil] HTTP请求失败 - method: {}, uri: {}, error: {}",
                                httpMethod, fullUri, error.getMessage(), error);
                    });
        } catch (Exception e) {
            log.error("[WebApiUtil] HTTP请求异常 - method: {}, uri: {}, error: {}",
                    httpMethod, fullUri, e.getMessage(), e);
            return Mono.error(e);
        }
    }

    /**
     * 构建完整的URI字符串（用于日志记录）
     */
    private String buildFullUri(String uri, Map<String, String> queryParams) {
        StringBuilder sb = new StringBuilder();
        if (uri != null) {
            sb.append(uri);
        }
        if (queryParams != null && !queryParams.isEmpty()) {
            sb.append("?");
            queryParams.forEach((key, value) -> sb.append(key).append("=").append(value).append("&"));
            sb.deleteCharAt(sb.length() - 1); // 删除最后一个 &
        }
        return sb.toString();
    }

    /**
     * 执行 HTTP API 请求（返回类型为 ParameterizedTypeReference，支持泛型）
     *
     * <p>适用于需要保留泛型类型的场景，如返回 List&lt;User&gt;、Map&lt;String, Object&gt; 等复杂类型。
     *
     * <p>示例：
     * <pre>
     * ParameterizedTypeReference&lt;List&lt;User&gt;&gt; typeRef = new ParameterizedTypeReference&lt;List&lt;User&gt;&gt;() {};
     * List&lt;User&gt; users = util.api(HttpMethod.GET, "/users", null, null, typeRef).block();
     * </pre>
     *
     * @param httpMethod   HTTP方法
     * @param uri          请求路径
     * @param queryParams  查询参数，可为 null
     * @param body         请求体对象，可为 null
     * @param typeRef      响应体的参数化类型引用
     * @param <T>          响应体类型
     * @return 包含响应体的 Mono 对象
     */
    public <T> Mono<T> api(HttpMethod httpMethod, String uri,
                           Map<String, String> queryParams,
                           Object body,
                           ParameterizedTypeReference<T> typeRef) {
        // 构建 URI 和查询参数
        String fullUri = buildFullUri(uri, queryParams);

        if (log.isDebugEnabled()) {
            log.debug("[WebApiUtil] HTTP请求(泛型) - method: {}, uri: {}, queryParams: {}, body: {}, typeRef: {}",
                    httpMethod, fullUri, queryParams, body != null ? body : "无", typeRef.getType());
        }

        try {
            WebClient.RequestBodyUriSpec requestSpec = webClient.method(httpMethod);

            WebClient.RequestBodySpec bodySpec = requestSpec.uri(uriBuilder -> {
                if (uri != null && !uri.isEmpty()) {
                    uriBuilder.path(uri);
                }
                if (queryParams != null) {
                    queryParams.forEach(uriBuilder::queryParam);
                }
                return uriBuilder.build();
            });

            WebClient.RequestHeadersSpec<?> headersSpec;
            if (body != null) {
                headersSpec = bodySpec.bodyValue(body);
            } else {
                headersSpec = bodySpec;
            }

            Mono<T> resultMono = headersSpec.retrieve().bodyToMono(typeRef);

            // 添加响应日志和错误日志
            return resultMono
                    .doOnSuccess(response -> {
                        if (log.isDebugEnabled()) {
                            log.debug("[WebApiUtil] HTTP响应成功(泛型) - method: {}, uri: {}, response: {}",
                                    httpMethod, fullUri, response);
                        }
                    })
                    .doOnError(error -> {
                        log.error("[WebApiUtil] HTTP请求失败(泛型) - method: {}, uri: {}, error: {}",
                                httpMethod, fullUri, error.getMessage(), error);
                    });
        } catch (Exception e) {
            log.error("[WebApiUtil] HTTP请求异常(泛型) - method: {}, uri: {}, error: {}",
                    httpMethod, fullUri, e.getMessage(), e);
            return Mono.error(e);
        }
    }

}
