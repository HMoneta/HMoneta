package fan.summer.hmoneta.util;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
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
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/15
 */
public class WebApiUtil {
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

    public static WebApiUtil build(String baseUrl, Map<String, String> headers) {
        return new WebApiUtil(baseUrl, headers);
    }

    public static WebApiUtil build(String baseUrl) {
        return new WebApiUtil(baseUrl, null);
    }

    /**
     * 构建忽略SSL证书验证的WebApiUtil
     */
    public static WebApiUtil buildIgnoreSsl(String baseUrl, Map<String, String> headers) {
        return new WebApiUtil(baseUrl, headers, true);
    }

    public <T> Mono<T> api(HttpMethod httpMethod, String uri,
                           Map<String, String> queryParams,
                           Object body,
                           Class<T> respObj) {
        WebClient.RequestBodyUriSpec requestSpec = webClient.method(httpMethod);

        // 构建 URI 和查询参数
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

        return headersSpec.retrieve().bodyToMono(respObj);
    }

    /**
     * 支持泛型的API调用，使用ParameterizedTypeReference保留泛型类型
     */
    public <T> Mono<T> api(HttpMethod httpMethod, String uri,
                           Map<String, String> queryParams,
                           Object body,
                           ParameterizedTypeReference<T> typeRef) {
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

        return headersSpec.retrieve().bodyToMono(typeRef);
    }

}
