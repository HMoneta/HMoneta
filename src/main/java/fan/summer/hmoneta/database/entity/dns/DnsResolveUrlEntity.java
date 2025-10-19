package fan.summer.hmoneta.database.entity.dns;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 存储每一个DNS解析组对应的需解析的URL及解析情况
 */
@Entity
@Getter
@Setter
public class DnsResolveUrlEntity {
    @Id
    private String id;

    private String groupId;

    private String url;

    private Integer resolveStatus;

    private String ipAddress;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
