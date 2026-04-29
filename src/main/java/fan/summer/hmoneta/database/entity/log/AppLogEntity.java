package fan.summer.hmoneta.database.entity.log;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "hm_app_log", indexes = {
    @Index(name = "idx_log_timestamp", columnList = "timestamp"),
    @Index(name = "idx_log_level", columnList = "level"),
    @Index(name = "idx_log_service", columnList = "service")
})
@Data
public class AppLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long timestamp;

    private String level;

    private String service;

    private String thread;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String exception;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
