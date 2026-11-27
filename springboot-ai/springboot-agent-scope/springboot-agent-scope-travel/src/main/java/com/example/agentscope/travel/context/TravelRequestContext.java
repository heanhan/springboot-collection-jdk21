package com.example.agentscope.travel.context;

import java.time.ZoneId;

/**
 * 一次旅游问答请求的运行时元数据，不包含提示词、知识正文或长期记忆。
 */
public final class TravelRequestContext {

    private final String requestId;
    private final String userId;
    private final String sessionId;
    private final String channel;
    private final ZoneId zoneId;

    public TravelRequestContext(
            String requestId,
            String userId,
            String sessionId,
            String channel,
            ZoneId zoneId) {
        this.requestId = requestId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.channel = channel;
        this.zoneId = zoneId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUserId() {
        return userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getChannel() {
        return channel;
    }

    public ZoneId getZoneId() {
        return zoneId;
    }
}
