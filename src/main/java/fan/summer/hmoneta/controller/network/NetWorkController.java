package fan.summer.hmoneta.controller.network;

import fan.summer.hmoneta.service.network.NetworkService;
import fan.summer.hmoneta.service.network.dto.DevicesInfoDto;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 网络设备 Controller
 * <p>
 * 提供网络设备与雷池 WAF 站点关联的 API 接口。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/4/5
 */
@RestController
@RequestMapping("/hm/network")
@AllArgsConstructor
public class NetWorkController {

    private NetworkService networkService;

    /**
     * 获取家庭网络设备列表
     * <p>
     * 查询所有 UniFi 连接的设备，并与雷池 WAF 站点进行匹配。
     * 如果设备 IP 在站点的上游服务器中，则认为该设备受到 WAF 保护。
     * </p>
     *
     * @return 设备信息列表，包含 UniFi 设备和雷池站点信息（如果匹配）
     */
    @GetMapping("/devices")
    public List<DevicesInfoDto> listHomeNetDevices() {
        System.out.printf("1");
        return networkService.listHomeNetDevices();
    }

}
