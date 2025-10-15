package fan.summer.hmoneta.service.dns;

import fan.summer.hmoneta.common.enums.exception.dns.DnsExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.repository.dns.DnsProviderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DnsServiceTest {
    @Spy
    @InjectMocks
    private DnsService dnsService;
    @Mock
    private DnsProviderRepository dnsProviderRepository;

    @Test
    void should_return_dns_provider_list() {
        List<DnsProviderEntity> testDataLIst = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DnsProviderEntity entity = new DnsProviderEntity();
            entity.setId(String.valueOf(i));
            entity.setProviderName("provider" + i);
            entity.setProviderCode(String.valueOf(i));
            entity.setUpdatedAt(LocalDateTime.now());
            entity.setCreatedAt(LocalDateTime.now());
            testDataLIst.add(entity);
        }
        when(dnsProviderRepository.findAll()).thenReturn(testDataLIst);
        List<DnsProviderEntity> dnsProviderEntities = dnsService.queryAllDnsProvider();
        assertNotNull(dnsProviderEntities);
        assertEquals(10, dnsProviderEntities.size());

    }

    @Test
    void should_raise_exception_when_dns_resolve_group_empty_() {
        HMException ex = assertThrows(HMException.class,
                () -> dnsService.insertDnsResolveGroup(null));
        assertEquals(DnsExceptionEnum.DNS_GROUP_EMPTY_ERROR.getMessage(),
                ex.getMessage());
    }

}