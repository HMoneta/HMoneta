package fan.summer.hmoneta.database.repository.dns;

import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DnsResolveUrlRepository extends JpaRepository<DnsResolveUrlEntity, String> {
}
