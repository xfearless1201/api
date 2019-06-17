package com.cn.tianxia.api.common.v2;


import okhttp3.ConnectionPool;

import java.util.concurrent.TimeUnit;

/**
 * 创建OkHTTPClinet 实例
 * jacky
 */
public enum OkHttpClient {
    CLIENT;

    private okhttp3.OkHttpClient clientInstance;

    private Integer connectTimeout_time = 10;
    private Integer writeTimeout_time = 10;
    private Integer readTimeout_time = 30;

    OkHttpClient() {
        clientInstance = new okhttp3.OkHttpClient.Builder()
                .connectTimeout(connectTimeout_time, TimeUnit.SECONDS)
                .writeTimeout(writeTimeout_time, TimeUnit.SECONDS)
                .readTimeout(readTimeout_time, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)//当为true时,会进行而多次重连,timeOut会失效，可能导致当前用户线程占有过久
                 //每个地址支持最大并发连接数与连接存活时间
                .connectionPool(new ConnectionPool(6,5, TimeUnit.MINUTES))//
                .build();
    }

    public okhttp3.OkHttpClient getClientInstance() {
        return clientInstance;
    }

}
