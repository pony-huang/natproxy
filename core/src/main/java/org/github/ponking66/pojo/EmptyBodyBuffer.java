package org.github.ponking66.pojo;

/**
 * @author pony
 * @time 0:13
 */
public class EmptyBodyBuffer implements BodyBuffer {
    @Override
    public void write(byte[] bytes) {

    }

    @Override
    public byte[] read() {
        return new byte[0];
    }
}
