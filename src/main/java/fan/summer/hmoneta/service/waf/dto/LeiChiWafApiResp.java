package fan.summer.hmoneta.service.waf.dto;

import lombok.Data;

/**
 * 雷池 API 通用响应结构
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/2/7
 */
@Data
public class LeiChiWafApiResp<T> {
    private String err;
    private String msg;
    private T data;
}
