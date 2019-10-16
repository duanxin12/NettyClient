package com.self.client.netty;

import android.util.Log;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 通道监听
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<Object> {
    private static final String TAG = "NettyClientHandler";

    public NettyClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Log.w(TAG,"通道出现异常！");
        NettyClient.getInstance().sendMessageToChangeHandler(NettyClient.NETTY_DISCONNECT_OPERATE);
    }



    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object msg) throws Exception {
        MessageReceiveManager.getInstance().receiverMessage(msg);
    }

    /**
     * 通道空闲监听
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

        //判断是否登录
        if (NettyClient.getInstance().getNettyClientCurrentType() != NettyClientType.LOGINEDSUCCESS ) {
            return;
        }

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                //发送心跳
                RequestSendManager.getInstance().sendHeartRequest();
            }
        }
    }

}
