package org.github.ponking66.util;

/**
 * @author pony
 * @date 2023/5/26
 */
public class ObjectUtils {

    private ObjectUtils() {
    }

    public static boolean isEmpty(Object obj) {
        if (obj instanceof String str) {
            return str.length() == 0;
        }
        return obj == null;
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }
}
