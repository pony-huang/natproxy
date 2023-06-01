package org.github.ponking66;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author pony
 * @date 2023/6/1
 */
public class TSLSSLTest {

    @Test
    public void client() throws SSLException {
        clientSslContext();
    }


    private SslContext clientSslContext() throws SSLException {
//        File crt = new File("D:\\WorkPlace\\natproxy\\cert\\client.crt");
//        File key = new File("D:\\WorkPlace\\natproxy\\cert\\pkcs8_client.key");
//        File ca = new File("D:\\WorkPlace\\natproxy\\cert\\ca.crt");
//        return SslContextBuilder.forClient().keyManager(crt, key).trustManager(ca).build();
        File crt = new File("D:\\WorkPlace\\natproxy\\cert\\client-cert.pem");
        File key = new File("D:\\WorkPlace\\natproxy\\cert\\client-key.pem");
        File ca = new File("D:\\WorkPlace\\natproxy\\cert\\ca-cert.pem");
     return SslContextBuilder.forClient().keyManager(crt, key).trustManager(ca).build();
    }

    @Test
    public void server() throws SSLException {
        serverSslContext();
    }

    private SslContext serverSslContext() throws SSLException {
        File crt = new File("D:\\WorkPlace\\natproxy\\cert\\server.crt");
        File key = new File("D:\\WorkPlace\\natproxy\\cert\\pkcs8_server.key");
        File ca = new File("D:\\WorkPlace\\natproxy\\cert\\ca.crt");
        return SslContextBuilder.forClient().keyManager(crt, key).trustManager(ca).build();
    }

    @Test
    public void test() throws SSLException {
        SslHandler client = new SslHandler(clientSslContext().newEngine(ByteBufAllocator.DEFAULT));
        SslHandler server = new SslHandler(serverSslContext().newEngine(ByteBufAllocator.DEFAULT));
        EmbeddedChannel channel = new EmbeddedChannel(client, server);


        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes("Hello world".getBytes(StandardCharsets.UTF_8));

        channel.writeOutbound(buf);

        ByteBuf msg1 = channel.readOutbound();

        channel.writeInbound(buf);

        ByteBuf msg2 = channel.readInbound();

    }
}
