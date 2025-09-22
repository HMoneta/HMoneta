package fan.summer.hmoneta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan("fan.summer.hmoneta.database.entity")
public class HMonetaApplication {

    void main(String[] args) {
        SpringApplication.run(HMonetaApplication.class, args);
    }

}
