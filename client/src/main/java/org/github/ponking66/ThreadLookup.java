package org.github.ponking66;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.github.ponking66.common.ProxyConfig;

/**
 * @author pony
 * @date 2023/10/30
 */
@Plugin(name = "natproxy", category = StrLookup.CATEGORY)
public class ThreadLookup implements StrLookup {

    @Override
    public String lookup(String key) {
        if (key.equals(ProxyConfig.ENV_PROPERTIES_LOG_FILE_NAME)) {
            return ProxyConfig.CLIENT_FILE_LOG;
        }
        return null;
    }

    @Override
    public String lookup(LogEvent event, String key) {
        return null;
    }
}
