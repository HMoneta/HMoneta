package fan.summer.hmoneta.service.unifi.dto.SiteManager.local.client;

import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/19
 */
@Data
public class ClientsLocalInfo {
    private String type;
    private String id;
    private String name;
    private String connectedAt;
    private String ipAddress;
    private Object access;
}
