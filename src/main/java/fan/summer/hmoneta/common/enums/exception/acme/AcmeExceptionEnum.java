package fan.summer.hmoneta.common.enums.exception.acme;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

public enum AcmeExceptionEnum implements HMExceptionEnum {
    CER_ERROR_LOG_NOT_FIND("001", "未找到指定日志"),
    CER_ERROR_FOLDER_NOT_EXIST("002", "证书文件夹不存在"),
    CER_CREATE_FOLDER_ERROR("003", "创建证书文件夹失败"),
    DNS_SERVICE_NOT_FOUND_PROVIDER_ERROR("004", "未找到DNS供应商");

    private String code;
    private String message;

    AcmeExceptionEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }
}
