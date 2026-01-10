package fan.summer.hmoneta.common.config;


import fan.summer.hmoneta.common.interceptor.ApiInterceptor;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
public class HMInterceptor implements WebMvcConfigurer {


    @Resource
    private ApiInterceptor apiInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/hm/user/login",
                        "/hm/user/valid", "/hm/user/login/status");
    }
}
