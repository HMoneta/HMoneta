package fan.summer.hmoneta.database.repository.dns;

import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DnsProviderRepository extends JpaRepository<DnsProviderEntity, String> {
    DnsProviderEntity findByProviderName(String provider);
}
