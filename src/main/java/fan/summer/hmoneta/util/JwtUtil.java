package fan.summer.hmoneta.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 增强版JWT工具类 - 支持注解排除敏感字段
 * 提供JWT（JSON Web Token）的生成、解析和验证功能
 */
@Slf4j
@Component
public class JwtUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${jwt.secret:povTvh!U7e9aJCwLUmp^qIVRCrgOAa=a}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400}")
    private Long jwtExpiration;

    // 预定义的敏感字段列表
    private static final Set<String> DEFAULT_SENSITIVE_FIELDS = Set.of(
            "password", "pwd", "passwd", "secret",
            "salt", "token", "refreshToken", "accessToken",
            "privateKey", "secretKey", "apiKey", "key",
            "credential", "auth", "authorization", "hash"
    );

    /**
     * JWT排除字段注解 - 标记不应包含在JWT中的字段
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface JwtExclude {
        String reason() default "敏感信息";
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 生成token - 基础版本，自动过滤敏感字段
     */
    public <T> String createTokenForObject(T data) {
        return createTokenForObject(data, new String[0]);
    }

    /**
     * 生成token - 支持自定义排除字段
     *
     * @param data 待需要生成token的对象
     * @param excludeFields 额外需要排除的字段
     * @param <T>  待需要生成token的对象的类型
     * @return token
     */
    public <T> String createTokenForObject(T data, String... excludeFields) {
        try {
            Map<String, Object> payload = buildPayload(data, excludeFields);

            Date now = new Date();
            Date expiry = new Date(now.getTime() + jwtExpiration * 1000);

            String token = Jwts.builder()
                    .claims(payload)
                    .subject("HMToken")
                    .issuedAt(now)
                    .expiration(expiry)
                    .signWith(getSecretKey())
                    .compact();

            log.debug("[JwtUtil] Token创建成功，载荷字段: {}, 过期时间: {}",
                    payload.keySet(), expiry);
            return token;

        } catch (Exception e) {
            log.error("[JwtUtil] 创建Token失败", e);
            throw new JwtCreationException("创建JWT Token失败", e);
        }
    }

    /**
     * 构建JWT载荷，过滤敏感字段
     */
    private <T> Map<String, Object> buildPayload(T data, String... extraExcludes) {
        // 使用Jackson将对象转换为Map
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = new HashMap<>(
                OBJECT_MAPPER.convertValue(data, Map.class)
        );

        // 获取所有需要排除的字段
        Set<String> fieldsToExclude = new HashSet<>();

        // 1. 添加默认敏感字段
        fieldsToExclude.addAll(DEFAULT_SENSITIVE_FIELDS);

        // 2. 添加自定义排除字段
        if (extraExcludes != null) {
            fieldsToExclude.addAll(Arrays.asList(extraExcludes));
        }

        // 3. 通过反射检查@JwtExclude注解
        fieldsToExclude.addAll(getAnnotatedExcludeFields(data.getClass()));

        // 4. 移除匹配敏感模式的字段
        Set<String> sensitiveKeys = new HashSet<>();
        for (String key : payload.keySet()) {
            if (isSensitiveField(key, fieldsToExclude)) {
                sensitiveKeys.add(key);
            }
        }

        // 移除敏感字段
        sensitiveKeys.forEach(payload::remove);

        if (!sensitiveKeys.isEmpty()) {
            log.debug("[JwtUtil] 已移除敏感字段: {}", sensitiveKeys);
        }

        return payload;
    }

    /**
     * 获取使用@JwtExclude注解标记的字段
     */
    private Set<String> getAnnotatedExcludeFields(Class<?> clazz) {
        Set<String> excludeFields = new HashSet<>();

        // 遍历所有字段（包括父类）
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(JwtExclude.class)) {
                    excludeFields.add(field.getName());
                    JwtExclude annotation = field.getAnnotation(JwtExclude.class);
                    log.debug("[JwtUtil] 发现@JwtExclude字段: {} (原因: {})",
                            field.getName(), annotation.reason());
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        return excludeFields;
    }

    /**
     * 判断字段是否为敏感字段
     */
    private boolean isSensitiveField(String fieldName, Set<String> excludeFields) {
        String lowerKey = fieldName.toLowerCase();

        // 1. 精确匹配
        if (excludeFields.contains(fieldName) || excludeFields.contains(lowerKey)) {
            return true;
        }

        // 2. 模糊匹配敏感关键词
        return lowerKey.contains("password") ||
                lowerKey.contains("secret") ||
                lowerKey.contains("token") ||
                lowerKey.contains("key") ||
                lowerKey.contains("salt") ||
                lowerKey.contains("hash") ||
                lowerKey.endsWith("pwd") ||
                lowerKey.startsWith("auth") ||
                lowerKey.contains("credential");
    }

    /**
     * 解析JWT Token获取Claims
     */
    public Claims parseToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new JwtValidationException("JWT Token不能为空");
        }

        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("[JwtUtil] JWT Token已过期");
            throw new JwtValidationException("JWT Token已过期", e);
        } catch (UnsupportedJwtException e) {
            log.warn("[JwtUtil] 不支持的JWT Token");
            throw new JwtValidationException("不支持的JWT Token", e);
        } catch (MalformedJwtException e) {
            log.warn("[JwtUtil] 格式错误的JWT Token");
            throw new JwtValidationException("格式错误的JWT Token", e);
        } catch (SecurityException | SignatureException e) {
            log.warn("[JwtUtil] JWT Token签名验证失败");
            throw new JwtValidationException("JWT Token签名验证失败", e);
        } catch (IllegalArgumentException e) {
            log.warn("[JwtUtil] JWT Token参数错误");
            throw new JwtValidationException("JWT Token参数错误", e);
        }
    }

    /**
     * 直接从Token解析为指定类型的对象
     */
    public <T> T parseTokenToBean(String token, Class<T> clazz) {
        try {
            Claims claims = parseToken(token);
            return OBJECT_MAPPER.convertValue(claims, clazz);
        } catch (IllegalArgumentException e) {
            log.error("[JwtUtil] 解析JWT Token为Bean失败: {}", e.getMessage());
            throw new JwtValidationException("解析JWT Token为Bean失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[JwtUtil] 解析JWT Token为Bean失败", e);
            throw new JwtValidationException("解析JWT Token为Bean失败", e);
        }
    }

    /**
     * 校验token合法性
     */
    public boolean validate(String token) {
        try {
            parseToken(token);
            log.debug("[JwtUtil] Token校验通过");
            return true;
        } catch (Exception e) {
            log.debug("[JwtUtil] Token校验失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取JSONObject
     */
    public JsonNode getJSONObject(String token) {
        try {
            Claims claims = parseToken(token);
            return OBJECT_MAPPER.valueToTree(claims);
        } catch (Exception e) {
            log.error("[JwtUtil] 解析JWT Token为JSONObject失败", e);
            throw new JwtValidationException("解析JWT Token失败", e);
        }
    }

    /**
     * 从token中获取subject
     */
    public String getSubject(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    /**
     * 判断token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * JWT创建异常
     */
    public static class JwtCreationException extends RuntimeException {
        public JwtCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * JWT验证异常
     */
    public static class JwtValidationException extends RuntimeException {
        public JwtValidationException(String message) {
            super(message);
        }

        public JwtValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}