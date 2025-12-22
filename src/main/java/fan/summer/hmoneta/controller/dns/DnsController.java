package fan.summer.hmoneta.controller.dns;

import fan.summer.hmoneta.common.enums.exception.dns.DnsExceptionEnum;
import fan.summer.hmoneta.common.exception.HMException;
import fan.summer.hmoneta.controller.dns.entity.req.DnsResolveReq;
import fan.summer.hmoneta.controller.dns.entity.req.DnsResolveUrlReq;
import fan.summer.hmoneta.controller.dns.entity.req.GroupModifyReq;
import fan.summer.hmoneta.controller.dns.entity.resp.DnsResolveResp;
import fan.summer.hmoneta.database.entity.dns.DnsProviderEntity;
import fan.summer.hmoneta.database.entity.dns.DnsResolveUrlEntity;
import fan.summer.hmoneta.service.dns.DnsService;
import fan.summer.hmoneta.util.ObjectUtil;
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

    @PostMapping("/modify_group")
    public ResponseEntity<String> modifyDnsResolveGroup(@RequestBody GroupModifyReq req) {
        dnsService.modifyDnsResolveGroup(req);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/delete_group")
    public ResponseEntity<String> deleteDnsResolveGroup(@RequestBody GroupModifyReq req) {
        dnsService.deleteDnsResolveGroup(req);
        return ResponseEntity.ok("success");
    }

    @PostMapping("/url/modify")
    public ResponseEntity<String> deleteDnsResolveUrl(@RequestBody DnsResolveUrlReq req) {
        if (ObjectUtil.isEmpty(req)) {
            throw new HMException(DnsExceptionEnum.DNS_URL_MODIFY_INFO_EMPTY);
        }
        DnsResolveUrlEntity dnsResolveUrlEntity = new DnsResolveUrlEntity();
        if (ObjectUtil.isNotEmpty(req.getUrl())) {
            dnsResolveUrlEntity.setId(req.getId());
        }
        dnsResolveUrlEntity.setUrl(req.getUrl());
        dnsResolveUrlEntity.setGroupId(req.getGroupId());
        dnsService.modifyDnsResolveUrl(dnsResolveUrlEntity);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/url/delete")
    public ResponseEntity<String> deleteDnsResolveUrl(@RequestBody String urlId) {
        dnsService.deleteDnsResolveUrl(urlId);
        return ResponseEntity.ok().build();
    }
}
