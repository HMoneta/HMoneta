package fan.summer.hmoneta.database.repository.log;

import fan.summer.hmoneta.database.entity.log.AppLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppLogRepository extends JpaRepository<AppLogEntity, Long> {

    @Query("SELECT l FROM AppLogEntity l WHERE " +
           "(:startTime IS NULL OR l.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR l.timestamp <= :endTime) AND " +
           "(:level IS NULL OR l.level = :level) AND " +
           "(:service IS NULL OR l.service = :service) " +
           "ORDER BY l.timestamp DESC")
    Page<AppLogEntity> findByFilters(
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime,
            @Param("level") String level,
            @Param("service") String service,
            Pageable pageable);

    List<AppLogEntity> findByTimestampBetween(Long startTime, Long endTime);

    @Query("SELECT DISTINCT l.service FROM AppLogEntity l WHERE l.service IS NOT NULL ORDER BY l.service")
    List<String> findDistinctServices();

    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
