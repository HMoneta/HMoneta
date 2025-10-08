package fan.summer.hmoneta.common.interceptor;

import fan.summer.hmoneta.database.entity.user.UserEntity;
import fan.summer.hmoneta.util.JwtUtil;
import fan.summer.hmoneta.util.ObjectUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;


public class ApiInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ApiInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            logger.info("===============Api访问鉴权===============");
            String hmToken = request.getHeader("HMToken");
            if (ObjectUtil.isNotEmpty(hmToken)) {
                logger.info(">>>>>>>>>>验证HMToken:{}合法性>>>>>>>>>>", hmToken);
                JwtUtil jwtUtil = new JwtUtil();
                if (jwtUtil.validate(hmToken)) {
                    logger.info("<<<<<<<<<<<HMToken合法<<<<<<<<<<<");
                    logger.info(">>>>>>>>>>开始设置环境变量>>>>>>>>>>");
                    UserEntity user = jwtUtil.parseTokenToBean(hmToken, UserEntity.class);
                    logger.info("<<<<<<<<<<<环境变量设置完成<<<<<<<<<<<");
                    return true;
                } else {
                    logger.error("<<<<<<<<<<<HMToken非法，拒绝访问Api<<<<<<<<<<<");
                    return false;
                }

            } else {
                logger.error("<<<<<<<<<<<HMToken为空，拒绝访问Api<<<<<<<<<<<");
                return false;
            }

        } finally {
            logger.info("===============完成Api访问鉴权===============");
        }
    }

}
