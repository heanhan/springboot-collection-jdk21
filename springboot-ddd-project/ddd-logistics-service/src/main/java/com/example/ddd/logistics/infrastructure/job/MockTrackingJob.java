package com.example.ddd.logistics.infrastructure.job;

import com.example.ddd.logistics.application.service.ShipApplicationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 基础设施：可关闭的 Mock 承运商，每次扫描推进一阶段，单笔失败不影响其他包裹。 */
@Component
@ConditionalOnProperty(name="ddd.logistics.mock-enabled",havingValue="true")
public class MockTrackingJob {
    private final ShipApplicationService service;
    public MockTrackingJob(ShipApplicationService service) { this.service=service; }
    @Scheduled(fixedDelayString="${ddd.logistics.mock-track-interval-seconds:30}",timeUnit=java.util.concurrent.TimeUnit.SECONDS)
    public void tick() {
        for(String id:service.activeIds()) {
            try { service.advance(id); }
            catch(Exception e) { org.slf4j.LoggerFactory.getLogger(getClass()).warn("物流推进失败 shipmentId={}",id,e); }
        }
    }
}
