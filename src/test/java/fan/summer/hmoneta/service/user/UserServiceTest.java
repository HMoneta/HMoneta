package fan.summer.hmoneta.service.user;

import fan.summer.hmoneta.common.enums.exception.user.UserExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.entity.user.req.UserReq;
import fan.summer.hmoneta.database.entity.user.UserEntity;
import fan.summer.hmoneta.database.repository.user.UserRepository;
import fan.summer.hmoneta.util.JwtUtil;
import fan.summer.hmoneta.util.Md5Util;
import fan.summer.hmoneta.util.ObjectUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Spy // 使用@Spy来验证内部方法调用
    @InjectMocks  // 创建真实的UserService实例，并注入Mock的依赖
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserRepository userRepository;

    @Test
    void should_throw_exception_when_username_exists () {
        // 测试数据
        String username = "admin";
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        UserEntity existEntity = new UserEntity();
        existEntity.setUsername(username);

        // 定义userRepository中findByUsername在测试时返回的值
        when(userRepository.findByUsername(username)).thenReturn(existEntity);

        //捕获被测试方法中抛出的异常
        HMException ex = assertThrows(HMException.class,
                () -> userService.register(entity));
        // 判断异常是否符合预期
        assertEquals(UserExceptionEnum.USER_NAME_EXIT_ERROR.getMessage(),
                ex.getMessage());
        // 验证数据库逻辑
        verify(userRepository, never()).save(any());

    }

    @Test
    void should_save_user_success(){
        try(MockedStatic<Md5Util> mocked = mockStatic(Md5Util.class)){
            // 定义Md5Util中md5Digest在测试时返回的值
            mocked.when(()-> Md5Util.md5Digest("123456",10)).thenReturn("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");
            // 测试数据
            UserEntity entity = new UserEntity();
            entity.setUsername("admin");
            entity.setPassword("123456");
            entity.setSalt(10);

            when(userRepository.findByUsername("admin")).thenReturn(null);

            userService.register(entity);

            assertEquals("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9", entity.getPassword());

            verify(userRepository).save(entity);

        }
    }

    @Test
    void should_not_init_sys_admin(){
        UserEntity entity = new UserEntity();
        entity.setUsername("admin");
        entity.setPassword("123456");
        entity.setSalt(10);

        List<UserEntity> users = new ArrayList<UserEntity>();
        users.add(entity);

        when(userRepository.findAll()).thenReturn(users);

        userService.initHMUser();

        verify(userService, never()).register(entity);
    }

    @Test
    void should_init_sys_admin(){
        try(MockedStatic<Md5Util> mocked = mockStatic(Md5Util.class)){
            mocked.when(Md5Util::saltGenerator).thenReturn(10);
            mocked.when(()->Md5Util.md5Digest(anyString(),eq(10))).thenReturn("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");

            UserEntity entity = new UserEntity();
            entity.setUsername("admin");
            entity.setPassword("123456");
            entity.setSalt(10);

            when(userRepository.findAll()).thenReturn(List.of());
            when(userRepository.findByUsername("admin")).thenReturn(null);

            userService.initHMUser();

            // 使用ArgumentCaptor捕获传递给register的用户对象
            ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
            verify(userService).register(userCaptor.capture());

            UserEntity capturedUser = userCaptor.getValue();
            assertEquals("admin", capturedUser.getUsername());
            assertNotNull(capturedUser.getPassword());
            assertEquals(10, capturedUser.getSalt());
            assertEquals("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9", capturedUser.getPassword());// 密码长度为10

        }
    }

    @Test
    void should_success_login(){
        try(MockedStatic<Md5Util> mocked = mockStatic(Md5Util.class)){
            mocked.when(() -> Md5Util.md5Digest(anyString(),eq(10))).thenReturn("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");

            UserReq req = new UserReq();
            req.setUsername("admin");
            req.setPassword("123456");

            UserEntity entity = new UserEntity();
            entity.setId(1L);
            entity.setUsername("admin");
            entity.setPassword("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");
            entity.setSalt(10);

            when(userRepository.findByUsername("admin")).thenReturn(entity);
            when(jwtUtil.createTokenForObject(entity)).thenReturn("uerTestToken");

            String token = userService.login(req);

            assertEquals("uerTestToken", token);
        }
    }
    @Test
    void should_throw_user_info_misses_when_login() {
        try (MockedStatic<Md5Util> mocked = mockStatic(Md5Util.class); MockedStatic<ObjectUtil> mockedObj = mockStatic(ObjectUtil.class)) {
            mocked.when(() -> Md5Util.md5Digest(anyString(), eq(10))).thenReturn("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");

            UserReq req = new UserReq();
            req.setPassword("123456");

            mockedObj.when(() -> ObjectUtil.isEmpty(req.getUsername())).thenReturn(true);
            mockedObj.when(() -> ObjectUtil.isEmpty(req.getPassword())).thenReturn(false);

            UserEntity entity = new UserEntity();
            entity.setId(1L);
            entity.setUsername("admin");
            entity.setPassword("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");
            entity.setSalt(10);

            //捕获被测试方法中抛出的异常
            HMException ex = assertThrows(HMException.class,
                    () -> userService.login(req));

            assertEquals(UserExceptionEnum.USER_INFO_MISSED_ERROR.getMessage(), ex.getMessage());
        }
    }

    @Test
    void should_throw_user_not_exist_when_login() {
        try (MockedStatic<ObjectUtil> mockedObj = mockStatic(ObjectUtil.class)) {

            UserReq req = new UserReq();
            req.setUsername("admin");
            req.setPassword("123456");

            mockedObj.when(() -> ObjectUtil.isEmpty(req.getUsername())).thenReturn(false);
            mockedObj.when(() -> ObjectUtil.isEmpty(req.getPassword())).thenReturn(false);

            when(userRepository.findByUsername(anyString())).thenReturn(null);

            //捕获被测试方法中抛出的异常
            HMException ex = assertThrows(HMException.class,
                    () -> userService.login(req));

            assertEquals(UserExceptionEnum.USER_NAME_ERROR.getMessage(), ex.getMessage());
        }
    }

    @Test
    void should_throw_password_error_when_login(){
        try (MockedStatic<Md5Util> mocked = mockStatic(Md5Util.class); MockedStatic<ObjectUtil> mockedObj = mockStatic(ObjectUtil.class)) {
            mocked.when(() -> Md5Util.md5Digest(anyString(), eq(10))).thenReturn("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b9");

            UserReq req = new UserReq();
            req.setUsername("admin");
            req.setPassword("1234567");

            mockedObj.when(() -> ObjectUtil.isEmpty(req.getUsername())).thenReturn(false);
            mockedObj.when(() -> ObjectUtil.isEmpty(req.getPassword())).thenReturn(false);

            UserEntity entity = new UserEntity();
            entity.setId(1L);
            entity.setUsername("admin");
            entity.setPassword("b3d4f8e8c7a7e5f6a1b3c4d5e6f7a8b91");
            entity.setSalt(10);

            when(userRepository.findByUsername("admin")).thenReturn(entity);

            //捕获被测试方法中抛出的异常
            HMException ex = assertThrows(HMException.class,
                    () -> userService.login(req));

            assertEquals(UserExceptionEnum.USER_PASSWORD_ERROR.getMessage(), ex.getMessage());
        }
    }
}