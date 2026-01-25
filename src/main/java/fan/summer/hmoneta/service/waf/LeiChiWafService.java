package fan.summer.hmoneta.service.waf;

import fan.summer.hmoneta.database.entity.waf.LeiChiTokenEntity;
import fan.summer.hmoneta.database.repository.waf.LeiChiRepository;
import fan.summer.hmoneta.util.WebApiUtil;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/25
 */
@Service
public class LeiChiWafService {
    private WebApiUtil webApiUtil;
    private final LeiChiRepository leiChiRepository;

    public LeiChiWafService(LeiChiRepository leiChiRepository) {
        this.leiChiRepository = leiChiRepository;
    }

    public void insertLeiChiApiToken(LeiChiTokenEntity leiChiTokenEntity) {
        List<LeiChiTokenEntity> all = leiChiRepository.findAll();
        if (!all.isEmpty()) {
            leiChiRepository.deleteAll();
        } else {
            leiChiRepository.save(leiChiTokenEntity);
        }
    }

//    private void initWebApiUtil(boolean isLocal) {
//
//        this.webApiUtil = WebApiUtil.build(unifiSettingEntity.getBaseUri(), Map.of("Accept", "application/json", "X-SLCE-API-TOKEN", unifiSettingEntity.getApiKey()));
//
//    }
}

