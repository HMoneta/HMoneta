package fan.summer.hmoneta.service.plugin;

import fan.summer.hmoneta.HMonetaApplication;
import fan.summer.plugins.common.HMPlugin;
import fan.summer.plugins.common.enums.PluginType;
import fan.summer.plugins.core.dns.DNSProviderPlugin;
import lombok.Getter;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PropertiesPluginDescriptorFinder;
import org.pf4j.spring.SpringPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.Map;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/9/24
 */
@Service
public class PluginService {

    private static final Logger LOG = LoggerFactory.getLogger(PluginService.class);

    @Getter
    SpringPluginManager pluginManager = new SpringPluginManager(Paths.get("plugins")) {
        @Override
        protected PluginDescriptorFinder createPluginDescriptorFinder() {
            // 使用 plugin.properties 描述文件
            return new PropertiesPluginDescriptorFinder();
        }
    };
    @Getter
    private Map<PluginType, HMPlugin> pluginMap;

    public void initPlugins(){
        LOG.info("-------主程序已启动，开始初始化系统插件---------");
        // 加载并启动插件
        pluginManager.loadPlugins();
        pluginManager.startPlugins();

    }

}
