package fan.summer.hmoneta.database.repository.acme;


import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcmeCertificationRepository extends JpaRepository<AcmeCertificationEntity, Long> {
    

    List<AcmeCertificationEntity> findByDomain(String domain);

    void deleteByDomain(String domain);
}
