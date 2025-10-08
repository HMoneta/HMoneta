package fan.summer.hmoneta.common;

import org.springframework.boot.Banner;
import org.springframework.core.env.Environment;

import java.io.PrintStream;

/**
 * HMBanner 类实现了 Banner 接口，用于在应用启动时打印自定义的横幅。
 * 该横幅包含了应用的基本信息，如名称和版本号。版本号是从环境属性中获取的，
 * 如果未能成功获取到版本号，则会显示默认消息“config file can not be loaded”来代替实际版本号。
 * 横幅的具体内容通过静态字符串 BANNER 定义，并且在调用 printBanner 方法时将其中的占位符 "ver" 替换为具体的版本信息后输出。
 */
public class HMBanner implements Banner {
    private static final String BANNER =
            """
                            _    _  __  __                           _       \s
                            | |  | ||  \\/  |                         | |      \s
                            | |__| || \\  / | ___  _ __ ___   ___  ___| |_ __ _\s
                            |  __  || |\\/| |/ _ \\| '_ ` _ \\ / _ \\/ __| __/ _` |
                            | |  | || |  | | (_) | | | | | |  __/ (__| || (_| |
                            |_|  |_||_|  |_|\\___/|_| |_| |_|\\___|\\___|\\__\\__,_|
                                                                             \s
                                                HMoneta (ver)
                    """;

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        String version = environment.getProperty("hmoneta.version");
        if (version == null) {
            version = "config file can not be loaded";
        }
        String banner = BANNER.replace("ver", version);
        out.print(banner);
    }

}
