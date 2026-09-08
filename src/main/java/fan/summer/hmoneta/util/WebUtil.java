package fan.summer.hmoneta.util;

import java.util.Arrays;
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

        // 验证 xxx.xxx[.xxx...] 格式（至少两段标签，支持根域名与多级子域名）
        // 每个部分只能包含字母、数字、连字符，且不能以连字符开头或结尾
        String label = "[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?";
        String pattern = "^" + label + "(\\." + label + ")+$";

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
        // 末两段视为主域名，其余为子域名；仅两段（根域名）时 sub 为 null。
        // 对 example.co.uk 这类多段后缀，由 DNS 提供商自行向上探测真实 Zone
        map.put("host", parts[parts.length - 2] + "." + parts[parts.length - 1]);
        if (parts.length == 2) {
            map.put("sub", null);
        } else {
            map.put("sub", String.join(".", Arrays.copyOfRange(parts, 0, parts.length - 2)));
        }
        return map;
    }
}
