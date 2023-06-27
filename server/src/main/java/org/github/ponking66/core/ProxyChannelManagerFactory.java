package org.github.ponking66.core;

/**
 * @author pony
 * @date 2023/6/26
 */
public class ProxyChannelManagerFactory {

    public static IProxyManager getProxyChannelManager() {
        return new LocalProxyManager();
    }
}
