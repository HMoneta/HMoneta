package fan.summer.hmoneta.service.unifi;

import fan.summer.hmoneta.database.entity.unifi.UnifiSettingEntity;
import fan.summer.hmoneta.database.repository.unifi.UnifiSettingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2026/1/16
 */
@Service
@AllArgsConstructor
public class UnifiApiService {
    private final UnifiSettingRepository unifiSettingRepository;

    // Unifi设置信息
    public UnifiSettingEntity queryUnifiSettingInfo() {
        return unifiSettingRepository.findAll().stream().findFirst().orElse(null);
    }

    @Transactional
    public void settingUnifiSettingInfo(UnifiSettingEntity unifiSettingEntity) {
        if (unifiSettingRepository.count() > 0) {
            unifiSettingRepository.deleteAll();
        }
        unifiSettingRepository.save(unifiSettingEntity);
    }


    // Site Manager Api

    public void siteListHost() {

    }
}
