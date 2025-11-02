package fan.summer.hmoneta.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 类的详细说明
 *
 * @author phoebej
 * @version 1.00
 * @Date 2025/11/1
 */
public class WebUtil {

    public static boolean validateFormat(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        // 验证 xxx.xxx.xxx 格式
        // 每个部分只能包含字母、数字、连字符，且不能以连字符开头或结尾
        String pattern = "^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                "\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?" +
                "\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?$";

        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(url.trim());

        return m.matches();
    }

    public static Map<String, String> extractParts(String url) {
        if (!validateFormat(url)) {
            return null;
        }
        String[] parts = url.trim().split("\\.");
        Map<String, String> map = new HashMap<>();
        map.put("sub", parts[0]);
        map.put("host", parts[1] + "." + parts[2]);
        return map;
    }
}
