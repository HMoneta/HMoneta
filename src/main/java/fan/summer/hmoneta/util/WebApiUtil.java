package fan.summer.hmoneta.util;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * WebAPI工具类 - 使用Builder模式实现可配置的WebClient单例
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/15
 */
public abstract class WebApiUtil {
    private static WebClient webClient;

    public static boolean isNotBuild() {
        return webClient == null;
    }

    public static synchronized void build(String baseUrl, Map<String, String> headers) {
        WebClient.Builder clientBuilder = WebClient.builder().baseUrl(baseUrl);
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(clientBuilder::defaultHeader);
        }
        webClient = clientBuilder.build();
    }

    public static synchronized void build(String baseUrl) {
        build(baseUrl, null);
    }

    // Api访问方法
    public static <T> Mono<T> api(HttpMethod httpMethod, Class<T> respObj) {
        if (isNotBuild()) {
            throw new IllegalStateException("请先调用 build() 初始化");
        }
        WebClient.RequestBodyUriSpec requestSpec = switch (httpMethod.name()) {
            case "POST", "PUT", "PATCH", "GET", "DELETE" -> webClient.method(httpMethod);
            default -> throw new IllegalArgumentException("不支持: " + httpMethod);
        };
        return requestSpec.retrieve().bodyToMono(respObj);
    }

}
