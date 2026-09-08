package fan.summer.hmoneta.service.plugin;

import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import fan.summer.hmoneta.service.dns.cloudflare.CloudflareApiClient;
import fan.summer.hmoneta.service.dns.cloudflare.CloudflareDnsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pf4j.spring.SpringPluginManager;
import org.springframework.beans.factory.ObjectProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * PluginService 内置 DNS 提供商注册逻辑测试
 */
@ExtendWith(MockitoExtension.class)
class PluginServiceTest {

    @Mock
    private SpringPluginManager pluginManager;
    @Mock
    private DnsProviderRepository dnsProviderRepository;
    @Mock
    private ObjectProvider<HmDnsProviderPlugin> builtInProviders;

    private CloudflareDnsProvider cloudflareProvider() {
        // 不会触发网络调用：本测试不调用 authenticate()
        return new CloudflareDnsProvider(new CloudflareApiClient());
    }

    private PluginService newService() {
        return new PluginService(pluginManager, dnsProviderRepository, builtInProviders);
    }

    private void stubPluginManager() {
        when(pluginManager.getPlugins()).thenReturn(List.of());
        when(pluginManager.getPlugins(org.pf4j.PluginState.STARTED)).thenReturn(List.of());
        when(pluginManager.getExtensions(HmDnsProviderPlugin.class)).thenReturn(List.of());
    }

    @Test
    void should_register_and_persist_built_in_provider_when_absent() {
        CloudflareDnsProvider provider = cloudflareProvider();
        stubPluginManager();
        when(builtInProviders.stream()).thenReturn(Stream.of(provider));
        when(pluginManager.whichPlugin(CloudflareDnsProvider.class)).thenReturn(null);
        when(dnsProviderRepository.findByProviderName("Cloudflare")).thenReturn(null);

        PluginService service = newService();
        service.initPlugins();

        ArgumentCaptor<DnsProviderEntity> captor = ArgumentCaptor.forClass(DnsProviderEntity.class);
        verify(dnsProviderRepository).save(captor.capture());
        DnsProviderEntity saved = captor.getValue();
        assertEquals("Cloudflare", saved.getProviderName());
        assertEquals("built-in", saved.getPluginVersion());
        assertTrue(saved.getAuthenticateWay().contains("apiToken"));
        assertTrue(saved.getAuthenticateWay().contains("proxied"));

        assertSame(provider, service.getDnsProvider("Cloudflare"));
    }

    @Test
    void should_refresh_authenticate_way_for_existing_provider_row() {
        CloudflareDnsProvider provider = cloudflareProvider();
        stubPluginManager();
        when(builtInProviders.stream()).thenReturn(Stream.of(provider));
        when(pluginManager.whichPlugin(CloudflareDnsProvider.class)).thenReturn(null);

        // 数据库中已存在旧记录：版本相同但凭据键不含 proxied
        DnsProviderEntity existing = new DnsProviderEntity();
        existing.setId("cf-id");
        existing.setProviderName("Cloudflare");
        existing.setPluginVersion("built-in");
        existing.setAuthenticateWay(Set.of("apiToken"));
        existing.setCreatedAt(LocalDateTime.now());
        when(dnsProviderRepository.findByProviderName("Cloudflare")).thenReturn(existing);

        PluginService service = newService();
        service.initPlugins();

        ArgumentCaptor<DnsProviderEntity> captor = ArgumentCaptor.forClass(DnsProviderEntity.class);
        verify(dnsProviderRepository).save(captor.capture());
        assertTrue(captor.getValue().getAuthenticateWay().contains("proxied"),
                "新增凭据键 proxied 应刷新到数据库记录");
        assertSame(provider, service.getDnsProvider("Cloudflare"));
    }

    @Test
    void should_prefer_external_plugin_over_built_in_with_same_name() {
        CloudflareDnsProvider builtIn = cloudflareProvider();
        HmDnsProviderPlugin external = mock(HmDnsProviderPlugin.class);
        when(external.providerName()).thenReturn("Cloudflare");
        when(external.authenticateWay()).thenReturn(Set.of("apiToken"));

        when(pluginManager.getPlugins()).thenReturn(List.of());
        when(pluginManager.getPlugins(org.pf4j.PluginState.STARTED)).thenReturn(List.of());
        when(pluginManager.getExtensions(HmDnsProviderPlugin.class)).thenReturn(List.of(external));
        when(builtInProviders.stream()).thenReturn(Stream.of(builtIn));
        when(pluginManager.whichPlugin(any())).thenReturn(null);
        when(dnsProviderRepository.findByProviderName("Cloudflare")).thenReturn(null);

        PluginService service = newService();
        service.initPlugins();

        assertSame(external, service.getDnsProvider("Cloudflare"));
    }
}
