package org.github.ponking66;

import org.github.ponking66.common.Client;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.common.Server;
import org.github.ponking66.util.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

/**
 * @author pony
 * @date 2023/5/26
 */
public class LoadYamlTest {

    @Test
    public void test(){
        InputStream resourceAsStream = ResourceUtils.getResourceAsStream("setting.yaml");
        Assert.assertNotNull(resourceAsStream);

        Server server = ProxyConfig.server();
        Client client = ProxyConfig.client();

        Assert.assertNotNull(server);
        Assert.assertNotNull(client);


    }
}
