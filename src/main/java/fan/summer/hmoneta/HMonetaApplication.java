package fan.summer.hmoneta;


import fan.summer.hmoneta.common.HMBanner;
import org.apache.commons.text.RandomStringGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.core.env.Environment;

import java.util.Random;

@SpringBootApplication
@EntityScan("fan.summer.hmoneta.database.entity")
public class HMonetaApplication {

    private static final Logger LOG = LoggerFactory.getLogger(HMonetaApplication.class);

    static void main(String[] args) {
        RandomStringGenerator randomStringGenerator = RandomStringGenerator.builder().withinRange('a', 'z')
                .withinRange('A', 'Z').get();
        MDC.put("LOG_ID", System.currentTimeMillis() + randomStringGenerator.generate(3));
        SpringApplication application = new SpringApplication(HMonetaApplication.class);
        application.setBanner(new HMBanner());
        Environment env = application.run(args).getEnvironment();
        LOG.info("服务启动成功!");
        LOG.info("启动成功，项目地址：http://localhost:{}", env.getProperty("server.port"));
    }


}
