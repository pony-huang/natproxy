package org.github.ponking66.util;

import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.pojo.*;
import org.github.ponking66.protoctl.Header;
import org.github.ponking66.protoctl.MessageType;
import org.github.ponking66.protoctl.NettyMessage;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/7/3
 */
public class RequestResponseUtils {

    private RequestResponseUtils() {
    }

    public static NettyMessage transferResp(byte[] data, String userId) {
        NettyMessage message = new NettyMessage();
        message.setHeader(new Header().setType(MessageType.TRANSFER_RESPONSE));
        TransferResp transferResp = new TransferResp(userId, data);
        message.setBody(transferResp);
        return message;
    }

    public static NettyMessage transferResp(byte[] data, String userId, InetSocketAddress inetSocketAddress) {
        NettyMessage message = new NettyMessage();
        message.setHeader(new Header().setType(MessageType.TRANSFER_RESPONSE));
        TransferResp resp = new TransferResp(userId, data);
        resp.setRemoteAddress(inetSocketAddress);
        message.setBody(resp);
        return message;
    }

    public static NettyMessage transferRep(byte[] data, String userId) {
        NettyMessage proxyMessage = new NettyMessage();
        proxyMessage.setHeader(new Header().setType(MessageType.TRANSFER_REQUEST));
        TransferReq rep = new TransferReq(userId, data);
        proxyMessage.setBody(rep);
        return proxyMessage;
    }

    public static NettyMessage transferRep(byte[] bytes, String userId, InetSocketAddress sender) {
        NettyMessage proxyMessage = new NettyMessage();
        proxyMessage.setHeader(new Header().setType(MessageType.TRANSFER_REQUEST));
        TransferReq rep = new TransferReq(userId, bytes);
        rep.setRemoteAddress(sender);
        proxyMessage.setBody(rep);
        return proxyMessage;
    }

    public static NettyMessage proxyTunnelInfoResp(String userId) {
        ProxyTunnelInfoResp resp = new ProxyTunnelInfoResp();
        resp.setUserId(userId);
        resp.setToken(ProxyConfig.client().getKey());
        NettyMessage proxyMessage = new NettyMessage();
        proxyMessage.setHeader(new Header().setType(MessageType.CONNECT_RESPONSE));
        proxyMessage.setBody(resp);
        return proxyMessage;
    }

    public static NettyMessage loginResp(LoginResp.RespError error, Header.Status status) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_RESPONSE);
        message.setHeader(header);
        header.setStatus(status.getVal());
        message.setBody(new LoginResp(error));
        return message;
    }

    public static NettyMessage loginRep(String key) {
        NettyMessage message = new NettyMessage();
        message.setHeader(new Header().setType(MessageType.LOGIN_REQUEST));
        message.setBody(new LoginReq(key));
        return message;
    }

    public static NettyMessage heartbeatRequest() {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.HEARTBEAT_REQUEST);
        message.setHeader(header);
        return message;
    }

    public static NettyMessage disconnect(String userId) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.DISCONNECT);
        message.setHeader(header);
        message.setBody(new CloseChannelReq(userId, null));
        return message;
    }

}
