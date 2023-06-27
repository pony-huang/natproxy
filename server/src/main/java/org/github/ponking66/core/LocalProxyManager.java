package org.github.ponking66.core;

import org.github.ponking66.common.Proxy;
import org.github.ponking66.common.ProxyConfig;

import java.util.List;

/**
 * @author pony
 * @date 2023/6/26
 */
public class LocalProxyManager implements IProxyManager {

    @Override
    public boolean containsKey(String key) {
        return ProxyConfig.server().getKeys().contains(key);
    }

    @Override
    public List<Proxy> proxies(String clientKey) {
        return ProxyConfig.server().getProxy();
    }

    @Override
    public List<Integer> extranetPortByClientKey(String clientKey) {
        return ProxyConfig.getClientKeyExtranetPort(clientKey);
    }

}
