package com.self.client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.self.client.netty.NettyClient;
import com.self.client.netty.NettyClientConfig;
import com.self.client.netty.NettyClientStatusListener;
import com.self.client.netty.NettyClientType;

public class MainService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        initLongLink();
        return START_REDELIVER_INTENT;
    }

    /**
     * 配置长连接差数，启动长连接
     */
    private void initLongLink() {

        NettyClient.initNettyClient(this,
                new NettyClientConfig.Builder()
                        .setSelfAlias("123456789")
                        .setTargetHost("netty.client.net")
                        .setTargetPort(8088)
                        .setHeartbeatTime(300000)
                        .setEncrypt(true)
                        .build(),
                new NettyClientStatusListener() {
                    @Override
                    public void nettyClientStatus(String mNettyClientType) {

                        if (mNettyClientType.equals(NettyClientType.ISCONNECTTING)) {

                        } else if (mNettyClientType.equals(NettyClientType.CONNECTEDSUCCESS)) {

                        } else if (mNettyClientType.equals(NettyClientType.CONNECTEDFAILD)) {

                        } else if (mNettyClientType.equals(NettyClientType.ISLOGINNING)) {

                        } else if (mNettyClientType.equals(NettyClientType.LOGINEDSUCCESS)) {

                            //start send messages

                        } else if (mNettyClientType.equals(NettyClientType.LOGINEDFAILD)) {

                        } else if (mNettyClientType.equals(NettyClientType.DISCONNECTED)) {

                        } else if (mNettyClientType.equals(NettyClientType.ISRECONNECTTING)) {

                        } else if (mNettyClientType.equals(NettyClientType.ERROR)) {

                        } else if (mNettyClientType.equals(NettyClientType.NONE)) {

                        }
                    }
                });

    }
}
