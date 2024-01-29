package org.example.rocketmq.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MqMsg implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id ;

    /**
     * 消息内容
     */
    private String body ;
}
