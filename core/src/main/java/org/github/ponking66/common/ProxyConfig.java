package org.github.ponking66.common;

import org.github.ponking66.pojo.ProxyTunnelInfoReq;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pony
 * @date 2023/5/15
 */
public class ProxyConfig {

    private static final ConfigInfo CONFIG_INFO;

    public static final String CLIENT_CONFIG_FILENAME = "client_config.yaml";

    public static final String SERVER_CONFIG_FILENAME = "server_config.yaml";

    public static final String SERVER_FILE_LOG = "server.log";

    public static final String CLIENT_FILE_LOG = "client.log";

    public static final String ENV_PROPERTIES_CONFIG_FILE_NAME = "config.filename";

    public static final String ENV_PROPERTIES_LOG_FILE_NAME = "log.filename";

    public static final String ENV_PROPERTIES_PATH = "config.path";

    public static final String ENV_PROPERTIES_GLOBAL_CONFIG_FILE_NAME = "setting.yaml";

    public static final int READER_IDLE_TIME_SECONDS = 0;
    public static final int WRITER_IDLE_TIME_SECONDS = 0;
    public static final int ALL_IDLE_TIME_SECONDS = 30;

    static {
        try {
            CONFIG_INFO = ConfigInfo.getInstance();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static ProxyTunnelInfoReq getProxyInfo(int port) {
        ProxyTunnelInfoReq proxyTunnelInfoReq = null;
        for (Proxy p : CONFIG_INFO.getServer().getProxy()) {
            if (p.getExtranetPort() == port) {
                proxyTunnelInfoReq = new ProxyTunnelInfoReq();
                proxyTunnelInfoReq.setHost(p.getHost());
                proxyTunnelInfoReq.setPort(p.getIntranetPort());
                proxyTunnelInfoReq.setType(p.getType());
            }
        }
        return proxyTunnelInfoReq;
    }

    public static List<Integer> getClientKeyExtranetPort(String key) {
        List<Integer> ports = new ArrayList<>();
        for (Proxy p : CONFIG_INFO.getServer().getProxy()) {
            if (p.getKey().equals(key)) {
                ports.add(p.getExtranetPort());
            }
        }
        return ports;
    }

    public static int getServerPort() {
        return CONFIG_INFO.getServer().getPort();
    }

    public static String getServerHost() {
        return CONFIG_INFO.getServer().getHost();
    }

    public static Client client() {
        return CONFIG_INFO.getClient();
    }

    public static Server server() {
        return CONFIG_INFO.getServer();
    }
}
