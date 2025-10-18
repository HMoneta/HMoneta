package fan.summer.hmoneta.common.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

/**
 * Api
 */
@Aspect
@Component
public class ApiAspect {
    private static final Logger LOG = LoggerFactory.getLogger(ApiAspect.class);
    private ObjectMapper objectMapper;

    @Autowired
    public ApiAspect(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Pointcut("execution(public * fan.summer..*Controller.*(..))")
    public void apiControllerAspect() {
    }

    /**
     * 环绕通知：记录请求和响应详情
     */
    @Around("apiControllerAspect()")
    public Object doAround(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 打印请求信息
        logRequest(proceedingJoinPoint);

        Object result = null;
        try {
            // 执行目标方法
            result = proceedingJoinPoint.proceed();

            // 打印响应信息
            logResponse(result, startTime);

            return result;

        } catch (Throwable e) {
            // 打印异常信息
            logException(e, startTime);
            throw e;
        }
    }

    /**
     * 记录请求信息
     */
    private void logRequest(ProceedingJoinPoint joinPoint) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                LOG.warn("无法获取请求上下文");
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            Signature signature = joinPoint.getSignature();
            String methodName = signature.getName();

            LOG.info("==================== Api请求详情(开始)  ====================");
            LOG.info("请求地址: {} {}", request.getRequestURL(), request.getMethod());
            LOG.info("类名方法: {}.{}", signature.getDeclaringTypeName(), methodName);
            LOG.info("远程地址: {}", request.getRemoteAddr());

            // 处理请求参数
            String argsJson = getArgumentsJson(joinPoint.getArgs());
            LOG.info("请求参数: {}", argsJson);

        } catch (Exception e) {
            LOG.error("记录请求信息失败", e);
        } finally {
            LOG.info("==================== Api请求详情(结束) ====================");
        }
    }

    /**
     * 记录响应信息
     */
    private void logResponse(Object result, long startTime) {
        long duration = System.currentTimeMillis() - startTime;
        try {
            String resultJson = objectMapper.writeValueAsString(result);
            LOG.info("==================== Api响应详情(开始) ====================");
            LOG.info("响应参数: {}", resultJson);


        } catch (JsonProcessingException e) {
            LOG.error("响应参数 JSON 序列化失败", e);
        } finally {
            LOG.info("执行耗时: {} ms", duration);
            LOG.info("==================== Api响应详情(结束) ====================");
        }
    }

    /**
     * 记录异常信息
     */
    private void logException(Throwable e, long startTime) {
        long duration = System.currentTimeMillis() - startTime;

        LOG.error("==================== API 执行异常 ====================");
        LOG.error("异常类型: {}", e.getClass().getName());
        LOG.error("异常信息: {}", e.getMessage());
        LOG.error("执行耗时: {} ms", duration);
        LOG.error("========================================================", e);
    }

    /**
     * 将参数数组转换为 JSON 字符串
     * 过滤掉不可序列化的对象（ServletRequest、ServletResponse、MultipartFile）
     */
    private String getArgumentsJson(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest
                    || args[i] instanceof ServletResponse
                    || args[i] instanceof MultipartFile) {
                // 对于不可序列化的对象，记录其类型
                arguments[i] = "[" + args[i].getClass().getSimpleName() + "]";
            } else {
                arguments[i] = args[i];
            }
        }

        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            LOG.warn("参数 JSON 序列化失败: {}", e.getMessage());
            return "[JSON 序列化失败]";
        }
    }


}
