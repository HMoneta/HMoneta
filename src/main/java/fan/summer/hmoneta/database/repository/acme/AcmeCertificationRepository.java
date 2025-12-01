package fan.summer.hmoneta.database.repository.acme;


import fan.summer.hmoneta.database.entity.acme.AcmeCertificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcmeCertificationRepository extends JpaRepository<AcmeCertificationEntity, Long> {
    

    void deleteByDomain(String domain);

    AcmeCertificationEntity findOneByDomain(String domain);
}
