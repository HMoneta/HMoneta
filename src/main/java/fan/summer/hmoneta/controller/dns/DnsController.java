package fan.summer.hmoneta.controller.dns;

import fan.summer.hmoneta.controller.dns.entity.req.DnsResolveReq;
import fan.summer.hmoneta.controller.dns.entity.resp.DnsResolveResp;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.service.dns.DnsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/resolve_info")
    public ResponseEntity<List<DnsResolveResp>> queryAllDnsResolve() {
        List<DnsResolveResp> resolveResps = dnsService.queryAllDnsResolve();
        return ResponseEntity.ok(resolveResps);
    }

    @PostMapping("/insert_group")
    public ResponseEntity<List<DnsProviderEntity>> insertDnsResolveGroup(@RequestBody DnsResolveReq req) {
        dnsService.insertDnsResolveGroup(req);
        return ResponseEntity.ok().build();
    }
}
