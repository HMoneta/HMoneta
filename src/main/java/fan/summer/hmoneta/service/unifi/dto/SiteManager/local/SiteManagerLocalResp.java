package fan.summer.hmoneta.service.unifi.dto.SiteManager.local;

import lombok.Data;

import java.util.List;

/**
 * UniFi Site Manager 本地响应基类
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/18
 */
@Data
public class SiteManagerLocalResp<T> {
    private Integer offset;
    private Integer limit;
    private Integer count;
    private Integer totalCount;
    private List<T> data;
}
