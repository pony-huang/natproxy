package org.github.ponking66.protoctl;

/**
 * @author pony
 * @date 2023/5/7
 */
public class MessageType {

    public static byte LOGIN_REQUEST = 0x01;
    public static byte LOGIN_RESPONSE = 0x02;
    public static byte HEARTBEAT_REQUEST = 0x03;
    public static byte HEARTBEAT_RESPONSE = 0x04;
    public static byte CONNECT_RESPONSE = 0x05;
    public static byte CONNECT_REQUEST = 0x06;
    public static byte DISCONNECT = 0x07;

    public static byte DISCONNECT_PROXY_TUNNEL = 0x08;
    public static byte TRANSFER_REQUEST = 0x09;
    public static byte TRANSFER_RESPONSE = 0x0a;
}
