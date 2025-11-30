package fan.summer.hmoneta.database.repository.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeAsyncLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcmeAsyncLogRepository extends JpaRepository<AcmeAsyncLogEntity, String> {

}
