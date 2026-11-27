package com.example.agentscope.travel.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 旅游运行时上下文的公共配置。
 */
@Validated
@ConfigurationProperties(prefix = "travel.context")
public class TravelContextProperties {

    /**
     * 动态时间提示使用的时区。
     */
    @NotBlank(message = "请配置旅游上下文时区")
    private String timeZone = "Asia/Shanghai";

    public TravelContextProperties() {
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
}
