package fan.summer.hmoneta.common.enums.exception.waf;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

/**
 * 雷池WAF相关异常枚举
 */
public enum LeiChiExceptionEnum implements HMExceptionEnum {
    LEICHI_API_NOT_INIT("WAF_001", "雷池API未初始化，请先配置API令牌"),
    ;

    private final String code;
    private final String message;

    LeiChiExceptionEnum(String code, String message) {
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
