package fan.summer.hmoneta.controller.dns.entity.req;

import lombok.Data;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/10/27
 */
@Data
public class GroupModifyReq {
    private String id;

    private String groupName;

    private String authId;

    private String authKey;

    private Boolean isDelete;

}
