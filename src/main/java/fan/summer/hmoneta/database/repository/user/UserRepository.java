package fan.summer.hmoneta.database.repository.user;

import fan.summer.hmoneta.database.entity.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户仓库接口，继承自JpaRepository，提供用户实体的基本操作以及额外的查询方法。
 * 该接口主要用于处理与用户相关的数据库操作。
 *
 * @see JpaRepository
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    /**
     * 根据用户名查找用户实体。
     *
     * @param username 用户名
     * @return 返回与给定用户名匹配的用户实体，如果没有找到则返回null
     */
    UserEntity findByUsername(String username);
}
