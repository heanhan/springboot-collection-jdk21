package org.example.rocketmq.controller;


import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.example.rocketmq.entity.MqMsg;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping(value = "/send")
public class MQController {

    @Resource
    private RocketMQTemplate rocketMqTemplate;

    @Resource
    private DefaultMQProducer defaultMqProducer ;

    @GetMapping("/msg1")
    public String sendMsg1 (){
        try {
            // 构建消息主体
            JsonMapper jsonMapper = new JsonMapper();
            String msgBody = jsonMapper.writeValueAsString(new MqMsg(1,"boot_mq_msg"));
            // 发送消息
            rocketMqTemplate.convertAndSend("boot-mq-topic",msgBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK" ;
    }

    @GetMapping("/send/msg2")
    public String sendMsg2 (){
        try {
            // 构建消息主体
            JsonMapper jsonMapper = new JsonMapper();
            String msgBody = jsonMapper.writeValueAsString(new MqMsg(2,"boot_mq_msg"));
            // 构建消息对象
            Message message = new Message();
            message.setTopic("boot-mq-topic");
            message.setTags("boot-mq-tag");
            message.setKeys("boot-mq-key");
            message.setBody(msgBody.getBytes());
            // 发送消息，打印日志
            SendResult sendResult = defaultMqProducer.send(message);
            log.info("msgId:{},sendStatus:{}",sendResult.getMsgId(),sendResult.getSendStatus());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "OK" ;
    }


}
