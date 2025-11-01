package fan.summer.hmoneta.database.entity.dns;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.Map;

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

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> credentials;

}
