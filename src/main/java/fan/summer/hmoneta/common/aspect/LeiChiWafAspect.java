package fan.summer.hmoneta.common.aspect;

import fan.summer.hmoneta.common.enums.exception.waf.LeiChiExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.service.waf.LeiChiWafService;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 雷池WAF服务初始化检查切面
 * 在执行 LeiChiWafService 方法前自动检查并初始化 WebApiUtil
 */
@Aspect
@Component
@Log4j2
public class LeiChiWafAspect {

    private static final String LEI_CHI_SERVICE = "execution(* fan.summer.hmoneta.service.waf.LeiChiWafService.*(..))";

    private final LeiChiWafService leiChiWafService;

    @Autowired
    public LeiChiWafAspect(LeiChiWafService leiChiWafService) {
        this.leiChiWafService = leiChiWafService;
    }

    @Pointcut(LEI_CHI_SERVICE + " && !execution(* fan.summer.hmoneta.service.waf.LeiChiWafService.initWebApiUtil(..))" +
            " && !execution(* fan.summer.hmoneta.service.waf.LeiChiWafService.isWebApiUtilInit(..))" +
            " && !execution(* fan.summer.hmoneta.service.waf.LeiChiWafService.queryLeiChiToken(..))" +
            " && !execution(* fan.summer.hmoneta.service.waf.LeiChiWafService.insertLeiChiApiToken(..))")
    public void leiChiServicePointcut() {
    }

    @Around("leiChiServicePointcut()")
    public Object checkInit(ProceedingJoinPoint joinPoint) throws Throwable {
        leiChiWafService.initWebApiUtil();

        if (!leiChiWafService.isWebApiUtilInit()) {
            log.error("雷池API未初始化成功，无法执行方法: {}", joinPoint.getSignature().getName());
            throw new HMException(LeiChiExceptionEnum.LEICHI_API_NOT_INIT);
        }

        return joinPoint.proceed();
    }
}
