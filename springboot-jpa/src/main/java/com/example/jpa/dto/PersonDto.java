package com.example.jpa.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class PersonDto {


    /**
     * ID，唯一标识列，使用主键自增策略
     */
    private Long id;

    /**
     * 用户名称
     */
    private String userName;


    /**
     * 性别
     */
    private Boolean sex;

    /**
     * 身高
     */
    private Double height;

    /**
     * 爱好
     */
    private String hover;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 最后修改时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastModifiedTime;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 最后修改人ID
     */
    private Long lastModifierId;

    /**
     * false:0 无效
     * true:1 有效 默认
     */
    private Boolean isDeleted;

    public PersonDto(Long id, String userName, Boolean sex, Double height, LocalDateTime lastModifiedTime, Long lastModifierId, Boolean isDeleted) {
        this.id = id;
        this.userName = userName;
        this.sex = sex;
        this.height = height;
        this.lastModifiedTime = lastModifiedTime;
        this.lastModifierId = lastModifierId;
        this.isDeleted = isDeleted;
    }
}
