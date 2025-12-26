package fan.summer.hmoneta.common.enums.exception.plugin;

import fan.summer.hmoneta.common.enums.exception.HMExceptionEnum;

public enum PluginExceptionEnum implements HMExceptionEnum {
    PLUGIN_FILE_EMPTY_ERROR("001", "上传插件的文件为空"),
    PLUGIN_FILE_TYPE_ERROR("002", "上传插件非ZIP格式"),
    PLUGIN_FILE_EXIST_ERROR("003","插件文件已存在");
    private final String code;
    private final String message;

    PluginExceptionEnum(String code, String message) {
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
