package fan.summer.hmoneta.service.user;

import fan.summer.hmoneta.common.enums.exception.user.UserExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.entity.user.req.UserReq;
import fan.summer.hmoneta.database.entity.user.UserEntity;
import fan.summer.hmoneta.database.repository.user.UserRepository;
import fan.summer.hmoneta.util.Md5Util;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.StringJoiner;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/9/19
 */
@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * 初始化系统用户的方法。此方法在Spring容器初始化完成后自动调用。
     * 方法首先检查数据库中是否存在任何用户，如果不存在，则创建一个默认的管理员用户（用户名为"admin"），
     * 并为其生成一个随机密码和盐值后保存到数据库中。若已存在用户，则直接跳过创建步骤。
     * 整个过程中会记录相应的日志信息以便于跟踪初始化状态。
     */
    @PostConstruct
    public void initHMUser(){
        LOG.info("===============初始化系统用户===============");
        List<UserEntity> all = userRepository.findAll();
        LOG.info(">>>>>>>>>>检查是否存在系统用户>>>>>>>>>>");
        if(all.isEmpty()){
            LOG.warn("<<<<<<<<<<<系统用户不存在<<<<<<<<<<<");
            LOG.info(">>>>>>>>>>开始创建系统用户>>>>>>>>>>");
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+~`|}{[]:;?><,./-=";
            Random random = new SecureRandom();
            StringJoiner password = new StringJoiner("");
            for (int i = 0; i < 10; i++) {
                int index = random.nextInt(characters.length());
                password.add(characters.charAt(index) + "");
            }
            UserEntity entity = new UserEntity();
            entity.setUsername("admin");
            entity.setPassword(password.toString());
            entity.setSalt(Md5Util.saltGenerator());
            this.register(entity);
            LOG.info("<<<<<<<<<<<完成系统用户创建<<<<<<<<<<<");
            LOG.info("系统用户信息->Username:admin;Password:{}",password);
        }else {
            LOG.info("<<<<<<<<<<<已存系统用户系统用户<<<<<<<<<<<");
        }
        LOG.info("===============完成系统用户初始化===============");
    }
    
    /**
     * 用户注册方法。
     *
     * 该方法首先通过用户名查找数据库中是否存在相同的用户。如果存在，则抛出一个表示用户名已存在的异常；如果不存在，
     * 则对用户的密码进行MD5加密（使用盐值），然后保存用户实体到数据库中。
     *
     * @param entity 待注册的用户实体
     * @throws HMException 当用户名已存在时抛出此异常
     */
    public void register(UserEntity entity){
        UserEntity byUsername = userRepository.findByUsername(entity.getUsername());
        if(byUsername != null){
            throw new HMException(UserExceptionEnum.USER_NAME_EXIT_ERROR);
        }else{
            entity.setPassword(Md5Util.md5Digest(entity.getPassword(), entity.getSalt()));
            userRepository.save(entity);
        }
    }

    public void login(){

    }

}
