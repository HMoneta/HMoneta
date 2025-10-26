package fan.summer.hmoneta.database.entity.dns;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

/**
 * 记录每个解析组的信息
 */
@Entity
@Getter
@Setter
public class DnsResolveGroupEntity {
    @Id
    private String id;

    private String groupName;

    private String providerId;

    private String authId;

    private String authKey;

}
