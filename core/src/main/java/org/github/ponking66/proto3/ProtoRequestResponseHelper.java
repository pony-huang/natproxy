package org.github.ponking66.proto3;

import com.google.protobuf.ByteString;
import org.github.ponking66.common.ProxyConfig;
import org.github.ponking66.util.ObjectUtils;

import java.net.InetSocketAddress;

/**
 * @author pony
 * @date 2023/7/3
 */
public class ProtoRequestResponseHelper {

    private final static int VERSION = 1;

    private ProtoRequestResponseHelper() {
    }

    public static NatProxyProtos.Packet transferResponse(byte[] data, String userId) {

        NatProxyProtos.TransferResponse.Builder body = NatProxyProtos.TransferResponse.newBuilder()
                .setContent(ByteString.copyFrom(data))
                .setUserId(userId);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.TRANSFER_RESPONSE)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setTransferResponse(body).build();
    }

    public static NatProxyProtos.Packet transferResponse(byte[] data, String userId, InetSocketAddress inetSocketAddress) {
        NatProxyProtos.TransferResponse.Builder body = NatProxyProtos.TransferResponse.newBuilder()
                .setContent(ByteString.copyFrom(data))
                .setUserId(userId)
                .setRemoteAddressHost(inetSocketAddress.getHostName())
                .setRemoteAddressPort(inetSocketAddress.getPort());

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.TRANSFER_RESPONSE)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setTransferResponse(body).build();
    }

    public static NatProxyProtos.Packet transferRequest(byte[] data, String userId) {
        NatProxyProtos.TransferRequest.Builder body = NatProxyProtos.TransferRequest.newBuilder()
                .setContent(ByteString.copyFrom(data))
                .setUserId(userId);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.TRANSFER_REQUEST)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setTransferRequest(body).build();
    }

    public static NatProxyProtos.Packet transferRequest(byte[] bytes, String userId, InetSocketAddress sender) {

        NatProxyProtos.TransferRequest.Builder body = NatProxyProtos.TransferRequest.newBuilder()
                .setContent(ByteString.copyFrom(bytes))
                .setUserId(userId).setRemoteAddressHost(sender.getHostName())
                .setRemoteAddressPort(sender.getPort());

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.TRANSFER_REQUEST)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setTransferRequest(body).build();
    }

    public static NatProxyProtos.Packet proxyTunnelInfoResponse(String userId) {

        NatProxyProtos.ProxyTunnelInfoResponse.Builder body = NatProxyProtos.ProxyTunnelInfoResponse.newBuilder()
                .setUserId(userId).setToken(ProxyConfig.client().getKey());

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.CONNECT_RESPONSE)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setProxyTunnelInfoResponse(body).build();
    }

    public static NatProxyProtos.Packet loginResponse(NatProxyProtos.LoginResponse.ResponseError error, NatProxyProtos.Header.Status status) {
        NatProxyProtos.LoginResponse.Builder body = NatProxyProtos.LoginResponse.newBuilder()
                .setError(error);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.LOGIN_RESPONSE)
                .setVersion(VERSION)
                .setStatus(status);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setLoginResponse(body).build();
    }

    public static NatProxyProtos.Packet loginRequest(String key) {

        NatProxyProtos.LoginRequest.Builder body = NatProxyProtos.LoginRequest.newBuilder()
                .setClientKey(key);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.LOGIN_REQUEST)
                .setVersion(VERSION)
                .setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setLoginRequest(body).build();
    }

    public static NatProxyProtos.Packet heartbeatRequest() {

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.HEARTBEAT_REQUEST)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).build();
    }

    public static NatProxyProtos.Packet disconnect(String userId) {

        NatProxyProtos.CloseChannelRequest.Builder body = NatProxyProtos.CloseChannelRequest.newBuilder();
        if (ObjectUtils.isEmpty(userId)) {
            body.setUserId("");
        } else {
            body.setUserId(userId);
        }

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.DISCONNECT)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setCloseChannelRequest(body).build();
    }

    public static NatProxyProtos.Packet disconnect(String userId, String token) {

        NatProxyProtos.CloseChannelRequest.Builder body = NatProxyProtos.CloseChannelRequest.newBuilder()
                .setToken(token)
                .setUserId(userId);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.DISCONNECT)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setCloseChannelRequest(body).build();
    }


    public static NatProxyProtos.Packet connect(String host, int port, String type, String userId) {
        NatProxyProtos.ProxyTunnelInfoRequest.Builder body = NatProxyProtos.ProxyTunnelInfoRequest.newBuilder()
                .setHost(host)
                .setPort(port)
                .setType(type)
                .setUserId(userId);

        NatProxyProtos.Header.Builder header = NatProxyProtos.Header.newBuilder()
                .setType(NatProxyProtos.Header.MessageType.CONNECT_REQUEST)
                .setVersion(VERSION).setStatus(NatProxyProtos.Header.Status.SUCCESS);

        return NatProxyProtos.Packet.newBuilder().setHeader(header).setProxyTunnelInfoRequest(body).build();
    }

}
