package fan.summer.hmoneta.database.entity.waf;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * 雷池WAF令牌实体类
 * <p>
 * 该实体类用于存储雷池WAF系统的访问令牌信息。
 * 主要用于WAF（Web应用防火墙）认证和访问控制场景。
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/25
 */
@Data
@Entity
public class LeiChiTokenEntity {

    /**
     * 主键ID
     * 自增生成策略
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 雷池WAF访问令牌
     * 存储认证所需的token字符串
     */
    private String token;
}
