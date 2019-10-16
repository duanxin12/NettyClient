package com.self.client.netty;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

/**
 * 用于管理网络变化监听
 */
class NetworkMonitorService extends Service {

    private NetworkChangeReceiver mReceiver;
    private NettyClient mNettyClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mNettyClient = NettyClient.getInstance();
        registerNetworkReceiver();
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void registerNetworkReceiver() {
        mReceiver = new NetworkChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, filter);
    }

    class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                if (mNettyClient.isNetworkAvailable()) {
                    mNettyClient.sendMessageToChangeHandler(NettyClient.NETTY_CONNECT_OPERATE);
                } else {
                    mNettyClient.sendMessageToChangeHandler(NettyClient.NETTY_DISCONNECT_OPERATE);
                }
            }
        }
    }
}
