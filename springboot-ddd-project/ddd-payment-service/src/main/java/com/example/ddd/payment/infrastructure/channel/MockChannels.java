package com.example.ddd.payment.infrastructure.channel;

import com.example.ddd.payment.domain.model.aggregate.*;
import com.example.ddd.payment.domain.service.PaymentChannelService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * 基础设施渠道装配：仅返回模拟 URL，绝不连接真实支付宝或微信账户。
 */
@Configuration
public class MockChannels {
    /**
     * 模拟渠道适配器，退款无外部副作用，幂等由本地支付/退款事务保证。
     */
    public static class MockAdapter implements PaymentChannelService.Adapter {
        private final String name;

        public MockAdapter(String name) {
            this.name = name;
        }

        public String payUrl(PaymentOrder.State p) {
            return "mock://" + name + "/pay/" + p.paymentId();
        }

        public void refund(RefundOrder.State refund) {
        }
    }

    /**
     * 支付宝沙箱策略示例。
     */
    public static final class AlipayMockAdapter extends MockAdapter {
        public AlipayMockAdapter() {
            super("alipay");
        }
    }

    /**
     * 微信沙箱策略示例。
     */
    public static final class WechatMockAdapter extends MockAdapter {
        public WechatMockAdapter() {
            super("wechat");
        }
    }

    @Bean
    PaymentChannelService channels() {
        return new PaymentChannelService(Map.of(PaymentOrder.Channel.ALIPAY, new AlipayMockAdapter(),
                PaymentOrder.Channel.WECHAT, new WechatMockAdapter(), PaymentOrder.Channel.MOCK, new MockAdapter("mock")));
    }
}
