package fan.summer.hmoneta.common.handle;

import fan.summer.hmoneta.common.exception.HMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 处理Controller抛出的异常
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/23
 */
@ControllerAdvice
public class ControllerExceptionHandler {
    public static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ApiException> handleException(Exception e) {
        return ResponseEntity.status(500).body(new ApiException("500", e.getMessage()));
    }

    @ExceptionHandler(HMException.class)
    @ResponseBody
    public ResponseEntity<ApiException> handleHMException(HMException e) {
        return ResponseEntity.status(500).body(new ApiException(e.getE().getCode(), e.getE().getMessage()));
    }

    record ApiException(String exceptionCode, String message) {
    }
}
