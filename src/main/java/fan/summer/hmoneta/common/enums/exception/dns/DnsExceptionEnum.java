package fan.summer.hmoneta.common.enums.exception.dns;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

public enum DnsExceptionEnum implements HMExceptionEnum {
    DNS_GROUP_EMPTY_ERROR("001", "DNS解析组信息为空"),
    DNS_GROUP_AUTH_EMPTY_ERROR("001", "DNS服务商授权信息为空");
    private final String code;
    private final String message;

    DnsExceptionEnum(String code, String message) {
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
