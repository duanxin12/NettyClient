package com.self.client.netty;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {

    private final static String TAG = "NettyClient";

    private static volatile NettyClient mNettyClient;
    private static NettyClientConfig mNettyClientConfig;
    private static NettyClientStatusListener mNettyStatusListener;
    private EventLoopGroup group;
    private Channel channel;
    private Bootstrap mBootstrap;
    private static Context mContext;

    private static String mNettyCurrentType;

    //用于计算连续连接失败的次数，该次数可以用于延长连接的间隔
    private AtomicInteger mAtomicInteger = new AtomicInteger(1);

    private HandlerThread mNettyOperateHandleThread = null;
    private Handler mNettyOperateHandler;

    //用于标识Netty是执行连接操作 Or 断开操作
    public final static int NETTY_CONNECT_OPERATE = 1;
    public final static int NETTY_DISCONNECT_OPERATE = 2;

    private final static int NETTY_OPERATE_MARK = 110011;

    /**
     * 初始化NettyClient
     *
     * @param context
     * @param nettyClientConfig
     * @param nettyStatusListener
     */
    public static void initNettyClient(Context context, NettyClientConfig nettyClientConfig, NettyClientStatusListener nettyStatusListener) {
        if (mNettyClientConfig == null) {
            throw new NullPointerException("Please config NettyClientConfig!");
        }

        if (mNettyStatusListener != null) {
            mNettyStatusListener = nettyStatusListener;
        }

        mContext = context;
        mNettyClientConfig = nettyClientConfig;
        newNettyClient();
        startNetworkMonitorService();
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * 创建NettyClient对象
     */
    private static void newNettyClient() {
        if (null == mNettyClient) {
            synchronized (NettyClient.class) {
                if (null == mNettyClient) {
                    mNettyClient = new NettyClient();
                }
            }
        }
    }

    private NettyClient() {

        /**
         * 用于执行长连接的启动与断开
         */
        mNettyOperateHandleThread = new HandlerThread("NettyOperateThread");
        mNettyOperateHandleThread.start();

        mNettyOperateHandler = new Handler(mNettyOperateHandleThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == NETTY_OPERATE_MARK) {
                    mNettyClientStatusChange(msg.arg1);
                }
            }
        };
    }

    /**
     * @param mChangeType
     */
    public void sendMessageToChangeHandler(int mChangeType) {
        Message msg = new Message();
        msg.arg1 = mChangeType;
        //用于标识netty的操作命令
        msg.what = NETTY_OPERATE_MARK;

        /**
         * 当出现新的操作命令时，应覆盖旧的操作命令；3秒延时，用于防止网络出现频繁波动，也就是说连接与断开操作至少存在3秒间隔
         */
        if (mNettyOperateHandler.hasMessages(msg.what)) {
            mNettyOperateHandler.removeMessages(msg.what);
        }

        mNettyOperateHandler.sendMessageDelayed(msg, 3000);
    }

    /**
     * NETTY_CONNECT_OPERATE：netty connect NETTY_DISCONNECT_OPERATE：netty disconnect
     *
     * @param mNettyChange
     */
    private void mNettyClientStatusChange(int mNettyChange) {
        switch (mNettyChange) {
            case NETTY_CONNECT_OPERATE:
                reconnect();
                break;
            case NETTY_DISCONNECT_OPERATE:
                disconnect();
                break;
            default:
                break;
        }
    }

    /**
     * 初始化Netty配置
     */
    private void initNettyConfig() {
        setNettyClientCurrentType(NettyClientType.NONE);
        group = new NioEventLoopGroup();
        mBootstrap = new Bootstrap().group(group)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, mNettyClientConfig.connectionTimeout)
                .channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer());
    }

    /**
     * 获取NettyClient对象
     *
     * @return
     */
    public static NettyClient getInstance() {
        if (mNettyClient == null) {
            throw new NullPointerException("Please transfer initNettyClient first!");
        }
        return mNettyClient;
    }

    /**
     * 启动网络监听服务
     */
    private static void startNetworkMonitorService() {
        Intent intent = new Intent(mContext, NetworkMonitorService.class);
        mContext.startService(intent);
    }

    /**
     * 获取NettyClient配置
     *
     * @return
     */
    public NettyClientConfig getNettyClientConfig() {
        return mNettyClientConfig;
    }

    /**
     * 获取当前NettyClient状态
     *
     * @return
     */
    public String getNettyClientCurrentType() {
        return mNettyCurrentType;
    }

    /**
     * 设置NettyClient当前状态并调用状态回调
     *
     * @param mNettyClientCurrentType
     */
    public void setNettyClientCurrentType(String mNettyClientCurrentType) {
        mNettyCurrentType = mNettyClientCurrentType;
        sendNettyClientCurrentTypeToLister(mNettyCurrentType);
    }

    /**
     * 回调NettyClient当前状态
     */
    private void sendNettyClientCurrentTypeToLister(String mNettyCurrentType) {
        if (mNettyStatusListener != null) {
            mNettyStatusListener.nettyClientStatus(mNettyCurrentType);
        }
    }

    /**
     * 开启Netty连接
     */
    private void startConnect() {
        initNettyConfig();
        connect();
    }

    /**
     * Netty连接管理
     */
    private void connect() {

        if (channel != null) {
            Log.w(TAG, "通道已存在！");
            return;
        }
        /**
         * 在NettyClient状态为ISCONNECTTING、ISLOGINNING、ISRECONNECTTING的情况下不允许启动连接
         */
        if (mNettyCurrentType.equals(NettyClientType.ISCONNECTTING)
                || mNettyCurrentType.equals(NettyClientType.ISLOGINNING)
                || mNettyCurrentType.equals(NettyClientType.ISRECONNECTTING)) {
            Log.w(TAG, mNettyCurrentType + "情况下不允许启动连接！");
            return;
        }

        Log.w(TAG, "开始连接！");

        setNettyClientCurrentType(NettyClientType.ISCONNECTTING);

        try {

            /**
             * 域名转换
             */
            String ipAddress = getIpAddressFromHost(mNettyClientConfig.host);

            if (TextUtils.isEmpty(ipAddress)) {
                Log.w(TAG, "域名转换失败！");
                return;
            } else {
                Log.w(TAG, "域名转换成功：host = " + ipAddress + " port = " + mNettyClientConfig.port);
            }

            Log.w(TAG, "发送连接请求！");
            /**
             * 服务器连接
             */
            mBootstrap.connect(ipAddress, mNettyClientConfig.port).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future != null && future.isSuccess()) {

                        setNettyClientCurrentType(NettyClientType.CONNECTEDSUCCESS);
                        Log.w(TAG, "连接成功！");
                        //重置重连时间
                        mAtomicInteger.set(1);
                        channel = future.channel();
                        //发送登录请求
                        Log.w(TAG, "发送登录请求！");
                        //登录
                        sendLoginRequest();

                    } else {
                        setNettyClientCurrentType(NettyClientType.CONNECTEDFAILD);
                        Log.w(TAG, "连接失败：" + future.cause().toString());
                        reconnect();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            setNettyClientCurrentType(NettyClientType.ERROR);
            reconnect();
        }

    }

    /**
     * 请求登录
     */
    private void sendLoginRequest() {
        RequestSendManager.getInstance().sendLoginRequest();
    }

    public Channel getChannel() {
        return channel;
    }

    /**
     * 域名转IP
     *
     * @param host
     * @return
     */
    private String getIpAddressFromHost(String host) {
        if (TextUtils.isEmpty(host)) {
            throw new NullPointerException("please config host first!");
        }

        String iPAddress = "";

        /**
         * 判断host中是否包含字母
         */
        String regex = ".*[a-zA-Z]+.*";
        Matcher matcher = Pattern.compile(regex).matcher(host);
        if (matcher.matches()) {
            InetAddress returnStr = null;
            if (isNetworkAvailable()) {
                try {
                    returnStr = InetAddress.getByName(host);
                    iPAddress = returnStr.getHostAddress();
                } catch (UnknownHostException e) {
                    return iPAddress;
                }
            } else {
                Log.w(TAG, "网络异常！");
            }
        } else {
            iPAddress = host;
        }
        return iPAddress;
    }

    /**
     * 断开Netty
     */
    private synchronized void disconnect() {
        if (group != null) {
            try {
                channel.closeFuture().sync();
                channel = null;
                group.shutdownGracefully().sync();
                group = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.w(TAG, "长连接关闭!");
            setNettyClientCurrentType(NettyClientType.DISCONNECTED);
        } else {
            setNettyClientCurrentType(NettyClientType.NONE);
        }
    }

    /**
     * 重连
     *
     * @return
     */
    private synchronized void reconnect() {

        if (channel != null) {
            disconnect();
        }

        //第一次连接时不会延时
        int connectNumber = mAtomicInteger.incrementAndGet() - 2;
        if (connectNumber >= 1) {
            try {
                Thread.sleep((connectNumber)
                        * mNettyClientConfig.reconnectIntervalTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (isNetworkAvailable()) {
            setNettyClientCurrentType(NettyClientType.ISRECONNECTTING);
            startConnect();
            return;
        }

    }

    /**
     * 网络是否可用
     *
     * @return
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mConnectivityManager == null) {
            return false;
        }
        NetworkInfo mNetWorkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetWorkInfo == null) {
            return false;
        }
        return mNetWorkInfo.isConnected() && mNetWorkInfo.isAvailable();
    }

}
