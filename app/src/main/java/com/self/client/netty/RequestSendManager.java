package com.self.client.netty;

import android.text.TextUtils;
import android.util.Log;
import com.zoomtech.emm.utils.netty.newnetty.MessageContent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class RequestSendManager {
    private final static String TAG = "RequestSendManager";
    private static volatile RequestSendManager mRequestSendManager;
    private Channel mChannel;
    private NettyClient mNettyClient;
    private NettyClientConfig mNettyClientConfig;

    /**
     * 登录请求专用ID
     */
    private final static int LOGIN_REQUEST_ID = 10;

    /**
     * 心跳请求专用ID
     */
    private final static int HEART_REQUEST_ID = 11;

    private RequestSendManager() {
        mNettyClient = NettyClient.getInstance();
        mNettyClientConfig = mNettyClient.getNettyClientConfig();
        mChannel = mNettyClient.getChannel();
    }

    public static RequestSendManager getInstance() {
        if (null == mRequestSendManager) {
            synchronized (NettyClient.class) {
                if (null == mRequestSendManager) {
                    mRequestSendManager = new RequestSendManager();
                }
            }
        }
        return mRequestSendManager;
    }

    /**
     * 执行登录请求
     */
    public void sendLoginRequest() {
        login(new Request(MessageType.LOGIN_IN, 10, "login!"));
    }

    private void login(Request mLoginRequest) {
        sendMessage(mLoginRequest);
    }

    /**
     * 发送心跳请求
     */
    public void sendHeartRequest() {
        if (!mNettyClient.getNettyClientCurrentType().equals(NettyClientType.LOGINEDSUCCESS)) {
            Log.w(TAG, "请先登录成功！");
            return;
        }
        heart(new Request(MessageType.HEART_SEND, 11, "login!"));
    }

    private void heart(Request mHeartRequest) {
        sendMessage(mHeartRequest);
    }

    /**
     * 接收到消息之后告知服务器已收到
     */
    public void sendBackRequest(int requestId) {
        back(new Request(MessageType.DATA_SEND_BACK, requestId, "success!"));
    }

    private void back(Request mBackRequest) {
        sendMessage(mBackRequest);
    }

    /**
     * 发送一般请求：需在登录成功后再调用
     */
    public void sendGeneralRequest(int requestId, String requestContent) {
        if (!mNettyClient.getNettyClientCurrentType().equals(NettyClientType.LOGINEDSUCCESS)) {
            Log.w(TAG, "请先登录成功！");
            return;
        }
        general(new Request(MessageType.DATA_SEND, requestId, requestContent));
    }

    private void general(Request mGeneralRequest) {
        sendMessage(mGeneralRequest);
    }

    /**
     * 发送请求
     * @param mRequest
     */
    private void sendMessage(final Request mRequest) {

        if (mRequest == null) {
            throw new NullPointerException("Request is null!");
        }

        /**
         * 如果未登录，则只能发送登录请求
         */
        if (!mNettyClient.getNettyClientCurrentType().equals(NettyClientType.LOGINEDSUCCESS)) {
            if (mRequest.requestId != 10) {
                Log.w(TAG, "netty has not login!");
                return;
            }
        }

        mChannel.writeAndFlush(requestToMessage(mRequest)).addListener(new FutureListener() {
            @Override
            public void success(ChannelFuture future) {
                switch(mRequest.requestId) {
                    case LOGIN_REQUEST_ID: //登录
                        Log.w(TAG, "登录写入通道成功!");
                        mNettyClient.setNettyClientCurrentType(NettyClientType.ISLOGINNING);
                        break;
                    case HEART_REQUEST_ID: //心跳
                        Log.w(TAG, "心跳写入通道成功!");
                        break;
                    default: //普通消息
                        Log.w(TAG, "消息写入通道成功: " + "requestId = " + mRequest.requestId);
                        break;
                }

            }

            @Override
            public void error(ChannelFuture future) {
                switch(mRequest.requestId) {
                    case LOGIN_REQUEST_ID: //登录
                        Log.w(TAG, "登录写入通道失败!");
                        mNettyClient.setNettyClientCurrentType(NettyClientType.LOGINEDFAILD);
                        break;
                    case HEART_REQUEST_ID: //心跳
                        Log.w(TAG, "心跳写入通道失败!");
                        break;
                    default: //普通消息
                        Log.w(TAG, "消息写入通道失败: " + "requestId = " + mRequest.requestId);
                        break;
                }

            }
        });
    }

    /**
     * 数据打包
     *
     * @param mRequest
     * @return
     */
    public MessageContent.Content requestToMessage(Request mRequest) {

        MessageContent.Content.Builder builder = MessageContent.Content.newBuilder();
        builder.setMsgType(mRequest.requestType);
        builder.setMsgId(mRequest.requestId);
        builder.setDeviceId(mNettyClientConfig.alias);
        builder.setContent(mRequest.requestContent);

        Log.w(TAG, "数据打包：" + mRequest.toString());

        /**
         * 是否加密
         */
        if (mNettyClientConfig.isEncrypt) {
            builder.setEncrypt(1);

            String content = doEncode(builder.getContent());
            if (TextUtils.isEmpty(content)) {
                builder.setContent("content is empty!");
            } else {
                builder.setContent(content);
            }
        } else {
            builder.setEncrypt(0);
        }

        return builder.build();
    }

    /**
     * 消息加密
     * @param content
     * @return
     */
    private String doEncode(String content) {
        if (TextUtils.isEmpty(content))
            return null;

        try {
            return AesKit.encrypt(content);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private abstract class FutureListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future != null && future.isSuccess()) {
                success(future);
            } else {
                error(future);
            }
        }

        public abstract void success(ChannelFuture future);

        public abstract void error(ChannelFuture future);
    }
}
