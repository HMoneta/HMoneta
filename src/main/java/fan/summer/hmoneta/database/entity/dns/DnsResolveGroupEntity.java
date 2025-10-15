package fan.summer.hmoneta.database.entity.dns;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/10/15
 */
@Entity
@Getter
@Setter

public class DnsResolveGroupEntity {
    @Id
    private String id;

    private String providerId;

    private String authId;

    private String authKey;

    private List<String> urls;

}
