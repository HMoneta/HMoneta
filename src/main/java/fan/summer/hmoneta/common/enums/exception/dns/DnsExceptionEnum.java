package fan.summer.hmoneta.common.enums.exception.dns;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

public enum DnsExceptionEnum implements HMExceptionEnum {
    DNS_GROUP_EMPTY_ERROR("001", "DNS解析组信息为空"),
    DNS_GROUP_AUTH_EMPTY_ERROR("002", "DNS服务商授权信息为空"),
    DNS_GROUP_URL_WITH_PROTOCOL_ERROR("003", "待解析网址不可携带协议类型"),
    DNS_GROUP_URL_FORMAT_ERROR("004", "待解析网址格式错误"),
    DNS_GROUP_MODIFY_INFO_EMPTY("005", "所修改的分组缺少关键信息");
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
