package fan.summer.hmoneta.common.enums.exception.user;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

/**
 * 枚举类 UserExceptionEnum 实现了 HMExceptionEnum 接口，用于定义用户相关的异常情况。
 * 通过提供特定的错误代码和错误信息，该枚举帮助开发者在处理用户相关操作时能够更清晰地识别并响应不同的错误状况。
 * 每个枚举实例代表一种具体的用户异常类型，并且每个实例都关联有一个唯一的错误代码以及对应的错误描述。
 *
 * @see HMExceptionEnum
 */
public enum UserExceptionEnum implements HMExceptionEnum {
    USER_NAME_ERROR("USER_001", "用户名错误"),
    USER_PASSWORD_ERROR("USER_002", "用户密码错误"),
    USER_NAME_EXIT_ERROR("USER_002", "用户已存在")
    ;
    private final String code;
    private final String message;

    UserExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getMessage() {
        return this.message;
    }
}
