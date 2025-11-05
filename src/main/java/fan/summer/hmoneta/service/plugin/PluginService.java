package fan.summer.hmoneta.service.plugin;


import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.pf4j.spring.SpringPluginManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件管理服务
 */
@Service
public class PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    private final SpringPluginManager pluginManager;
    private final Map<String, HmDnsProviderPlugin> dnsProviders = new ConcurrentHashMap<>();
    private final DnsProviderRepository dnsProviderRepository;

    @Autowired
    public PluginService(SpringPluginManager pluginManager, DnsProviderRepository dnsProviderRepository) {
        this.pluginManager = pluginManager;
        this.dnsProviderRepository = dnsProviderRepository;
    }

    @PostConstruct
    public void initPlugins() {
        long startTime = System.currentTimeMillis();
        logger.info("========== 主程序已启动，开始初始化系统插件 ==========");

        try {
            // 步骤1: 加载插件
            loadPlugins();

            // 步骤2: 启动插件
            startPlugins();

            // 步骤3: 初始化 DNS 插件
            initDnsProviders();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("========== 系统插件初始化完成 ==========");
            logger.info("总耗时: {}ms", duration);
            logger.info("成功加载插件数: {}", getStartedPlugins().size());
            logger.info("==========================================");

        } catch (Exception e) {
            logger.error("插件初始化失败", e);
        }
    }

    /**
     * 加载插件
     */
    private void loadPlugins() {
        logger.info("步骤1: 开始加载插件文件...");
        long startTime = System.currentTimeMillis();
        List<PluginWrapper> plugins = pluginManager.getPlugins();
        long duration = System.currentTimeMillis() - startTime;

        logger.info("插件文件加载完成，耗时: {}ms，发现 {} 个插件", duration, plugins.size());
        logger.info("已加载的插件列表:");

        int index = 1;
        for (PluginWrapper plugin : plugins) {
            logger.info("  {}. {} (版本: {}, 状态: {})",
                    index++, plugin.getPluginId(),
                    plugin.getDescriptor().getVersion(),
                    plugin.getPluginState());
        }
    }

    /**
     * 启动插件
     */
    private void startPlugins() {
        logger.info("步骤2: 开始启动插件...");

        pluginManager.startPlugins();

        List<PluginWrapper> startedPlugins = getStartedPlugins();
        List<PluginWrapper> failedPlugins = getFailedPlugins();

        logger.info("插件启动完成: 成功 {} 个, 失败 {} 个",
                startedPlugins.size(), failedPlugins.size());

        if (!failedPlugins.isEmpty()) {
            logger.warn("以下插件加载失败:");
            for (PluginWrapper plugin : failedPlugins) {
                logger.warn("  - {}: {} - {}",
                        plugin.getPluginId(),
                        plugin.getPluginState(),
                        plugin.getFailedException() != null ?
                                plugin.getFailedException().getMessage() : "未知错误");
            }
        }
    }

    /**
     * 初始化 DNS 提供者
     */
    private void initDnsProviders() {
        logger.info("步骤3: 开始初始化 DNS 插件...");

        // 获取所有 HmDnsProviderPlugin 扩展
        List<HmDnsProviderPlugin> providers = pluginManager.getExtensions(HmDnsProviderPlugin.class);

        logger.info("发现 {} 个 DNS Provider 插件", providers.size());

        int successCount = 0;
        if(!providers.isEmpty()) {
            // 记录DDNS供应商
            for (HmDnsProviderPlugin provider : providers) {
                logger.info("开始持久化{}插件", provider.providerName());
                DnsProviderEntity byProviderName = dnsProviderRepository.findByProviderName(provider.providerName());
                if (byProviderName == null) {
                    DnsProviderEntity dnsProviderEntity = new DnsProviderEntity();
                    dnsProviderEntity.setProviderName(provider.providerName());
                    dnsProviderEntity.setId(UUID.randomUUID().toString());
                    dnsProviderEntity.setProviderCode(provider.providerName() + UUID.randomUUID());
                    dnsProviderEntity.setCreatedAt(LocalDateTime.now());
                    dnsProviderEntity.setAuthenticateWay(provider.authenticateWay());
                    dnsProviderRepository.save(dnsProviderEntity);
                    logger.info("完成持久化{}插件", provider.providerName());
                }else {
                    logger.info("数据库已记录{}插件", provider.providerName());
                }
                dnsProviders.put(provider.providerName(), provider);
            }
        }
        logger.info("DNS 插件初始化完成，成功: {} 个", successCount);
    }

    /**
     * 获取已启动的插件
     */
    private List<PluginWrapper> getStartedPlugins() {
        return pluginManager.getPlugins(PluginState.STARTED);
    }

    /**
     * 获取启动失败的插件
     */
    private List<PluginWrapper> getFailedPlugins() {
        return pluginManager.getPlugins().stream()
                .filter(p -> p.getPluginState() != PluginState.STARTED)
                .toList();
    }

    /**
     * 获取 DNS 提供者
     */
    public HmDnsProviderPlugin getDnsProvider(String providerName) {
        return dnsProviders.get(providerName);
    }

    public void upLoadPlugin(InputStream plugin, Path filePath) throws IOException {
        Files.copy(plugin, filePath);

    }

    /**
     * 获取所有 DNS 提供者
     */
    public Map<String, HmDnsProviderPlugin> getAllDnsProviders() {
        return dnsProviders;
    }

    @PreDestroy
    public void cleanup() {
        logger.info("停止所有插件...");
        pluginManager.stopPlugins();
        logger.info("插件系统已关闭");
    }
}