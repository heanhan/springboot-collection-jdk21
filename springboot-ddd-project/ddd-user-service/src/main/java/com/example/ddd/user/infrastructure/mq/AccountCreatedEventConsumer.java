package com.example.ddd.user.infrastructure.mq;

import com.example.ddd.common.util.JsonUtils;
import com.example.ddd.contract.MqTopics;
import com.example.ddd.contract.auth.event.AccountCreatedEvent;
import com.example.ddd.user.application.command.CreateUserCommand;
import com.example.ddd.user.application.service.UserApplicationService;
import com.example.ddd.user.domain.repository.UserRepository;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * MQ 消费者：监听 auth-service 发布的 AccountCreated 事件，在 user-service 中创建对应的业务用户档案。
 *
 * <p><b>为什么需要这个流程？</b>
 * auth-service 只关心凭据（用户名/密码），user-service 关心业务档案（昵称/头像/会员等级）。
 * 用户在 auth 注册后，通过 MQ 异步通知 user 创建档案，是<b>限界上下文之间的最终一致性</b>典型场景。</p>
 *
 * <p><b>幂等策略：</b>
 * <ul>
 *   <li>业务幂等：先判断 mobile 是否已存在，存在则跳过（简单可靠）。</li>
 *   <li>技术幂等：用 Redis SETNX 记录 eventId，24 小时内不重复消费。</li>
 * </ul>
 *
 * <p><b>消费失败重试：</b>
 * RocketMQ 默认会重试 16 次，若最终失败进入死信队列，需人工介入。</p>
 */
@Component
@RocketMQMessageListener(
        topic = MqTopics.AUTH_EVENT,
        selectorExpression = MqTopics.TAG_ACCOUNT_CREATED,
        consumerGroup = "ddd-user-account-created-consumer"
)
public class AccountCreatedEventConsumer implements RocketMQListener<String> {

    private static final Logger log = LoggerFactory.getLogger(AccountCreatedEventConsumer.class);
    private static final String IDEMPOTENT_KEY_PREFIX = "user:mq:idem:";
    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);

    private final UserApplicationService userApplicationService;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    public AccountCreatedEventConsumer(UserApplicationService userApplicationService,
                                       UserRepository userRepository,
                                       @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.userApplicationService = userApplicationService;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void onMessage(String payload) {
        AccountCreatedEvent event = JsonUtils.fromJson(payload, AccountCreatedEvent.class);
        if (event == null) {
            log.warn("[MQ] cannot deserialize AccountCreatedEvent: {}", payload);
            return;
        }
        log.info("[MQ] received AccountCreated userId={} mobile={}", event.getUserId(), event.getMobile());

        // 幂等：eventId 已处理过则直接返回
        if (redisTemplate != null) {
            String key = IDEMPOTENT_KEY_PREFIX + event.eventId();
            Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
            if (Boolean.FALSE.equals(firstTime)) {
                log.info("[MQ] duplicate event ignored: {}", event.eventId());
                return;
            }
        }

        // 业务幂等：手机号已存在则跳过（避免 MQ 重试导致重复插入）
        if (userRepository.existsByMobile(event.getMobile())) {
            log.info("[MQ] user with mobile already exists, skip. mobile={}", event.getMobile());
            return;
        }

        CreateUserCommand cmd = new CreateUserCommand(
                event.getNickname() == null ? event.getUsername() : event.getNickname(),
                event.getMobile(),
                event.getEmail(),
                null
        );
        try {
            userApplicationService.createUser(cmd);
        } catch (Exception e) {
            log.error("[MQ] handle AccountCreated failed userId={}", event.getUserId(), e);
            // 抛出异常触发 RocketMQ 重试
            throw e;
        }
    }
}
