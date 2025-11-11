package fan.summer.hmoneta.database.repository.dns;

import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DnsResolveUrlRepository extends JpaRepository<DnsResolveUrlEntity, String> {
    List<DnsResolveUrlEntity> findAllByGroupId(String id);

    DnsResolveUrlEntity findOneByUrl(String url);
}
