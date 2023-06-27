package org.github.ponking66.core;

import org.github.ponking66.common.Proxy;

import java.util.List;

public interface IProxyManager {

    /**
     * 是否存在client_key
     *
     * @param key client_key
     */
    boolean containsKey(String key);

    /**
     * client_key 当前代理配置
     *
     * @param clientKey client_key
     */
    List<Proxy> proxies(String clientKey);

    /**
     * client_key 所有代理配置中服务代理绑定端口
     *
     * @param clientKey client_key
     */
    List<Integer> extranetPortByClientKey(String clientKey);
}
