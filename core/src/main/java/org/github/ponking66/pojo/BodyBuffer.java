package org.github.ponking66.pojo;

/**
 * @author pony
 * @time 0:12
 */
public interface BodyBuffer {
    void write(byte[] bytes);

    byte[] read();
}
