package fan.summer.hmoneta.service.acme;

import fan.summer.hmoneta.database.entity.acme.AcmeChallengeInfoEntity;
import fan.summer.hmoneta.database.entity.acme.AcmeUserInfoEntity;
import fan.summer.hmoneta.plugin.api.dns.HmDnsProviderPlugin;
import lombok.Data;

/**
 * 证书申请任务上下文
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/27
 */
@Data
public class AcmeTaskContext {
    private AcmeUserInfoEntity acmeUserInfoEntity;
    private HmDnsProviderPlugin hmDnsProviderPlugin;
    private AcmeChallengeInfoEntity acmeChallengeInfoEntity;
}
