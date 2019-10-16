package com.self.client.netty;

/**
 * 消息类型
 */
public class MessageType {

    /**
     * 心跳请求
     */
    public static final int HEART_SEND = 1;

    /**
     * 登陆请求
     */
    public static final int LOGIN_IN = 2;

    /**
     * 数据发送
     */
    public static final int DATA_SEND = 4;

    /**
     * 数据接收返回
     */
    public static final int DATA_SEND_BACK = 5;

    /**
     * 消息类型
     */
    public final static String MSG_TYPE = "msg_type";

    /**
     * 消息ID
     */
    public final static String MSG_ID = "msg_id";

    /**
     * 消息内容
     */
    public final static String MSG_CONTENT = "msg_content";
}
