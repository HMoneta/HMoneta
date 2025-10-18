package fan.summer.hmoneta.controller.user.entity.req;

import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/9/19
 */
@Data
public class UserReq {
    private String username;
    private String password;
    private String oldPassword;
}
