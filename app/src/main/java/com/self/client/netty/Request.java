package com.self.client.netty;

public class Request {

    public int requestType;
    public int requestId;
    public String requestContent;

    public Request(int requestType, int requestId, String requestContent) {
        this.requestType = requestType;
        this.requestId = requestId;
        this.requestContent = requestContent;
    }
}
