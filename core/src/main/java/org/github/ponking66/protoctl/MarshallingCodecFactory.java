package org.github.ponking66.protoctl;


import org.jboss.marshalling.*;

import java.io.IOException;

/**
 * @author Administrator
 * @version 1.0
 * @date 2014年3月15日
 */
public final class MarshallingCodecFactory {
    private static final MarshallerFactory marshallerFactory;

    private static final MarshallingConfiguration configuration = new MarshallingConfiguration();

    static {
        marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
        configuration.setVersion(4);
    }

    /**
     * 创建Jboss Marshaller
     */
    public static Marshaller buildMarshalling() throws IOException {
        return marshallerFactory.createMarshaller(configuration);
    }

    /**
     * 创建Jboss Unmarshaller
     */
    public static Unmarshaller buildUnMarshalling() throws IOException {
        return marshallerFactory.createUnmarshaller(configuration);
    }
}