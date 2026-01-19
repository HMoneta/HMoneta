package fan.summer.hmoneta.service.unifi;


import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteInfoLocal;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.SiteManagerLocalResp;
import fan.summer.hmoneta.service.unifi.dto.SiteManager.local.client.ClientsLocalInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class UnifiApiServiceTest {
    @Autowired
    private UnifiApiService unifiApiService;

    @Test
    public void testSiteListHost() {
        // 调用服务方法并验证结果
        unifiApiService.siteListHost();
    }

    @Test
    public void testLocalSiteList() {
        SiteManagerLocalResp<SiteInfoLocal> siteInfoLocalSiteManagerLocalResp = unifiApiService.listLocalSites();
        System.out.println(1);
    }

    @Test
    public void testListConnectedClients() {
        SiteManagerLocalResp<SiteInfoLocal> siteInfoLocalSiteManagerLocalResp = unifiApiService.listLocalSites();
        List<SiteInfoLocal> data = siteInfoLocalSiteManagerLocalResp.getData();
        SiteInfoLocal first = data.getFirst();
        String id = first.getId();
        SiteManagerLocalResp<ClientsLocalInfo> listSiteManagerLocalResp = unifiApiService.listConnectedClients(id);
        System.out.println(1);
    }


}