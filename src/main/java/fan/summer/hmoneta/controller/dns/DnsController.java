package fan.summer.hmoneta.controller.dns;

import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.service.dns.DnsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;


@RestController
@RequestMapping("/hm/dns")
public class DnsController {
    private final DnsService dnsService;
    @Autowired
    public DnsController(DnsService dnsService) {
        this.dnsService = dnsService;
    }

    @GetMapping("/query_all")
    public ResponseEntity<List<DnsProviderEntity>> getDnsService() {
        List<DnsProviderEntity> dnsProviderEntities = dnsService.queryAllDnsProvider();
        return ResponseEntity.ok(dnsProviderEntities);
    }

}
