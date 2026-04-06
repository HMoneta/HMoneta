package fan.summer.hmoneta.service.network;

import fan.summer.hmoneta.service.network.dto.DevicesInfoDto;
import fan.summer.hmoneta.service.unifi.UnifiApiService;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteInfoLocal;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteManagerLocalResp;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.client.ClientsLocalInfo;
import fan.summer.hmoneta.service.waf.LeiChiWafService;
import fan.summer.hmoneta.service.waf.dto.site.LeiChiSiteListResp;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 网络设备服务
 * <p>
 * 提供网络设备与雷池 WAF 站点关联分析功能，
 * 检查所有连接设备是否都受到 WAF 保护。
 * </p>
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/22
 */
@Service
@AllArgsConstructor
@Log4j2
public class NetworkService {

    private LeiChiWafService leiChiWafService;
    private UnifiApiService unifiApiService;

    /**
     * 查询家庭网络设备并检查 WAF 站点保护情况
     * <p>
     * 执行以下操作：
     * <ul>
     *   <li>查询所有 UniFi 连接的设备</li>
     *   <li>查询所有雷池 WAF 站点</li>
     *   <li>匹配设备 IP 与站点上游服务器</li>
     * </ul>
     * </p>
     *
     * @return 设备信息列表，包含 UniFi 设备和匹配的雷池站点信息
     */
    public List<DevicesInfoDto> listHomeNetDevices() {
        List<ClientsLocalInfo> allDevices = new ArrayList<>();
        // 查询所有连接设备
        SiteManagerLocalResp<SiteInfoLocal> siteInfoLocalSiteManagerLocalResp = unifiApiService.listLocalSites();
        if (siteInfoLocalSiteManagerLocalResp != null && siteInfoLocalSiteManagerLocalResp.getData() != null) {
            siteInfoLocalSiteManagerLocalResp.getData().forEach(item -> {
                String id = item.getId();
                SiteManagerLocalResp<ClientsLocalInfo> clientsLocalInfoSiteManagerLocalResp = unifiApiService.listConnectedClients(id);
                if (clientsLocalInfoSiteManagerLocalResp != null && clientsLocalInfoSiteManagerLocalResp.getData() != null) {
                    allDevices.addAll(clientsLocalInfoSiteManagerLocalResp.getData());
                }
            });
        }
        log.info("共查询到 {} 个连接设备", allDevices.size());

        // 查询所有 LeiChi 站点信息
        List<LeiChiSiteListResp.SiteInfo> allSites = leiChiWafService.listSite().getData();
        if (allSites == null) {
            allSites = new ArrayList<>();
        }
        log.info("共查询到 {} 个雷池站点", allSites.size());

        // 为每个设备查找对应的 WAF 站点并合并信息
        List<DevicesInfoDto> results = new ArrayList<>();
        for (ClientsLocalInfo device : allDevices) {
            DevicesInfoDto dto = new DevicesInfoDto();
            dto.setUnifiInfo(device);

            String deviceIp = device.getIpAddress();
            LeiChiSiteListResp.SiteInfo matchedSite = findSiteByUpstreamIp(allSites, deviceIp);

            if (matchedSite != null) {
                dto.setLeiChiSiteInfo(matchedSite);
            } else {
                log.debug("设备 {} (IP: {}) 未匹配到 WAF 站点", device.getName(), deviceIp);
            }

            results.add(dto);
        }

        // 统计匹配结果
        long matchedCount = results.stream().filter(dto -> dto.getLeiChiId() != null).count();
        long unmatchedCount = results.size() - matchedCount;
        log.info("WAF 站点匹配结果: 已匹配={}, 未匹配={}", matchedCount, unmatchedCount);

        return results;
    }

    /**
     * 根据设备 IP 查找对应的 WAF 站点
     * <p>
     * 遍历所有站点，检查设备的 IP 是否在站点的上游服务器列表中。
     * </p>
     *
     * @param sites 所有 WAF 站点列表
     * @param deviceIp 设备 IP 地址
     * @return 匹配的站点，未找到返回 null
     */
    private LeiChiSiteListResp.SiteInfo findSiteByUpstreamIp(List<LeiChiSiteListResp.SiteInfo> sites, String deviceIp) {
        if (deviceIp == null || deviceIp.isEmpty()) {
            return null;
        }

        for (LeiChiSiteListResp.SiteInfo site : sites) {
            List<String> upstreams = site.getUpstreams();
            if (upstreams == null || upstreams.isEmpty()) {
                continue;
            }

            for (String upstream : upstreams) {
                // 解析上游地址，提取 IP 和端口
                String upstreamIp = extractIpFromUpstream(upstream);
                if (deviceIp.equals(upstreamIp)) {
                    return site;
                }
            }
        }
        return null;
    }

    /**
     * 从上游地址中提取 IP
     * <p>
     * 支持格式：
     * <ul>
     *   <li>10.0.0.1:8080 → 10.0.0.1</li>
     *   <li>10.0.0.1 → 10.0.0.1</li>
     * </ul>
     * </p>
     *
     * @param upstream 上游服务器地址
     * @return 提取的 IP 地址
     */
    private String extractIpFromUpstream(String upstream) {
        if (upstream == null || upstream.isEmpty()) {
            return null;
        }
        // 如果包含端口号，截取 IP 部分
        int colonIndex = upstream.indexOf(':');
        if (colonIndex > 0) {
            return upstream.substring(0, colonIndex);
        }
        return upstream;
    }

    /**
     * 获取未受 WAF 保护的设备列表
     *
     * @return 未匹配到站点的设备信息列表
     */
    public List<DevicesInfoDto> getUnprotectedDevices() {
        return listHomeNetDevices().stream()
                .filter(dto -> dto.getLeiChiId() == null)
                .toList();
    }

    /**
     * 获取已受 WAF 保护的设备列表
     *
     * @return 已匹配到站点的设备信息列表
     */
    public List<DevicesInfoDto> getProtectedDevices() {
        return listHomeNetDevices().stream()
                .filter(dto -> dto.getLeiChiId() != null)
                .toList();
    }

}
