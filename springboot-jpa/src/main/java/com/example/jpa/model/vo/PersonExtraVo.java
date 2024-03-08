package com.example.jpa.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class PersonExtraVo {

    private Long personId;

    /**
     * 姓名
     */
    private String userName;

    /**
     * 工作
     */
    private String worker;

    /**
     * 开始工作时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginWorkTime;


}
