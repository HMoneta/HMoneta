package fan.summer.hmoneta.database.entity.dns;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/**
 * 代表DNS提供商的实体类。
 * 此类用于存储和管理DNS提供商的相关信息，包括但不限于提供商名称、代码、描述等。
 * 数据库表名为`dns_providers`，包含索引`idx_provider_code`和`idx_status`以优化查询性能。
 * 该实体通过JPA注解映射到数据库，并支持Lombok插件提供的Getter, Setter, ToString等功能。
 * 实现了equals和hashCode方法，确保在处理Hibernate代理对象时也能正确比较对象。
 */
@Entity
@Table(name = "dns_providers",indexes = {
        @Index(name = "idx_provider_name", columnList = "provider_name")
})
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class DnsProviderEntity {

    @Id
    private String id;
    private String providerName;
    private String providerCode;
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    // 所需验证的键值
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Set<String> authenticateWay;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        DnsProviderEntity that = (DnsProviderEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
