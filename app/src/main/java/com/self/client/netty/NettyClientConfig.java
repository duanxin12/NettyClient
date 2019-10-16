package com.self.client.netty;

public class NettyClientConfig {

    private NettyClientConfig() {

    }

    /**
     * 网址
     */
    public String host = "netty.client.net";

    /**
     * 端口
     */
    public int port = 8088;

    /**
     * 连接超时时间
     */
    public int connectionTimeout = 30000;

    /**
     * 心跳时间间隔(写空闲)，默认为5分钟
     */
    public int heartbeatTime = 300000;

    /**
     * 响应超时时间(读空闲监测 0表示不监测读)
     */
    public int readIdleTime = 0;//30000;

    /**
     * 重连时间间隔
     */
    public int reconnectIntervalTime = 20000;

    /**
     * 设置别名
     */
    public String alias = "0000";

    /**
     * 配置是否加密
     */
    public boolean isEncrypt = false;

    /**
     * 是否重发
     */
    public boolean isResend = false;

    /**
     * 重发时间
     */
    public long resendInterval = 3000;

    public static class Builder {

        private NettyClientConfig mNettyClientConfig;

        public Builder() {
            mNettyClientConfig = new NettyClientConfig();
        }

        /**
         * 配置网址
         * @param host
         */
        public Builder setTargetHost(String host) {
            mNettyClientConfig.host = host;
            return this;
        }

        /**
         * 配置端口
         * @param port
         */
        public Builder setTargetPort(int port) {
            mNettyClientConfig.port = port;
            return this;
        }


        /**
         * 配置连接超时
         * @param connectionTimeout
         */
        public Builder setConnectionTime(int connectionTimeout) {
            mNettyClientConfig.connectionTimeout = connectionTimeout;
            return this;
        }

        /**
         * 配置心跳超时
         * @param heartbeatTime
         */
        public Builder setHeartbeatTime(int heartbeatTime) {
            mNettyClientConfig.heartbeatTime = heartbeatTime;
            return this;
        }

        /**
         * 配置响应超时
         * @param readIdleTime
         */
        public Builder setReadIdleTime(int readIdleTime) {
            mNettyClientConfig.readIdleTime = readIdleTime;
            return this;
        }

        /**
         * 配置重连间隔
         * @param reconnectIntervalTime
         */
        public Builder setReconnectIntervalTime(int reconnectIntervalTime) {
            mNettyClientConfig.reconnectIntervalTime = reconnectIntervalTime;
            return this;
        }

        /**
         * 配置别名
         * @param alias
         */
        public Builder setSelfAlias(String alias) {
            mNettyClientConfig.alias = alias;
            return this;
        }

        /**
         * 配置是否加密
         * @param isEncrypt
         */
        public Builder setEncrypt(boolean isEncrypt ) {
            mNettyClientConfig.isEncrypt = isEncrypt;
            return this;
        }

        /**
         * 是否重发
         * @param isResend
         */
        public Builder setIsResend(boolean isResend ) {
            mNettyClientConfig.isResend = isResend;
            return this;
        }

        /**
         * 重发间隔
         * @param resendInterval
         */
        public Builder setResendInterval(long resendInterval ) {
            mNettyClientConfig.resendInterval = resendInterval;
            return this;
        }

        public NettyClientConfig build() {
            return mNettyClientConfig;
        }
    }
}
