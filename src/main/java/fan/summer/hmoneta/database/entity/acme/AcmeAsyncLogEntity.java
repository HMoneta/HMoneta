package fan.summer.hmoneta.database.entity.acme;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/29
 */
@Entity
@Data
public class AcmeAsyncLogEntity {
    @Id
    private String taskId;
    private String domain;
    private String logInfo;
    private boolean success;
}
