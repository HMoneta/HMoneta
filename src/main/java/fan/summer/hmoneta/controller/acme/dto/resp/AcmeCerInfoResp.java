package fan.summer.hmoneta.controller.acme.dto.resp;

import java.time.LocalDateTime;
import java.util.Date;

public record AcmeCerInfoResp(boolean haveCer, LocalDateTime certApplyTime, Date notBefore, Date notAfter) {
}
