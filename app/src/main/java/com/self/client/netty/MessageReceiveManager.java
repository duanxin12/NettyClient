package com.self.client.netty;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.zoomtech.emm.utils.netty.newnetty.MessageContent;

import org.json.JSONException;
import org.json.JSONObject;

public class MessageReceiveManager {

    public final static String TAG = "MessageReceiveManager";

    private static volatile MessageReceiveManager mMessageReceiveManager;
    private NettyClient mNettyClient;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public final static String NEW_MESSAGE_RECEIVER_BROADCAST = "NEW_MESSAGE_RECEIVER_BROADCAST";
    public final static String MESSAGE_SEND_SUCCESS_BACK_BROADCAST = "MESSAGE_SEND_SUCCESS_BACK_BROADCAST";

    private MessageReceiveManager() {
        mNettyClient = NettyClient.getInstance();
    }

    public static MessageReceiveManager getInstance() {
        if (null == mMessageReceiveManager) {
            synchronized (NettyClient.class) {
                if (null == mMessageReceiveManager) {
                    mMessageReceiveManager = new MessageReceiveManager();
                }
            }
        }
        return mMessageReceiveManager;
    }

    /**
     * Receiver信息处理
     *
     * @param msg
     */
    public void receiverMessage(final Object msg) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                MessageContent.Content content = (MessageContent.Content) msg;

                if (content.getMsgType() == MessageType.LOGIN_IN) {

                    Log.w(TAG, "登录成功！");
                    mNettyClient.setNettyClientCurrentType(NettyClientType.LOGINEDSUCCESS);

                } else if (content.getMsgType() == MessageType.DATA_SEND) {

                    try {
                        String contentStr = null;

                        if (content.getEncrypt() == 1) {
                            contentStr = doDecode(content.getContent());
                        } else {
                            contentStr = content.getContent();
                        }

                        Log.w(TAG, "接收到命令：" + contentStr);

                        JSONObject json = new JSONObject(contentStr);
                        JSONObject json1 = new JSONObject();

                        json1.put("commandCode", json.getString("commandCode"));
                        json1.put("commandId", json.getString("commandId"));
                        json1.put("strategyId", json.getString("strategyId"));

                        /**
                         * 发送接收成功消息给服务端
                         */
                        RequestSendManager.getInstance().sendBackRequest(content.getMsgId());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    /**
                     * 发送给客户端
                     */
                    sendReceiverBroadcast(content);

                } else if (content.getMsgType() == MessageType.DATA_SEND_BACK) {
                    /**
                     * 发送成功删除消息队列中的内容
                     */
                    Log.w(TAG, "消息发送成功：" + content.getMsgId());
                    sendMessageSuccessBroadcast(content);
                }
            }
        });
    }

    /**
     * 接收消息发送广播
     *
     * @param content
     */
    private void sendReceiverBroadcast(MessageContent.Content content) {
        Intent intent = new Intent();
        intent.setAction(NEW_MESSAGE_RECEIVER_BROADCAST);
        sendBroadcast(intent, content);
    }

    /**
     * 发送成功广播
     * @param content
     */
    private void sendMessageSuccessBroadcast(MessageContent.Content content) {
        Intent intent = new Intent();
        intent.setAction(MESSAGE_SEND_SUCCESS_BACK_BROADCAST);
        sendBroadcast(intent, content);
    }

    private void sendBroadcast(Intent intent, MessageContent.Content content) {
        intent.putExtra(MessageType.MSG_TYPE, content.getMsgType());
        intent.putExtra(MessageType.MSG_ID, content.getMsgId());
        if (content.getEncrypt() == 1) {
            String finalContent = doDecode(content.getContent());
            intent.putExtra(MessageType.MSG_CONTENT, finalContent);
            Log.w(TAG, "解密: " + finalContent);
        } else {
            intent.putExtra(MessageType.MSG_CONTENT, content.getContent());
        }

        mNettyClient.getContext().sendBroadcast(intent);
    }

    /**
     * 解密
     * @param content
     * @return
     */
    private String doDecode(String content) {
        if (TextUtils.isEmpty(content))
            return null;

        try {
            return AesKit.decrypt(content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
