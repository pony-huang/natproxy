package org.github.ponking66.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.core.ProxyChannelManager;
import org.github.ponking66.core.UsersApplication;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

import java.util.List;

/**
 * 处理代理客户端授权，
 * 请求必 uri=clientKey（代理客户端秘钥）
 *
 * @author pony
 * @date 2023/4/28
 */
public class ServerLoginAuthHandler extends BaseHandler {


    private final List<UsersApplication> usersApplications;

    public ServerLoginAuthHandler(List<UsersApplication> usersApplications) {
        this.usersApplications = usersApplications;
    }

    private NettyMessage buildResponse(Header.Status status) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_RESPONSE);
        header.setStatus(status.getVal());
        message.setHeader(header);
        return message;
    }

    @Override
    public void handleRead(ChannelHandlerContext ctx, NettyMessage message) {
        String body = (String) message.getBody();
        if (!ProxyConfig.server().getKeys().contains(body)) {
            LOGGER.info("Authentication failure!");
            ctx.writeAndFlush(buildResponse(Header.Status.FAILED));
            ctx.close();
        } else {
            LOGGER.info("Authentication success!");
            ctx.writeAndFlush(buildResponse(Header.Status.SUCCESS));
            bindProxySeverChannel(ctx, message);
        }
    }

    private void bindProxySeverChannel(ChannelHandlerContext ctx, NettyMessage message) {
        if (message.getBody() instanceof String clientKey) {
            // 获取该客户端下的映射端口
            List<Integer> ports = ProxyConfig.getClientKeyExtranetPort(clientKey);
            // 授权失败，客户端秘钥错误
            if (ports.isEmpty()) {
                LOGGER.warn("Error clientKey: {}", clientKey);
                ctx.close();
            } else {
                // 第一次建立连接 cmdChannel 应该为  null
                Channel cacheCmdChannel = ProxyChannelManager.getCmdChannel(clientKey);
                // 第二次试图在建立 cmdChannel
                // 授权失败，cmdChannel 已经存在
                if (cacheCmdChannel != null) {
                    LOGGER.warn("Channel already exists for clientKey, channel: {}, clientKey: {}", cacheCmdChannel, clientKey);
                    ctx.close();
                } else {
                    LOGGER.info("Bind port, clientKey: {}, ports: [{}], channel: {}", clientKey, ports, ctx.channel());
                    Channel cmdChannel = ctx.channel();
                    // 授权成功，设置cmdChannel相关的映射关系，缓存cmdChannel
                    ProxyChannelManager.addCmdChannel(ports, clientKey, cmdChannel);
                    try {
                        // 开启用户端口监听
                        for (UsersApplication usersApplication : usersApplications) {
                            usersApplication.start(clientKey);
                        }
                    } catch (Exception e) {
                        LOGGER.error("start user ports [{}] error, clientKey is [{}]", ports, clientKey);
                        ProxyChannelManager.removeCmdChannel(cmdChannel);
                        ctx.close();
                    }
                }
            }
        } else {
            LOGGER.warn("Parameter maybe error or null.");
        }
    }

    @Override
    public byte getMessageType() {
        return MessageType.LOGIN_REQUEST;
    }

}
