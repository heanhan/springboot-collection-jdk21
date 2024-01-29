package org.example.rocketmq.consumer;


import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RocketMQMessageListener(consumerGroup = "boot_group_1",topic = "boot-mq-topic")
public class ConsumerListener implements RocketMQListener<String> {

    @Override
    public void onMessage(String message) {
        log.info("开始消费...");
        log.info("\n=====\n message：{} \n=====\n",message);
    }
}
