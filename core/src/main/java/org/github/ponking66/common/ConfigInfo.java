
package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.ponking66.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * @author huang
 */
@Data
@ToString
public class ConfigInfo {

    private Server server;

    private Client client;

    private static volatile ConfigInfo CONFIG_INFO = null;

    private static final Logger LOGGER = LogManager.getLogger();

    private ConfigInfo() {
    }

    public static ConfigInfo getInstance() throws FileNotFoundException {
        if (CONFIG_INFO == null) {
            synchronized (ConfigInfo.class) {
                if (CONFIG_INFO == null) {
                    Constructor constructor = new Constructor(ConfigInfo.class);
                    Yaml yaml = new Yaml(constructor);
                    String filePath = System.getProperty(ProxyConfig.ENV_PROPERTIES_CONFIG_FILE_NAME);
                    InputStream is = new FileInputStream(new File(filePath));
                    // 本地运行
//                  InputStream is = ResourceUtils.getResourceAsStream(ProxyConfig.CONFIG_FILENAME);
                    CONFIG_INFO = yaml.load(is);
                    return CONFIG_INFO;
                }
            }
        }
        return CONFIG_INFO;
    }
}