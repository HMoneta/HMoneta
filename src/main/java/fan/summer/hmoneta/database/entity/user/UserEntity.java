package fan.summer.hmoneta.database.entity.user;

import fan.summer.hmoneta.database.repository.user.UserRepository;
import fan.summer.hmoneta.util.JwtUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 用户实体类，用于映射数据库中的用户表。该类通过JPA注解定义了与数据库表"hm_user"的关联关系。
 * 本类包含了用户的主键ID、用户名、密码以及盐值等属性。
 *
 * @see UserRepository
 */
@Entity
@Table(name = "hm_user")
@Data
public class UserEntity {
    @Id
    private Long id;

    private String username;

    @JwtUtil.JwtExclude
    private String password;
    @JwtUtil.JwtExclude
    private int salt;
}
