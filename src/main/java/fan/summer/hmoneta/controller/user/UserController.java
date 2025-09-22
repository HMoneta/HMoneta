package fan.summer.hmoneta.controller.user;

import fan.summer.hmoneta.common.enums.exception.user.UserExceptionEnum;
import fan.summer.hmoneta.controller.entity.user.req.UserReq;
import fan.summer.hmoneta.service.user.UserService;
import fan.summer.hmoneta.util.ObjectUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

/**
 * 类的详细说明
 *
 * @author summer
 * @version 1.00
 * @Date 2025/9/22
 */
@RestController
@RequestMapping("/hm/user")
public class UserController {
    private UserService userService;
    @Autowired
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody UserReq req) {
        if(ObjectUtil.isEmpty(req) || ObjectUtil.isEmpty(req.getUsername()) || ObjectUtil.isEmpty(req.getPassword())){
            return ResponseEntity.status(500).body(UserExceptionEnum.USER_INFO_MISSED_ERROR.getMessage());
        }
        String login = userService.login(req);
        return ResponseEntity.ok(login);
    }
}
