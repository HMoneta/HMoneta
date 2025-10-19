package fan.summer.hmoneta.database.repository.dns;

import fan.summer.hmoneta.database.entity.dns.DnsResolveGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DnsResolveGroupRepository extends JpaRepository<DnsResolveGroupEntity, String> {
}
