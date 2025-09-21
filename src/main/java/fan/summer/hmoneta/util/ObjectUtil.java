package fan.summer.hmoneta.util;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/**
 * 提供一组工具方法，用于处理对象是否为空或非空的判断。
 */
public class ObjectUtil {
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof CharSequence) {
            return ((CharSequence) obj).isEmpty();
        } else if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        } else if (obj instanceof Iterable) {
            return !((Iterable<?>) obj).iterator().hasNext();
        } else if (obj instanceof Iterator) {
            return !((Iterator<?>) obj).hasNext();
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        }
        return false;
    }
    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
}
