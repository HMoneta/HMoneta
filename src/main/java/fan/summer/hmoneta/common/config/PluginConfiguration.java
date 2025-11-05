package fan.summer.hmoneta.common.config;

import org.pf4j.spring.SpringPluginManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.nio.file.Paths;

@Configuration
public class PluginConfiguration {

    /**
     * 创建 SpringPluginManager Bean
     * SpringPluginManager 支持 Spring 依赖注入和 ApplicationContext
     */
    @Bean
    public SpringPluginManager pluginManager() {
        // 指定插件目录,默认为 ./plugins
        SpringPluginManager pluginManager = new SpringPluginManager(
            Paths.get("plugins")
        );

        return pluginManager;
    }
}