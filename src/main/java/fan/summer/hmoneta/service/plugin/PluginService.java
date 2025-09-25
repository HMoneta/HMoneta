package fan.summer.hmoneta.service.plugin;


import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.util.ObjectUtil;
import fan.summer.plugins.core.dns.DNSProviderPlugin;
import lombok.Getter;
import org.pf4j.PluginDescriptorFinder;
import org.pf4j.PropertiesPluginDescriptorFinder;
import org.pf4j.spring.SpringPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PluginService类是用于管理和操作系统插件的核心服务。它负责加载、启动、初始化和停止插件，并提供方法来获取关于已加载插件的信息。
 * 该服务依赖于Spring框架提供的功能，以确保插件的生命周期与应用程序的生命周期保持一致。
 */
@Service
public class PluginService implements InitializingBean {

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
    private Map<String, DNSProviderPlugin> dnsPluginMap;

    private final Log4jPluginLogger logger;

    private final DnsProviderRepository dnsProviderRepository;

    @Autowired
    PluginService(DnsProviderRepository dnsProviderRepository,Log4jPluginLogger logger) {
        this.dnsProviderRepository = dnsProviderRepository;
        this.logger = logger;
    }

    @PostConstruct
    public void initPlugins() {
        LOG.info("-------主程序已启动，开始初始化系统插件---------");

        try {
            // 1. 加载并启动插件
            pluginManager.loadPlugins();
            pluginManager.startPlugins();

            // 2. 初始化所有类型的Plugin
            loadPluginsByType();

            LOG.info("-------系统插件初始化完成，共加载{}个插件---------", dnsPluginMap.size());

        } catch (Exception e) {
            LOG.error("插件初始化失败", e);
            throw new RuntimeException("插件初始化失败", e);
        }
    }

    /**
     * 根据插件类型加载插件实例
     */
    private void loadPluginsByType() {
        List<DNSProviderPlugin> extensions = pluginManager.getExtensions(DNSProviderPlugin.class);
        dnsPluginMap = new HashMap<>();
        int index = 0;
        extensions.forEach(dnsPlugin -> {
            dnsPlugin.init(logger);
            String name = dnsPlugin.providerName();
            DnsProviderEntity byProviderName = dnsProviderRepository.findByProviderName(name);
            if(ObjectUtil.isEmpty(byProviderName)){
                LOG.info("开始初始化第{}个插件，插件名称为:{}", index + 1, name);
                // 设置供应商信息
                DnsProviderEntity provider = new DnsProviderEntity();
                String id = UUID.randomUUID().toString();
                provider.setId(id);
                provider.setProviderName(name);
                provider.setProviderCode(name + id);
                provider.setCreatedAt(LocalDateTime.now());
                provider.setUpdatedAt(LocalDateTime.now());
                dnsProviderRepository.save(provider);
            }
            dnsPluginMap.put(dnsPlugin.providerName(), dnsPlugin);
        });
        logger.info("-------完成系统初始化---------");
    }

    /**
     * 获取指定类型的插件
     */
//    public <T extends HMPlugin> T getPlugin(PluginType pluginType, Class<T> pluginClass) {
//        HMPlugin plugin = pluginMap.get(pluginType);
//        if (plugin != null && pluginClass.isInstance(plugin)) {
//            return pluginClass.cast(plugin);
//        }
//        return null;
//    }

//    /**
//     * 获取DNS插件的便捷方法
//     */
//    public DNSProviderPlugin getDNSPlugin() {
//        return getPlugin(PluginType.DNS_PLUGIN, DNSProviderPlugin.class);
//    }
//
//    /**
//     * 检查指定类型的插件是否已加载
//     */
//    public boolean isPluginLoaded(PluginType pluginType) {
//        return pluginMap.containsKey(pluginType);
//    }
//
//    /**
//     * 获取所有已加载的插件信息
//     */
//    public Map<PluginType, String> getLoadedPluginsInfo() {
//        Map<PluginType, String> info = new HashMap<>();
//        pluginMap.forEach((type, plugin) -> {
//            info.put(type, plugin.getClass().getName());
//        });
//        return info;
//    }

    /**
     * 停止并卸载所有插件
     */
    public void shutdown() {
        LOG.info("开始停止所有插件...");
        try {
            // 清理插件映射
            dnsPluginMap.clear();

            // 停止并卸载插件
            pluginManager.stopPlugins();
            pluginManager.unloadPlugins();

            LOG.info("所有插件已停止");
        } catch (Exception e) {
            LOG.error("停止插件时发生错误", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        LOG.debug("PluginService初始化完成");
    }
//
//    /**
//     * 可选：插件初始化接口
//     * 插件可以实现这个接口来定义自己的初始化逻辑
//     */
//    public interface InitializablePlugin {
//        void initialize() throws Exception;
//    }
}