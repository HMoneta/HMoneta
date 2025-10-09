package fan.summer.hmoneta.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import fan.summer.hmoneta.database.entity.user.UserEntity;
import fan.summer.hmoneta.util.JwtUtil;
import fan.summer.hmoneta.util.ObjectUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiInterceptor implements HandlerInterceptor {
    @Resource
    private final JwtUtil jwtUtil = new JwtUtil();
    private static final Logger logger = LoggerFactory.getLogger(ApiInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            logger.info("===============Api访问鉴权===============");
            String hmToken = request.getHeader("HMToken");
            if (ObjectUtil.isNotEmpty(hmToken)) {
                logger.info(">>>>>>>>>>验证HMToken:{}合法性>>>>>>>>>>", hmToken);
                if (jwtUtil.validate(hmToken)) {
                    logger.info("<<<<<<<<<<<HMToken合法<<<<<<<<<<<");
                    logger.info(">>>>>>>>>>开始设置环境变量>>>>>>>>>>");
                    UserEntity user = jwtUtil.parseTokenToBean(hmToken, UserEntity.class);
                    logger.info("<<<<<<<<<<<环境变量设置完成<<<<<<<<<<<");
                    return true;
                } else {
                    logger.error("<<<<<<<<<<<HMToken非法，拒绝访问Api<<<<<<<<<<<");
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "HMToken非法或已过期");
                    return false;
                }

            } else {
                logger.error("<<<<<<<<<<<HMToken为空，拒绝访问Api<<<<<<<<<<<");
                sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "请求头缺少HMToken");
                return false;
            }

        }catch (Exception e) {
            logger.error("<<<<<<<<<<<鉴权过程发生异常<<<<<<<<<<<", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "鉴权失败：" + e.getMessage());
            return false;
        }
        finally {
            logger.info("===============完成Api访问鉴权===============");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        result.put("timestamp", System.currentTimeMillis());

        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

}
