package org.github.ponking66.util;

import java.io.InputStream;
import java.util.Objects;

/**
 * @author pony
 * @date 2023/5/26
 */
public class ResourceUtils {

    private ResourceUtils() {
    }

    public static InputStream getResourceAsStream(String filename) {
        return ResourceUtils.class.getClassLoader().getResourceAsStream(filename);
    }

    public static String getPath(String filename) {
        return Objects.requireNonNull(ResourceUtils.class.getClassLoader().getResource("")).getPath() + filename;
    }

}
