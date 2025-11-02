//package fan.summer.hmoneta.service.plugin;
//
//
//import org.apache.logging.log4j.LogManager;
//import org.apache.logging.log4j.Logger;
//import org.springframework.stereotype.Component;
//
//@Component
//public class Log4jPluginLogger implements PluginLogger {
//
//    private static final Logger log4j = LogManager.getLogger(Log4jPluginLogger.class);
//
//    @Override
//    public void logger(String stepName, String detail, LogLeveEnum logLeveEnum) {
//        PluginLogger.super.logger(stepName, detail, logLeveEnum);
//    }
//
//    @Override
//    public void error(String message) {
//        log4j.error(message);
//
//    }
//
//    @Override
//    public void warn(String message) {
//        log4j.warn(message);
//
//    }
//
//    @Override
//    public void info(String message) {
//        log4j.info(message);
//    }
//
//    @Override
//    public void debug(String message) {
//        log4j.debug(message);
//    }
//}