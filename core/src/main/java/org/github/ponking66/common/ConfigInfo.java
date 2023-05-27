
package org.github.ponking66.common;

import lombok.Data;
import lombok.ToString;
import org.github.ponking66.util.ObjectUtils;
import org.github.ponking66.util.ResourceUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

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

    private ConfigInfo() {
    }

    public static ConfigInfo getInstance() throws FileNotFoundException {
        if (CONFIG_INFO == null) {
            synchronized (ConfigInfo.class) {
                if (CONFIG_INFO == null) {
                    Constructor constructor = new Constructor(ConfigInfo.class);
                    Yaml yaml = new Yaml(constructor);
                    InputStream is;
                    String path = System.getProperty("config.path");
                    if (!ObjectUtils.isEmpty(path)) {
                        is = new FileInputStream(path);
                    } else if (!ObjectUtils.isEmpty(System.getProperty("config.filename"))) {
                        String filename = System.getProperty("config.filename");
                        is = ResourceUtils.getResourceAsStream(filename);
                    } else {
                        is = ResourceUtils.getResourceAsStream("setting.yaml");
                    }
                    CONFIG_INFO = yaml.load(is);
                    return CONFIG_INFO;
                }
            }
        }
        return CONFIG_INFO;
    }
}